package com.audiocontroller.client;

import com.audiocontroller.audio.AudioManager;
import com.audiocontroller.client.events.ClientMusicEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.audiocontroller.AudioController;

@Mod.EventBusSubscriber(modid = AudioController.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AudioControllerClient {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Инициализация клиентской части
            AudioManager manager = AudioManager.getInstance();
            
            // Сканируем музыку
            manager.getMusicLoader().scanMusicFiles().thenRun(() -> {
                // Загружаем плейлисты и активный плейлист
                manager.getPlaylistManager().loadPlaylists().thenRun(() -> {
                    manager.loadActivePlaylist();
                });
            });
            ClientMusicEvents.register();
        });
    }
}

