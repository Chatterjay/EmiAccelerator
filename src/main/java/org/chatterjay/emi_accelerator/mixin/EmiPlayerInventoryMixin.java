package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EmiPlayerInventory.class, remap = false)
public class EmiPlayerInventoryMixin {

    @Inject(method = "getCraftables", at = @At("HEAD"))
    private void onGetCraftablesHead(CallbackInfoReturnable<?> ci) {
        ReloadTimer.enterPhase("getCraftables");
    }

    @Inject(method = "getCraftables", at = @At("RETURN"))
    private void onGetCraftablesReturn(CallbackInfoReturnable<?> ci) {
        ReloadTimer.exitPhase();
    }
}
