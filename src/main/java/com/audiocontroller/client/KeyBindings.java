package com.audiocontroller.client;

import com.audiocontroller.client.gui.AudioControllerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.audiocontroller.AudioController;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = AudioController.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    public static final KeyMapping OPEN_GUI = new KeyMapping(
        "key.audiocontroller.open_gui",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_M,
        "key.categories.audiocontroller"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI);
    }

    @Mod.EventBusSubscriber(modid = AudioController.MODID, value = Dist.CLIENT)
    public static class KeyHandler {
        @SubscribeEvent
        public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }

            if (OPEN_GUI.consumeClick()) {
                mc.setScreen(new AudioControllerScreen());
            }
        }
    }
}
