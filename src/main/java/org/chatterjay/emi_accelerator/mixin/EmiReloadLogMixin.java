package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.runtime.EmiReloadLog;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiReloadLog.class, remap = false)
public class EmiReloadLogMixin {

    @Inject(method = "bake", at = @At("HEAD"))
    private static void onBakeHead(CallbackInfo ci) {
        ReloadTimer.enterPhase("reloadLog.bake");
    }

    @Inject(method = "bake", at = @At("RETURN"))
    private static void onBakeReturn(CallbackInfo ci) {
        ReloadTimer.exitPhase();
    }
}
