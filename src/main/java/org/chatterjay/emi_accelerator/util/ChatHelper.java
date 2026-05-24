package org.chatterjay.emi_accelerator.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.chatterjay.emi_accelerator.config.ModConfig;

public class ChatHelper {

    public static void sendIfNotHidden(Component message) {
        Minecraft.getInstance().execute(() -> {
            if (ModConfig.isChatMessagesHidden()) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            player.displayClientMessage(message, false);
        });
    }
}
