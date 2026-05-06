package org.chatterjay.emi_accelerator.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {
    private static final Path CONFIG_DIR = Path.of("config", "emi-accelerator");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("emi-accelerator.properties");

    public static boolean cacheEnabled = true;
    public static boolean autoClearOnModChange = true;
    public static int maxFileSizeMb = 50;
    public static boolean diagnosticsEnabled = true;
    public static boolean deferredSearchEnabled = true;
    public static long reloadDebounceMs = 5000;

    public static void init() {
        var props = new Properties();
        try {
            Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
                    props.load(is);
                }
            }
            cacheEnabled = Boolean.parseBoolean(props.getProperty("cache.enabled", "true"));
            autoClearOnModChange = Boolean.parseBoolean(props.getProperty("cache.auto_clear_on_mod_change", "true"));
            maxFileSizeMb = Integer.parseInt(props.getProperty("cache.max_file_size_mb", "50"));
            diagnosticsEnabled = Boolean.parseBoolean(props.getProperty("diagnostics.enabled", "true"));
            deferredSearchEnabled = Boolean.parseBoolean(props.getProperty("search.deferred", "true"));
            reloadDebounceMs = Long.parseLong(props.getProperty("reload.debounce_ms", "5000"));
            save();
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        var props = new Properties();
        props.setProperty("cache.enabled", String.valueOf(cacheEnabled));
        props.setProperty("cache.auto_clear_on_mod_change", String.valueOf(autoClearOnModChange));
        props.setProperty("cache.max_file_size_mb", String.valueOf(maxFileSizeMb));
        props.setProperty("diagnostics.enabled", String.valueOf(diagnosticsEnabled));
        props.setProperty("search.deferred", String.valueOf(deferredSearchEnabled));
        props.setProperty("reload.debounce_ms", String.valueOf(reloadDebounceMs));
        try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
            props.store(os, "EMI Accelerator Config");
        } catch (IOException ignored) {
        }
    }

    public static Path getConfigDir() {
        return CONFIG_DIR;
    }
}
