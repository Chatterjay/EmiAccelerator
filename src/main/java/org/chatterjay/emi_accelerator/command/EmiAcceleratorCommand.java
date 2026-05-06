package org.chatterjay.emi_accelerator.command;

import com.mojang.brigadier.CommandDispatcher;
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
                .then(Commands.literal("clear").executes(EmiAcceleratorCommand::clear))
                .then(Commands.literal("reload")
                        .executes(EmiAcceleratorCommand::reload)
                        .then(Commands.literal("--force")
                                .executes(EmiAcceleratorCommand::reloadForce))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        long size = EmiStackCache.getCacheFileSize();
        boolean cached = EmiStackCache.wasCacheUsed();
        long loadTime = EmiStackCache.getLastLoadTime();

        ctx.getSource().sendSuccess(() -> Component.literal("§a[EMI加速] §7缓存状态:"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  §7文件大小: §f" + (size >= 0 ? formatSize(size) : "无")), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  §7缓存命中: " + (cached ? "§a是" : "§c否")), false);
        if (loadTime > 0) {
            ctx.getSource().sendSuccess(() -> Component.literal("  §7加载耗时: §f" + loadTime + "ms"), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("  §7延迟搜索: " + (ModConfig.deferredSearchEnabled ? "§a开" : "§c关")
                + (EmiSearchDeferrer.isRunning() ? " §e(构建中…)" : "")), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        EmiStackCache.clearCache();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[EMI加速] §7缓存已清除，下次重载将重新生成"), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        EmiReloadManager.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[EMI加速] §7正在重载..."), false);
        return 1;
    }

    private static int reloadForce(CommandContext<CommandSourceStack> ctx) {
        EmiStackCache.clearCache();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[EMI加速] §7缓存已清除"), false);
        EmiReloadManager.reload();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[EMI加速] §7正在进行全量重载(延迟搜索)..."), false);
        return 1;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
