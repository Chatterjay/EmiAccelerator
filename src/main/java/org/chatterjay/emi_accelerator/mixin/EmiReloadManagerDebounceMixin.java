package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.runtime.EmiReloadManager;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiReloadManager.class, remap = false)
public class EmiReloadManagerDebounceMixin {
    private static long lastReloadTime = 0;

    @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
    private static void onReload(CallbackInfo ci) {
        long debounceMs = ModConfig.reloadDebounceMs;
        if (debounceMs <= 0) return;

        long now = System.currentTimeMillis();
        if (now - lastReloadTime < debounceMs) {
            ci.cancel();
            return;
        }
        lastReloadTime = now;
    }
}
