package org.chatterjay.emi_accelerator.util;

import com.mojang.logging.LogUtils;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmiSearchDeferrer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean pending = new AtomicBoolean(false);
    private static volatile boolean running = false;

    public static void markPending() {
        pending.set(true);
    }

    public static boolean consumePending() {
        return pending.compareAndSet(true, false);
    }

    public static void doDefer() {
        if (running) return;
        running = true;

        CompletableFuture.runAsync(() -> {
            try {
                if (ModConfig.isDebugEnabled()) {
                    LOGGER.debug("Starting deferred EmiSearch.bake()");
                }
                long start = System.currentTimeMillis();
                EmiSearch.bake();
                long took = System.currentTimeMillis() - start;

                if (ModConfig.isDebugEnabled()) {
                    LOGGER.debug("Deferred EmiSearch.bake() completed in {}ms", took);
                }

                if (ModConfig.isDebugEnabled()) {
                    Util.ioPool().execute(() ->
                            LOGGER.info("EMI search index built in {}ms (background)", took));
                }

                Minecraft.getInstance().execute(() -> {
                    try {
                        EmiScreenManager.search.update();
                    } catch (Exception e) {
                        if (ModConfig.isDebugEnabled()) {
                            LOGGER.debug("Could not update search (screen may be closed)", e);
                        }
                    }
                });
                ChatHelper.sendIfNotHidden(
                        Component.translatable("emi_accelerator.search.ready"));
            } catch (Exception e) {
                LOGGER.error("Deferred EmiSearch.bake() failed", e);
            } finally {
                running = false;
            }
        }, Util.backgroundExecutor());
    }

    public static boolean isRunning() {
        return running;
    }
}
