package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.network.chat.Component;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiReloadManager.class, remap = false)
public class EmiReloadManagerMixin {

    @Inject(method = "step(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private static void onStep(Component text, CallbackInfo ci) {
        String name = text.getString();
        ReloadTimer.onStep(name);
        if ("Finishing up".equals(name) || "Baking search".equals(name)) {
            ReloadTimer.checkpoint("step_" + name.replace(' ', '_'));
        }
    }

    @Inject(method = "step(Lnet/minecraft/network/chat/Component;J)V", at = @At("HEAD"))
    private static void onStep(Component text, long worry, CallbackInfo ci) {
        String name = text.getString();
        ReloadTimer.onStep(name);
        if ("Finishing up".equals(name) || "Baking search".equals(name)) {
            ReloadTimer.checkpoint("step_" + name.replace(' ', '_'));
        }
    }
}
