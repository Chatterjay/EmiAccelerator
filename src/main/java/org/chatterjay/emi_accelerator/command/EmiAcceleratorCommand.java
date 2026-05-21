package org.chatterjay.emi_accelerator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.chatterjay.emi_accelerator.util.EmiStackCache;

import java.util.Locale;

public class EmiAcceleratorCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("emiacc")
                .then(Commands.literal("status")
                        .executes(EmiAcceleratorCommand::status))
                .then(Commands.literal("clear")
                        .executes(EmiAcceleratorCommand::clear))
                .then(Commands.literal("reload")
                        .executes(ctx -> reload(ctx, false))
                        .then(Commands.literal("--force")
                                .executes(ctx -> reload(ctx, true))))
        );
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        source.sendSystemMessage(Component.literal("§a[EMI加速] §7状态:"));

        String accelStatus = ModConfig.isCacheEnabled() ? "§a启用" : "§c禁用";
        source.sendSystemMessage(Component.literal("  §7加速功能: " + accelStatus));

        boolean cacheExists = EmiStackCache.cacheFileExists();
        String cacheHitStr;
        if (cacheExists) {
            cacheHitStr = EmiStackCache.wasCacheUsed() ? "§a是" : "§7无";
        } else {
            cacheHitStr = "§7无";
        }
        long size = EmiStackCache.getCacheFileSize();
        String sizeStr = size >= 0 ? formatSize(size) : "§7无";
        source.sendSystemMessage(Component.literal("  §7文件大小: " + sizeStr));
        source.sendSystemMessage(Component.literal("  §7缓存命中: " + cacheHitStr));

        long loadTime = EmiStackCache.getLastLoadTime();
        if (loadTime > 0) {
            source.sendSystemMessage(Component.literal("  §7加载耗时: " + loadTime + "ms"));
        }

        String deferredStatus = ModConfig.isDeferredSearchEnabled() ? "§a开" : "§c关";
        source.sendSystemMessage(Component.literal("  §7延迟搜索: " + deferredStatus));

        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        EmiStackCache.clearCache();
        var source = ctx.getSource();
        source.sendSystemMessage(Component.literal("§a[EMI加速] §7缓存已清除，下次重载将重新生成"));
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx, boolean force) {
        var source = ctx.getSource();
        source.sendSystemMessage(Component.literal("§a[EMI加速] §7正在重载..."));

        if (force) {
            EmiStackCache.clearCache();
        }

        EmiReloadManager.reload();

        if (EmiStackCache.wasCacheUsed()) {
            source.sendSystemMessage(Component.literal(
                    String.format("§a[EMI加速] §7重载完成 §a(缓存加速: %dms)", EmiStackCache.getLastLoadTime())));
        } else {
            source.sendSystemMessage(Component.literal("§a[EMI加速] §7重载完成 §e(未缓存)"));
        }

        return 1;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
