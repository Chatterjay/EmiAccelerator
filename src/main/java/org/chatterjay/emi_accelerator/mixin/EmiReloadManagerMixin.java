package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.runtime.EmiReloadManager;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiReloadManager.class, remap = false)
public class EmiReloadManagerMixin {

    @Inject(method = "reload", at = @At("HEAD"))
    private static void onReload(CallbackInfo ci) {
        ReloadTimer.start();
    }
}
