package com.audiocontroller;

import com.audiocontroller.config.ConfigHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(AudioController.MODID)
public class AudioController {
    public static final String MODID = "audiocontroller";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AudioController() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Регистрируем конфигурацию клиента
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigHandler.SPEC, "audiocontroller-client.toml");
        
        // Регистрируем экран настроек в меню модов (только на клиенте)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (mc, parent) -> new com.audiocontroller.client.gui.ConfigScreen(parent)
                )
            );
        }
        
        modEventBus.addListener(this::commonSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Audio Controller mod initialized");
    }
}

