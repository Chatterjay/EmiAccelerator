package org.chatterjay.emi_accelerator.util;

import dev.emi.emi.search.EmiSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class EmiSearchDeferrer {
    private static final Logger LOGGER = LoggerFactory.getLogger("EmiSearchDeferrer");
    private static volatile boolean pending = false;

    public static void markPending() {
        pending = true;
    }

    public static boolean isPending() {
        return pending;
    }

    public static void consumePending() {
        pending = false;
    }

    public static void doDefer() {
        consumePending();
        CompletableFuture.runAsync(() -> {
            try {
                EmiSearch.bake();
            } catch (Exception e) {
                LOGGER.error("Failed to bake search", e);
            }
        });
    }
}
