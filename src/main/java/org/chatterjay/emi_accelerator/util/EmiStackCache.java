package org.chatterjay.emi_accelerator.util;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EmiStackCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CACHE_VERSION = 1;

    private static volatile boolean cacheUsed = false;
    private static volatile long lastLoadTime = 0;

    private static Path getCacheFile() {
        return ModConfig.getConfigDir().resolve("stack-cache.json");
    }

    public static boolean tryLoad() {
        if (!ModConfig.isCacheEnabled()) return false;

        cacheUsed = false;
        lastLoadTime = 0;
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

        try (Reader reader = Files.newBufferedReader(cacheFile)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            int version = root.get("version").getAsInt();
            if (version != CACHE_VERSION) {
                Files.deleteIfExists(cacheFile);
                return false;
            }

            String modHash = root.get("mod_hash").getAsString();
            if (!modHash.equals(computeModHash())) {
                if (ModConfig.isAutoClearOnModChange()) {
                    Files.deleteIfExists(cacheFile);
                }
                return false;
            }

            JsonArray stacksArray = root.getAsJsonArray("stacks");
            List<EmiStack> builtStacks = new ArrayList<>();

            for (JsonElement elem : stacksArray) {
                try {
                    JsonObject entry = elem.getAsJsonObject();
                    String type = entry.get("type").getAsString();
                    var id = net.minecraft.resources.ResourceLocation.parse(entry.get("id").getAsString());

                    if ("item".equals(type)) {
                        Item item = BuiltInRegistries.ITEM.get(id);
                        if (item == null) continue;

                        if (entry.has("data")) {
                            var parseResult = ItemStack.CODEC.parse(JsonOps.INSTANCE, entry.get("data"));
                            parseResult.ifSuccess(stack -> builtStacks.add(EmiStack.of(stack)));
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
            try {
                Files.deleteIfExists(cacheFile);
            } catch (IOException ignored) {
            }
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
        root.addProperty("mod_hash", computeModHash());

        JsonArray stacksArray = new JsonArray();
        for (EmiStack stack : stacks) {
            try {
                Object key = stack.getKey();
                JsonObject entry = new JsonObject();

                if (key instanceof Item item) {
                    entry.addProperty("type", "item");
                    entry.addProperty("id", BuiltInRegistries.ITEM.getKey(item).toString());

                    var changes = stack.getComponentChanges();
                    if (changes != null && !changes.isEmpty()) {
                        ItemStack itemStack = stack.getItemStack();
                        ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, itemStack)
                                .ifSuccess(json -> entry.add("data", json));
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

        try (Writer writer = Files.newBufferedWriter(cacheFile)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write cache file", e);
        }
    }

    private static String computeModHash() {
        try {
            var mods = ModList.get().getMods();
            var input = mods.stream()
                    .map(m -> m.getModId() + "=" + m.getVersion())
                    .sorted()
                    .collect(Collectors.joining("\n"));
            var md = MessageDigest.getInstance("SHA-256");
            var sb = new StringBuilder();
            for (byte b : md.digest(input.getBytes())) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
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
