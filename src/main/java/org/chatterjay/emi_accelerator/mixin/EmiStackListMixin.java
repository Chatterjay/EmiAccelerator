package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.registry.EmiStackList;
import org.chatterjay.emi_accelerator.util.EmiStackCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiStackList.class, remap = false)
public class EmiStackListMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("EmiStackListMixin");

    @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
    private static void onReload(CallbackInfoReturnable<Boolean> cir) {
        if (EmiStackCache.tryLoad()) {
            cir.setReturnValue(true);
        }
    }
}
