package org.chatterjay.emi_accelerator;

import com.mojang.logging.LogUtils;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.chatterjay.emi_accelerator.util.EmiStackCache;
import org.slf4j.Logger;

@EventBusSubscriber(modid = EmiAccelerator.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EmiAcceleratorClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean joined = false;

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (joined) return;
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        joined = true;

        if (EmiStackCache.cacheFileExists()) {
            LOGGER.debug("Cache exists, skipping auto-reload");
            return;
        }

        LOGGER.info("No cache found, auto-triggering EMI reload...");
        EmiReloadManager.reload();

        Minecraft.getInstance().execute(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(
                        Component.literal("§a[EMI加速] §7首次加载，后台预热缓存..."),
                        false);
            }
        });
    }
}
