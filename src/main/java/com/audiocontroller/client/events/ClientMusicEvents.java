package com.audiocontroller.client.events;

import com.audiocontroller.audio.AudioManager;
import com.audiocontroller.config.ConfigHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import org.slf4j.Logger;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.audiocontroller.AudioController;

@Mod.EventBusSubscriber(modid = AudioController.MODID, value = Dist.CLIENT)
public class ClientMusicEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static void register() {
        // Регистрация событий
    }
    
    private static boolean isConfigLoaded() {
        try {
            ConfigHandler.REPLACE_VANILLA_MUSIC.get();
            return true;
        } catch (IllegalStateException e) {
            // Конфигурация ещё не загружена
            return false;
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        AudioManager manager = AudioManager.getInstance();
        Minecraft mc = Minecraft.getInstance();
        
        // Если мы не в мире (главное меню), останавливаем музыку
        if (mc.level == null) {
            if (manager.isPlaying()) {
                manager.stop();
            }
            return;
        }
        
        // Обрабатываем паузу игры
        if (mc.isPaused()) {
            // Если игра на паузе, ставим музыку на паузу
            if (manager.isPlaying()) {
                manager.pause();
            }
        } else {
            // Если игра не на паузе, возобновляем музыку если она была на паузе
            manager.resume();
        }
        
        // Если конфигурация загружена и включена замена музыки
        if (isConfigLoaded() && ConfigHandler.REPLACE_VANILLA_MUSIC.get()) {
            // Агрессивно останавливаем стандартную музыку каждый тик
            if (mc.getMusicManager() != null) {
                // Останавливаем MusicManager
                mc.getMusicManager().stopPlaying();
            }
            
            // Останавливаем все активные звуки музыки из SoundManager
            if (mc.getSoundManager() != null) {
                // Останавливаем все звуки с источником MUSIC
                mc.getSoundManager().stop(null, net.minecraft.sounds.SoundSource.MUSIC);
            }
            
            // Вызываем тик AudioManager для обработки автоматического воспроизведения
            // Только если игра не на паузе
            if (!mc.isPaused()) {
                manager.tick();
            }
        } else if (manager.getActivePlaylist().isPresent()) {
            // Если замена музыки выключена, но есть активный плейлист, просто вызываем tick
            // (музыка будет играть вместе со стандартной)
            // Только если игра не на паузе
            if (!mc.isPaused()) {
                manager.tick();
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // При входе в мир загружаем активный плейлист и запускаем воспроизведение
        AudioManager manager = AudioManager.getInstance();
        Minecraft mc = Minecraft.getInstance();
        
        // Загружаем активный плейлист
        manager.loadActivePlaylist();
        
        // Если есть активный плейлист, запускаем воспроизведение
        manager.getActivePlaylist().ifPresent(playlist -> {
            if (!playlist.getTracks().isEmpty()) {
                LOGGER.info("Автоматический запуск плейлиста при входе в мир: {}", playlist.getName());
                // Устанавливаем плейлист заново, чтобы запустить воспроизведение
                manager.setActivePlaylist(playlist);
            }
        });
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // При выходе из мира останавливаем музыку
        AudioManager manager = AudioManager.getInstance();
        if (manager.isPlaying()) {
            LOGGER.info("Остановка музыки при выходе из мира");
            manager.stop();
        }
    }
}

