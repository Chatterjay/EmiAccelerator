package org.chatterjay.emi_accelerator.mixin;

import dev.emi.emi.search.EmiSearch;
import net.minecraft.client.searchtree.SuffixArray;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.chatterjay.emi_accelerator.util.EmiSearchDeferrer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmiSearch.class, remap = false)
public class EmiSearchBakeMixin {

    @Shadow
    private static SuffixArray names;
    @Shadow
    private static SuffixArray tooltips;
    @Shadow
    private static SuffixArray mods;
    @Shadow
    private static SuffixArray aliases;

    @Inject(method = "bake", at = @At("HEAD"), cancellable = true)
    private static void onBake(CallbackInfo ci) {
        if (ModConfig.deferredSearchEnabled && EmiSearchDeferrer.consumePending()) {
            names = new SuffixArray();
            tooltips = new SuffixArray();
            mods = new SuffixArray();
            aliases = new SuffixArray();
            EmiSearchDeferrer.doDefer();
            ci.cancel();
        }
    }
}
