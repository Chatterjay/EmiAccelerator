package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.screen.EmiScreenManager;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiScreenManager.class, remap = false)
public class EmiScreenManagerMixin {

    @Inject(method = "forceRecalculate", at = @At("HEAD"))
    private static void onForceRecalculateHead(CallbackInfo ci) {
        ReloadTimer.checkpoint("forceRecalculate");
    }

    @Inject(method = "recalculate", at = @At("HEAD"))
    private static void onRecalculateHead(CallbackInfo ci) {
        ReloadTimer.checkpoint("recalculate");
    }
}
