package com.audiocontroller.client.gui;

import com.audiocontroller.audio.AudioManager;
import com.audiocontroller.audio.CustomMusicTrack;
import com.audiocontroller.audio.Playlist;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AudioControllerScreen extends Screen {
    private static final Component TITLE = Component.translatable("audiocontroller.gui.title");
    
    private final AudioManager audioManager;
    private Button playlistButton;
    private Button refreshButton;
    private VolumeSlider volumeSlider;

    public AudioControllerScreen() {
        super(TITLE);
        this.audioManager = AudioManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        
        // Адаптивные размеры и позиции
        int padding = Math.max(10, this.width / 40);
        int buttonWidth = Math.min(200, Math.max(150, this.width / 4));
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int topY = 40;
        int centerY = this.height / 2;
        
        // Кнопка плейлистов
        int playlistButtonY = centerY - 30;
        this.playlistButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.playlist"),
            button -> {
                // Открыть экран плейлистов
                this.minecraft.setScreen(new PlaylistScreen(this));
            }
        ).bounds(centerX - buttonWidth / 2, playlistButtonY, buttonWidth, buttonHeight).build());
        
        // Кнопка обновления
        int refreshButtonSize = Math.min(20, this.width / 40);
        int refreshButtonX = Math.max(this.width - refreshButtonSize - padding, padding);
        this.refreshButton = this.addRenderableWidget(Button.builder(
            Component.literal("🔄"),
            button -> {
                audioManager.getMusicLoader().scanMusicFiles().thenRun(() -> {
                    // Обновляем плейлисты после сканирования
                    audioManager.getPlaylistManager().loadPlaylists();
                });
            }
        ).bounds(refreshButtonX, padding, refreshButtonSize, refreshButtonSize).build());
        
        // Слайдер громкости (использует настройки Minecraft)
        int volumeSliderY = playlistButtonY + buttonHeight + 10;
        double musicVolume = 1.0;
        if (this.minecraft != null && this.minecraft.options != null) {
            try {
                musicVolume = this.minecraft.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC);
            } catch (Exception e) {
                // Используем значение по умолчанию
            }
        }
        this.volumeSlider = this.addRenderableWidget(new VolumeSlider(
            centerX - buttonWidth / 2, volumeSliderY, buttonWidth, buttonHeight,
            Component.translatable("audiocontroller.gui.volume"),
            musicVolume
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int centerX = this.width / 2;
        int topY = 20;
        int lineHeight = 15;
        int[] infoY = {topY + 25}; // Используем массив для обхода проблемы с final
        
        // Заголовок
        guiGraphics.drawCenteredString(this.font, TITLE, centerX, topY, 0xFFFFFF);
        
        // Информация о текущем треке
        audioManager.getCurrentTrack().ifPresent(track -> {
            String trackInfo = Component.translatable("audiocontroller.gui.tracks").getString() + ": " + track.getName();
            // Обрезаем текст, если он слишком длинный
            int maxWidth = this.width - 40;
            if (this.font.width(trackInfo) > maxWidth) {
                trackInfo = this.font.plainSubstrByWidth(trackInfo, maxWidth - 10) + "...";
            }
            guiGraphics.drawCenteredString(this.font, trackInfo, centerX, infoY[0], 0xCCCCCC);
            infoY[0] += lineHeight;
        });
        
        // Информация об активном плейлисте
        audioManager.getActivePlaylist().ifPresent(playlist -> {
            String playlistInfo = Component.translatable("audiocontroller.gui.playlist").getString() + ": " + playlist.getName();
            // Обрезаем текст, если он слишком длинный
            int maxWidth = this.width - 40;
            if (this.font.width(playlistInfo) > maxWidth) {
                playlistInfo = this.font.plainSubstrByWidth(playlistInfo, maxWidth - 10) + "...";
            }
            guiGraphics.drawCenteredString(this.font, playlistInfo, centerX, infoY[0], 0xAAAAAA);
            infoY[0] += lineHeight;
        });
        
        // Статус воспроизведения
        String status = audioManager.isPlaying() ? "▶ Воспроизведение" : "⏹ Остановлено";
        guiGraphics.drawCenteredString(this.font, status, centerX, infoY[0], 0xAAAAAA);
        
        // Инструкция (внизу экрана, если есть место)
        int instructionY = Math.min(this.height - 30, this.height / 2 + 60);
        String instruction = "Выберите плейлист для автоматического воспроизведения";
        int maxWidth = this.width - 40;
        if (this.font.width(instruction) > maxWidth) {
            instruction = this.font.plainSubstrByWidth(instruction, maxWidth - 10) + "...";
        }
        guiGraphics.drawCenteredString(this.font, instruction, centerX, instructionY, 0x888888);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
