package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.search.EmiSearch;
import org.chatterjay.emi_accelerator.util.EmiSearchDeferrer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiSearch.class, remap = false)
public class EmiSearchBakeMixin {

    @Inject(method = "bake", at = @At("HEAD"), cancellable = true)
    private static void onBake(CallbackInfo ci) {
        if (EmiSearchDeferrer.isPending()) {
            EmiSearchDeferrer.doDefer();
            ci.cancel();
        }
    }
}
