package org.chatterjay.emi_accelerator.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.chatterjay.emi_accelerator.util.EmiSearchDeferrer;
import org.chatterjay.emi_accelerator.util.EmiStackCache;
import org.chatterjay.emi_accelerator.util.ReloadTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.emi.emi.runtime.EmiReloadManager$ReloadWorker", remap = false)
public class EmiReloadWorkerMixin {

    @Inject(method = "run", at = @At("HEAD"))
    private void onRunHead(CallbackInfo ci) {
        ReloadTimer.startReload();
    }

    // ─── Checkpoints for every major method call in run() ───

    // Clearing phase
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/registry/EmiRecipes.clear()V", ordinal = 0))
    private void beforeClearRecipes(CallbackInfo ci) {
        ReloadTimer.checkpoint("clear_recipes");
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/registry/EmiStackList.clear()V", ordinal = 0))
    private void beforeClearStackList(CallbackInfo ci) {
        ReloadTimer.checkpoint("clear_stacklist");
    }

    // Tags
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/registry/EmiTags.reload()V", ordinal = 0))
    private void beforeTagsReload(CallbackInfo ci) {
        ReloadTimer.checkpoint("tags_reload");
    }

    // Stack list reload (Constructing index)
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/registry/EmiStackList.reload()V", ordinal = 0))
    private void beforeStackListReload(CallbackInfo ci) {
        ReloadTimer.checkpoint("stacklist_reload");
    }

    // Bake index
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/registry/EmiStackList.bake()V", ordinal = 0))
    private void beforeStackListBake(CallbackInfo ci) {
        ReloadTimer.checkpoint("stacklist_bake");
    }

    // Bake recipes
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/registry/EmiRecipes.bake()V", ordinal = 0))
    private void beforeRecipesBake(CallbackInfo ci) {
        ReloadTimer.checkpoint("recipes_bake");
    }

    // BoM reload (called inside Baking recipes)
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/bom/BoM.reload()V", ordinal = 0))
    private void beforeBoMReload(CallbackInfo ci) {
        ReloadTimer.checkpoint("bom_reload");
    }

    // EmiPersistentData.load (called inside Baking recipes)
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/runtime/EmiPersistentData.load()V", ordinal = 0))
    private void beforePersistentDataLoad(CallbackInfo ci) {
        ReloadTimer.checkpoint("persistent_data_load");
    }

    // Bake search — set flag to defer to background thread
    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/search/EmiSearch.bake()V", ordinal = 0))
    private void beforeSearchBake(CallbackInfo ci) {
        ReloadTimer.checkpoint("search_bake");
        if (ModConfig.deferredSearchEnabled) {
            EmiSearchDeferrer.markPending();
        }
    }

    // ─── Finishing up detailed breakdown ───

    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/screen/widget/EmiSearchWidget.update()V", ordinal = 0, shift = At.Shift.AFTER))
    private void afterSearchUpdate(CallbackInfo ci) {
        ReloadTimer.checkpoint("after_search_update");
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/screen/EmiScreenManager.forceRecalculate()V", ordinal = 0, shift = At.Shift.AFTER))
    private void afterForceRecalculate(CallbackInfo ci) {
        ReloadTimer.checkpoint("after_force_recalculate");
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "dev/emi/emi/runtime/EmiReloadLog.bake()V", ordinal = 0, shift = At.Shift.AFTER))
    private void afterReloadLogBake(CallbackInfo ci) {
        ReloadTimer.checkpoint("after_reload_log_bake");
    }

    @Inject(method = "run", at = @At("RETURN"))
    private void onRunReturn(CallbackInfo ci) {
        ReloadTimer.checkpoint("run_return");

        boolean cacheHit = EmiStackCache.wasCacheUsed();
        ReloadTimer.finishReload(cacheHit);
        long totalTime = ReloadTimer.getLastReloadDuration();

        if (!cacheHit && !EmiStackCache.cacheFileExists()) return;

        Minecraft.getInstance().execute(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            String deferred = (ModConfig.deferredSearchEnabled && EmiSearchDeferrer.isRunning())
                    ? " §7(搜索后台构建…)" : "";
            String msg = cacheHit
                    ? "§a[EMI加速] §7重载完成 §a(缓存加速: " + totalTime + "ms)" + deferred
                    : "§a[EMI加速] §7重载完成 §e(未缓存, " + totalTime + "ms)" + deferred;

            player.displayClientMessage(Component.literal(msg), false);
        });
    }
}
