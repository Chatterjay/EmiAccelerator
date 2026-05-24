package org.chatterjay.emi_accelerator.util;

import com.mojang.logging.LogUtils;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReloadTimer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static long reloadStartTime;
    private static long lastReloadDuration;

    public static void startReload() {
        if (!ModConfig.isDiagnosticsEnabled()) return;
        reloadStartTime = System.currentTimeMillis();
    }

    public static void checkpoint(String name) {
        if (!ModConfig.isDiagnosticsEnabled() || reloadStartTime == 0) return;
    }

    public static void finishReload(boolean cacheHit) {
        if (!ModConfig.isDiagnosticsEnabled() || reloadStartTime == 0) return;
        lastReloadDuration = System.currentTimeMillis() - reloadStartTime;
        LOGGER.debug("EMI reload completed in {}ms{}", lastReloadDuration, cacheHit ? " (cache hit)" : "");
    }

    public static long getLastReloadDuration() {
        return lastReloadDuration;
    }
}
