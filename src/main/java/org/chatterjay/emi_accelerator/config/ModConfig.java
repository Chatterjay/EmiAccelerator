package org.chatterjay.emi_accelerator.config;

import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ModConfig");
    private static final Path CONFIG_DIR = FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve("emi-accelerator"));
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("emi-accelerator.txt");

    private static boolean cacheEnabled = true;
    private static boolean autoClearOnModChange = true;
    private static int maxFileSizeMb = 50;
    private static boolean diagnosticsEnabled = true;
    private static boolean deferredSearchEnabled = true;

    public static void init() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            LOGGER.error("Could not create config directory", e);
        }

        if (Files.exists(CONFIG_FILE)) {
            load();
        } else {
            save();
        }
    }

    private static void load() {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
            cacheEnabled = Boolean.parseBoolean(props.getProperty("cacheEnabled", "true"));
            autoClearOnModChange = Boolean.parseBoolean(props.getProperty("autoClearOnModChange", "true"));
            maxFileSizeMb = Integer.parseInt(props.getProperty("maxFileSizeMb", "50"));
            diagnosticsEnabled = Boolean.parseBoolean(props.getProperty("diagnosticsEnabled", "true"));
            deferredSearchEnabled = Boolean.parseBoolean(props.getProperty("deferredSearchEnabled", "true"));
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
        }
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("cacheEnabled", String.valueOf(cacheEnabled));
        props.setProperty("autoClearOnModChange", String.valueOf(autoClearOnModChange));
        props.setProperty("maxFileSizeMb", String.valueOf(maxFileSizeMb));
        props.setProperty("diagnosticsEnabled", String.valueOf(diagnosticsEnabled));
        props.setProperty("deferredSearchEnabled", String.valueOf(deferredSearchEnabled));
        try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "EMI Accelerator Config");
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public static Path getConfigDir() {
        return CONFIG_DIR;
    }

    public static boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public static boolean isAutoClearOnModChange() {
        return autoClearOnModChange;
    }

    public static int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public static boolean isDiagnosticsEnabled() {
        return diagnosticsEnabled;
    }

    public static boolean isDeferredSearchEnabled() {
        return deferredSearchEnabled;
    }
}
