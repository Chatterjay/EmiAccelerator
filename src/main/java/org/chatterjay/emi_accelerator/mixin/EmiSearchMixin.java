package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.screen.widget.EmiSearchWidget;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiSearchWidget.class, remap = false)
public class EmiSearchMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateHead(CallbackInfo ci) {
        ReloadTimer.enterPhase("searchWidget.update");
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdateReturn(CallbackInfo ci) {
        ReloadTimer.exitPhase();
    }
}
