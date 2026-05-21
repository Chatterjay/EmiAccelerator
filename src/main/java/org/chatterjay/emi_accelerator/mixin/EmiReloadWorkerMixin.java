package org.chatterjay.emi_accelerator.mixin;

import org.chatterjay.emi_accelerator.util.EmiSearchDeferrer;
import org.chatterjay.emi_accelerator.util.EmiStackCache;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.emi.emi.runtime.EmiReloadManager$ReloadWorker", remap = false)
public class EmiReloadWorkerMixin {

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void onRun(CallbackInfo ci) {
        if (EmiStackCache.tryLoad()) {
            ReloadTimer.markCacheHit();
            ci.cancel();
            return;
        }
        ReloadTimer.start();
    }

    @Inject(method = "run", at = @At("RETURN"))
    private void onRunReturn(CallbackInfo ci) {
        EmiStackCache.saveAsync();
        ReloadTimer.checkpoint("search_bake");
        EmiSearchDeferrer.markPending();
    }
}
