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
        ReloadTimer.enterPhase("forceRecalculate");
    }

    @Inject(method = "forceRecalculate", at = @At("RETURN"))
    private static void onForceRecalculateReturn(CallbackInfo ci) {
        ReloadTimer.exitPhase();
    }

    @Inject(method = "recalculate", at = @At("HEAD"))
    private static void onRecalculateHead(CallbackInfo ci) {
        ReloadTimer.enterPhase("recalculate");
    }

    @Inject(method = "recalculate", at = @At("RETURN"))
    private static void onRecalculateReturn(CallbackInfo ci) {
        ReloadTimer.exitPhase();
    }
}
