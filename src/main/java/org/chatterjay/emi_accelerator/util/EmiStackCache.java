package org.chatterjay.emi_accelerator.util;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.ModList;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class EmiStackCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CACHE_VERSION = 2;
    private static final int MAGIC = 0x454D4943; // "EMIC"

    private static volatile boolean cacheUsed = false;
    private static volatile long lastLoadTime = 0;

    private static Path getCacheFile() {
        return ModConfig.getConfigDir().resolve("stack-cache.emic");
    }

    public static boolean tryLoad() {
        if (!ModConfig.isCacheEnabled()) return false;

        cacheUsed = false;
        lastLoadTime = 0;

        cleanOldJsonCache();

        var cacheFile = getCacheFile();
        if (!Files.exists(cacheFile)) return false;

        try {
            if (Files.size(cacheFile) > (long) ModConfig.getMaxFileSizeMb() * 1024 * 1024) {
                LOGGER.warn("Cache file exceeds size limit, deleting");
                Files.deleteIfExists(cacheFile);
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        long start = System.currentTimeMillis();

        try {
            byte[] fileBytes = Files.readAllBytes(cacheFile);
            if (fileBytes.length < 44) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            int magic = readInt(fileBytes, 0);
            if (magic != MAGIC) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            int version = readInt(fileBytes, 4);
            if (version != CACHE_VERSION) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            byte[] storedHash = new byte[32];
            System.arraycopy(fileBytes, 8, storedHash, 0, 32);
            byte[] computedHash = computeModHashBytes();
            if (!MessageDigest.isEqual(storedHash, computedHash)) {
                if (ModConfig.isAutoClearOnModChange()) {
                    Files.deleteIfExists(cacheFile);
                }
                return false;
            }

            int uncompressedLen = readInt(fileBytes, 40);
            if (uncompressedLen <= 0) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            byte[] compressed = new byte[fileBytes.length - 44];
            System.arraycopy(fileBytes, 44, compressed, 0, compressed.length);

            byte[] payload = new byte[uncompressedLen];
            try {
                Inflater inflater = new Inflater();
                inflater.setInput(compressed);
                int resultLen = inflater.inflate(payload);
                inflater.end();
                if (resultLen != uncompressedLen) {
                    Files.deleteIfExists(cacheFile);
                    return false;
                }
            } catch (Exception e) {
                LOGGER.debug("Decompression failed, cache may be corrupted");
                Files.deleteIfExists(cacheFile);
                return false;
            }

            String json = new String(payload, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            JsonArray stacksArray = root.getAsJsonArray("stacks");
            List<EmiStack> builtStacks = new ArrayList<>();

            for (JsonElement elem : stacksArray) {
                try {
                    JsonObject entry = elem.getAsJsonObject();
                    String type = entry.get("type").getAsString();
                    var id = ResourceLocation.parse(entry.get("id").getAsString());

                    if ("item".equals(type)) {
                        Item item = BuiltInRegistries.ITEM.get(id);
                        if (item == null) continue;

                        if (entry.has("data")) {
                            var parseResult = ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, entry.get("data"));
                            parseResult.result().ifPresent(stack -> builtStacks.add(EmiStack.of(stack)));
                        } else {
                            builtStacks.add(EmiStack.of(item));
                        }
                    } else if ("fluid".equals(type)) {
                        Fluid fluid = BuiltInRegistries.FLUID.get(id);
                        if (fluid != null) {
                            builtStacks.add(EmiStack.of(fluid));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to deserialize cache entry: {}", e.getMessage());
                }
            }

            if (builtStacks.isEmpty()) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            EmiStackList.stacks = builtStacks;
            cacheUsed = true;
            lastLoadTime = System.currentTimeMillis() - start;
            LOGGER.debug("EMI cache loaded {} stacks in {}ms", builtStacks.size(), lastLoadTime);
            return true;
        } catch (Exception e) {
            try { Files.deleteIfExists(cacheFile); } catch (IOException ignored) {}
            return false;
        }
    }

    public static void saveAsync() {
        if (!ModConfig.isCacheEnabled()) return;
        if (cacheUsed) return;

        CompletableFuture.runAsync(() -> {
            try {
                save();
            } catch (Exception e) {
                LOGGER.error("Failed to save EMI cache", e);
            }
        });
    }

    private static void save() {
        List<EmiStack> stacks = EmiStackList.stacks;
        if (stacks == null || stacks.isEmpty()) return;

        var cacheFile = getCacheFile();
        try {
            Files.createDirectories(cacheFile.getParent());
        } catch (IOException e) {
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", CACHE_VERSION);
        root.addProperty("mod_hash", bytesToHex(computeModHashBytes()));

        JsonArray stacksArray = new JsonArray();
        for (EmiStack stack : stacks) {
            try {
                Object key = stack.getKey();
                JsonObject entry = new JsonObject();

                if (key instanceof Item item) {
                    entry.addProperty("type", "item");
                    entry.addProperty("id", BuiltInRegistries.ITEM.getKey(item).toString());

                    if (stack.hasNbt()) {
                        ItemStack itemStack = stack.getItemStack();
                        ItemStack.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, itemStack)
                                .result()
                                .ifPresent(json -> entry.add("data", json));
                    }
                } else if (key instanceof Fluid fluid) {
                    entry.addProperty("type", "fluid");
                    entry.addProperty("id", BuiltInRegistries.FLUID.getKey(fluid).toString());
                } else {
                    continue;
                }

                stacksArray.add(entry);
            } catch (Exception e) {
                LOGGER.debug("Failed to serialize cache entry: {}", e.getMessage());
            }
        }

        root.add("stacks", stacksArray);

        try {
            String json = GSON.toJson(root);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            deflater.setInput(jsonBytes);
            deflater.finish();
            byte[] compressed = new byte[jsonBytes.length + 64];
            int compressedLen = deflater.deflate(compressed);
            deflater.end();

            byte[] header = new byte[44];
            writeInt(header, 0, MAGIC);
            writeInt(header, 4, CACHE_VERSION);
            System.arraycopy(computeModHashBytes(), 0, header, 8, 32);
            writeInt(header, 40, jsonBytes.length);

            byte[] fileBytes = new byte[header.length + compressedLen];
            System.arraycopy(header, 0, fileBytes, 0, header.length);
            System.arraycopy(compressed, 0, fileBytes, header.length, compressedLen);

            Files.write(cacheFile, fileBytes);
        } catch (Exception e) {
            LOGGER.error("Failed to write cache file", e);
        }
    }

    private static byte[] computeModHashBytes() {
        try {
            var mods = ModList.get().getMods();
            var input = mods.stream()
                    .map(m -> m.getModId() + "=" + m.getVersion())
                    .sorted()
                    .collect(Collectors.joining("\n"));
            var md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new byte[32];
        }
    }

    private static void cleanOldJsonCache() {
        try {
            Files.deleteIfExists(ModConfig.getConfigDir().resolve("stack-cache.json"));
        } catch (IOException ignored) {
        }
    }

    private static int readInt(byte[] arr, int off) {
        return ((arr[off] & 0xFF) << 24) | ((arr[off + 1] & 0xFF) << 16)
             | ((arr[off + 2] & 0xFF) << 8) | (arr[off + 3] & 0xFF);
    }

    private static void writeInt(byte[] arr, int off, int v) {
        arr[off] = (byte) (v >> 24);
        arr[off + 1] = (byte) (v >> 16);
        arr[off + 2] = (byte) (v >> 8);
        arr[off + 3] = (byte) v;
    }

    private static String bytesToHex(byte[] bytes) {
        var sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean wasCacheUsed() {
        return cacheUsed;
    }

    public static long getLastLoadTime() {
        return lastLoadTime;
    }

    public static boolean cacheFileExists() {
        return Files.exists(getCacheFile());
    }

    public static void clearCache() {
        try {
            Files.deleteIfExists(getCacheFile());
            cacheUsed = false;
        } catch (IOException e) {
            LOGGER.error("Failed to clear cache", e);
        }
    }

    public static long getCacheFileSize() {
        try {
            return Files.size(getCacheFile());
        } catch (IOException e) {
            return -1;
        }
    }
}
