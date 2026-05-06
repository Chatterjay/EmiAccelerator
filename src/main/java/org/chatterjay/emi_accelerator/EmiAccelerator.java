package org.chatterjay.emi_accelerator;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.chatterjay.emi_accelerator.command.EmiAcceleratorCommand;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.slf4j.Logger;

@Mod(EmiAccelerator.MODID)
public class EmiAccelerator {
    public static final String MODID = "emi_accelerator";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EmiAccelerator(IEventBus modEventBus, ModContainer modContainer) {
        ModConfig.init();

        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event ->
                EmiAcceleratorCommand.register(event.getDispatcher()));
    }
}
