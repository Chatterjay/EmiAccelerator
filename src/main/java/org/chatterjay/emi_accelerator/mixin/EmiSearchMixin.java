package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.screen.widget.EmiSearchWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiSearchWidget.class, remap = false)
public class EmiSearchMixin {

    @Inject(method = "method_1599", at = @At("HEAD"))
    private void onSearch(CallbackInfo ci) {
        // Diagnostic hook
    }
}
