package org.chatterjay.emi_accelerator.config;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_DIR = Path.of("config", "emi-accelerator");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("emi-accelerator.txt");

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("acceleration.enabled", "true");
        DEFAULTS.put("cache.enabled", "true");
        DEFAULTS.put("cache.auto_clear_on_mod_change", "true");
        DEFAULTS.put("cache.max_file_size_mb", "50");
        DEFAULTS.put("diagnostics.enabled", "true");
        DEFAULTS.put("search.deferred", "true");
        DEFAULTS.put("chat.hide_messages", "false");
        DEFAULTS.put("debug.enabled", "false");
    }

    private static final Map<String, String> VALUES = new ConcurrentHashMap<>(DEFAULTS);

    public static boolean isAccelerationEnabled() {
        return isTrue("acceleration.enabled");
    }
    public static boolean isCacheEnabled() {
        return isTrue("cache.enabled");
    }
    public static boolean isAutoClearOnModChange() {
        return isTrue("cache.auto_clear_on_mod_change");
    }
    public static int getMaxFileSizeMb() {
        return getInt("cache.max_file_size_mb", 50);
    }
    public static boolean isDiagnosticsEnabled() {
        return isTrue("diagnostics.enabled");
    }
    public static boolean isDeferredSearchEnabled() {
        return isTrue("search.deferred");
    }
    public static boolean isChatMessagesHidden() {
        return isTrue("chat.hide_messages");
    }
    public static boolean isDebugEnabled() {
        return isTrue("debug.enabled");
    }

    public static void setChatMessagesHidden(boolean v) { VALUES.put("chat.hide_messages", String.valueOf(v)); save(); }
    public static void setDebugEnabled(boolean v) { VALUES.put("debug.enabled", String.valueOf(v)); save(); }
    public static void setAccelerationEnabled(boolean v) { VALUES.put("acceleration.enabled", String.valueOf(v)); save(); }

    public static void init() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                load();
            } else {
                save();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to init config from {}", CONFIG_FILE.toAbsolutePath(), e);
        }
    }

    private static void load() throws IOException {
        try (BufferedReader r = Files.newBufferedReader(CONFIG_FILE)) {
            String line;
            while ((line = r.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if (DEFAULTS.containsKey(key)) {
                    VALUES.put(key, val);
                }
            }
        }
    }

    public static void save() {
        try (BufferedWriter w = Files.newBufferedWriter(CONFIG_FILE)) {
            for (var entry : DEFAULTS.entrySet()) {
                String val = VALUES.getOrDefault(entry.getKey(), entry.getValue());
                w.write(entry.getKey() + "=" + val);
                w.newLine();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}", CONFIG_FILE.toAbsolutePath(), e);
        }
    }

    private static boolean isTrue(String key) {
        return "true".equals(VALUES.get(key));
    }

    private static int getInt(String key, int def) {
        String v = VALUES.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    public static Path getConfigDir() {
        return CONFIG_DIR;
    }
}
