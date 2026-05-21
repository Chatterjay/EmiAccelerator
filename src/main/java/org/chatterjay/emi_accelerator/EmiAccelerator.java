package org.chatterjay.emi_accelerator;

import com.mojang.logging.LogUtils;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.chatterjay.emi_accelerator.command.EmiAcceleratorCommand;
import org.chatterjay.emi_accelerator.config.ModConfig;
import org.slf4j.Logger;

@Mod(EmiAccelerator.MODID)
public class EmiAccelerator {
    public static final String MODID = "emi_accelerator";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EmiAccelerator() {
        ModConfig.init();
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                EmiAcceleratorCommand.register(event.getDispatcher()));
    }
}
