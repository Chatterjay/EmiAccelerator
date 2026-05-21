package org.chatterjay.emi_accelerator.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReloadTimer {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReloadTimer");
    private static final Map<String, Long> timings = new LinkedHashMap<>();
    private static long startTime = 0;
    private static boolean cacheHit = false;

    public static void start() {
        timings.clear();
        cacheHit = false;
        startTime = System.currentTimeMillis();
    }

    public static void checkpoint(String name) {
        long now = System.currentTimeMillis();
        timings.put(name, now - startTime);
    }

    public static void markCacheHit() {
        cacheHit = true;
    }

    public static long finish() {
        long total = System.currentTimeMillis() - startTime;
        LOGGER.debug("EMI reload completed in {}ms{}", total, cacheHit ? " (cache hit)" : "");

        Path timingsFile = org.chatterjay.emi_accelerator.config.ModConfig.getConfigDir().resolve("reload-timings.json");
        try {
            Files.createDirectories(timingsFile.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("total_ms", total);
            root.addProperty("cache_hit", cacheHit);
            var timingsObj = new JsonObject();
            timings.forEach((name, ms) -> timingsObj.addProperty(name, ms));
            root.add("checkpoints", timingsObj);
            Files.writeString(timingsFile, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (IOException e) {
            // ignore
        }

        return total;
    }
}
