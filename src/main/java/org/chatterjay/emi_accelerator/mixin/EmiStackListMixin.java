package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.registry.EmiStackList;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.chatterjay.emi_accelerator.util.EmiStackCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListMixin {

    @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
    private static void onReloadHead(CallbackInfo ci) {
        if (!ModConfig.isAccelerationEnabled()) return;
        if (EmiStackCache.tryLoad()) {
            ci.cancel();
        }
    }

    @Inject(method = "reload", at = @At("RETURN"))
    private static void onReloadReturn(CallbackInfo ci) {
        if (!ModConfig.isAccelerationEnabled()) return;
        EmiStackCache.saveAsync();
    }
}
