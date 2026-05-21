package org.chatterjay.emi_accelerator.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.chatterjay.emi_accelerator.config.ModConfig;

public class ChatHelper {
    private static final int COLOR_AQUA = 0x55FFFF;
    private static final int COLOR_GRAY = 0xAAAAAA;

    public static void sendMessage(net.minecraft.client.player.LocalPlayer player, String message) {
        if (!ModConfig.isDiagnosticsEnabled()) return;
        if (player == null) return;
        MutableComponent component = Component.literal(message);
        player.displayClientMessage(component, false);
    }

    public static void sendStatus(net.minecraft.client.player.LocalPlayer player, String statusText, Object... args) {
        if (!ModConfig.isDiagnosticsEnabled()) return;
        if (player == null) return;
        String msg = String.format(statusText, args);
        MutableComponent component = Component.literal(msg);
        player.displayClientMessage(component, false);
    }
}
