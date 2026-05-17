package org.chatterjay.emi_accelerator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.chatterjay.emi_accelerator.util.EmiSearchDeferrer;
import org.chatterjay.emi_accelerator.util.EmiStackCache;
import org.chatterjay.emi_accelerator.util.ReloadTimer;

public class EmiAcceleratorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("emiacc")
                .then(Commands.literal("status").executes(EmiAcceleratorCommand::status))
                .then(Commands.literal("enable")
                        .executes(ctx -> toggleAccel(ctx))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> toggleAccel(ctx, BoolArgumentType.getBool(ctx, "value")))))
                .then(Commands.literal("chat")
                        .executes(ctx -> toggleChat(ctx))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> toggleChat(ctx, BoolArgumentType.getBool(ctx, "value")))))
                .then(Commands.literal("debug")
                        .executes(ctx -> toggleDebug(ctx))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> toggleDebug(ctx, BoolArgumentType.getBool(ctx, "value")))))
                .then(Commands.literal("clear").executes(EmiAcceleratorCommand::clear))
                .then(Commands.literal("reload")
                        .executes(EmiAcceleratorCommand::reload)
                        .then(Commands.literal("--force")
                                .executes(EmiAcceleratorCommand::reloadForce))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.status.title"), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.status.accel")
                .append(Component.translatable(ModConfig.isAccelerationEnabled()
                        ? "emi_accelerator.status.on" : "emi_accelerator.status.off")), false);

        if (!ModConfig.isAccelerationEnabled()) return 1;

        long size = EmiStackCache.getCacheFileSize();
        boolean cached = EmiStackCache.wasCacheUsed();
        long loadTime = EmiStackCache.getLastLoadTime();

        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.status.file_size",
                size >= 0 ? formatSize(size) : Component.translatable("emi_accelerator.status.none")), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.status.cache_hit_prefix")
                .append(Component.translatable(cached ? "emi_accelerator.status.yes" : "emi_accelerator.status.no")), false);
        if (loadTime > 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.status.load_time", loadTime), false);
        }
        Component deferredStatus = Component.translatable("emi_accelerator.status.deferred_prefix")
                .append(Component.translatable(ModConfig.isDeferredSearchEnabled() ? "emi_accelerator.status.on" : "emi_accelerator.status.off"));
        if (ModConfig.isDeferredSearchEnabled() && EmiSearchDeferrer.isRunning()) {
            deferredStatus = Component.literal("").append(deferredStatus)
                    .append(Component.translatable("emi_accelerator.status.building"));
        }
        final Component finalDeferredStatus = deferredStatus;
        ctx.getSource().sendSuccess(() -> finalDeferredStatus, false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        EmiStackCache.clearCache();
        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.cache.cleared"), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        EmiReloadManager.reload();
        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.reload.triggered"), false);
        return 1;
    }

    private static int reloadForce(CommandContext<CommandSourceStack> ctx) {
        EmiStackCache.clearCache();
        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.cache.cleared_reload"), false);
        EmiReloadManager.reload();
        ctx.getSource().sendSuccess(() -> Component.translatable("emi_accelerator.reload.full_force"), false);
        return 1;
    }

    private static int toggleAccel(CommandContext<CommandSourceStack> ctx) {
        return toggleAndReport(ctx, "accel", !ModConfig.isAccelerationEnabled(),
                v -> ModConfig.setAccelerationEnabled(v));
    }

    private static int toggleAccel(CommandContext<CommandSourceStack> ctx, boolean value) {
        return toggleAndReport(ctx, "accel", value,
                v -> ModConfig.setAccelerationEnabled(v));
    }

    private static int toggleChat(CommandContext<CommandSourceStack> ctx) {
        return toggleAndReport(ctx, "chat", ModConfig.isChatMessagesHidden(),
                v -> ModConfig.setChatMessagesHidden(!v));
    }

    private static int toggleChat(CommandContext<CommandSourceStack> ctx, boolean value) {
        return toggleAndReport(ctx, "chat", value,
                v -> ModConfig.setChatMessagesHidden(!v));
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> ctx) {
        return toggleAndReport(ctx, "debug", !ModConfig.isDebugEnabled(),
                v -> ModConfig.setDebugEnabled(v));
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> ctx, boolean value) {
        return toggleAndReport(ctx, "debug", value,
                v -> ModConfig.setDebugEnabled(v));
    }

    private static int toggleAndReport(CommandContext<CommandSourceStack> ctx, String key, boolean value,
                                       java.util.function.Consumer<Boolean> setter) {
        setter.accept(value);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("emi_accelerator.toggle." + key,
                        Component.translatable(value ? "emi_accelerator.status.on" : "emi_accelerator.status.off")),
                false);
        return 1;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
