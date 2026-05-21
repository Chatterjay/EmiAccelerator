package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.runtime.EmiReloadLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiReloadLog.class, remap = false)
public class EmiReloadLogMixin {

    @Inject(method = "addLog", at = @At("HEAD"))
    private void onAddLog(CallbackInfo ci) {
        // Diagnostic hook
    }
}
