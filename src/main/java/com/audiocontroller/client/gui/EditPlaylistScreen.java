package com.audiocontroller.client.gui;

import com.audiocontroller.audio.AudioManager;
import com.audiocontroller.audio.CustomMusicTrack;
import com.audiocontroller.audio.MusicLoader;
import com.audiocontroller.audio.Playlist;
import com.audiocontroller.audio.PlaylistManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EditPlaylistScreen extends Screen {
    private final Screen parent;
    private final Playlist playlist;
    private final PlaylistManager playlistManager;
    private final MusicLoader musicLoader;
    private final List<Button> playlistTrackButtons = new ArrayList<>();
    private final List<Button> availableTrackButtons = new ArrayList<>();
    private int selectedPlaylistTrackIndex = -1;
    private int selectedAvailableTrackIndex = -1;

    public EditPlaylistScreen(Screen parent, Playlist playlist) {
        super(Component.translatable("audiocontroller.gui.edit_playlist"));
        this.parent = parent;
        this.playlist = playlist;
        this.playlistManager = AudioManager.getInstance().getPlaylistManager();
        this.musicLoader = AudioManager.getInstance().getMusicLoader();
    }

    @Override
    protected void init() {
        super.init();
        
        // Адаптивные размеры
        int padding = Math.max(10, this.width / 40);
        int buttonHeight = 20;
        int buttonSpacing = 2;
        int centerX = this.width / 2;
        int topY = 40;
        int listWidth = (this.width - padding * 3) / 2;
        int leftListX = padding;
        int rightListX = centerX + padding / 2;
        int listTopY = topY + 30;
        int listHeight = this.height - listTopY - 80;
        
        // Заголовки
        int headerY = topY + 10;
        
        // Список треков в плейлисте (слева)
        createPlaylistTrackButtons(leftListX, listTopY, listWidth, buttonHeight, buttonSpacing);
        
        // Список доступных треков (справа)
        createAvailableTrackButtons(rightListX, listTopY, listWidth, buttonHeight, buttonSpacing);
        
        // Кнопки управления
        int buttonsY = this.height - 60;
        int buttonWidth = 100;
        int controlButtonSpacing = 10;
        
        // Кнопка добавления трека
        this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.add_track"),
            button -> {
                if (selectedAvailableTrackIndex >= 0) {
                    List<CustomMusicTrack> availableTracks = getAvailableTracks();
                    if (selectedAvailableTrackIndex < availableTracks.size()) {
                        CustomMusicTrack track = availableTracks.get(selectedAvailableTrackIndex);
                        addTrackToPlaylist(track);
                    }
                }
            }
        ).bounds(centerX - buttonWidth - controlButtonSpacing / 2, buttonsY, buttonWidth, buttonHeight).build());
        
        // Кнопка удаления трека
        this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.remove_track"),
            button -> {
                if (selectedPlaylistTrackIndex >= 0) {
                    removeTrackFromPlaylist(selectedPlaylistTrackIndex);
                }
            }
        ).bounds(centerX + controlButtonSpacing / 2, buttonsY, buttonWidth, buttonHeight).build());
        
        // Кнопка сохранения
        int saveButtonY = buttonsY + buttonHeight + 5;
        this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.save"),
            button -> {
                savePlaylist();
                this.minecraft.setScreen(this.parent);
            }
        ).bounds(centerX - buttonWidth / 2, saveButtonY, buttonWidth, buttonHeight).build());
        
        // Кнопка назад
        int backButtonY = saveButtonY + buttonHeight + 5;
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.back"),
            button -> this.minecraft.setScreen(this.parent)
        ).bounds(centerX - buttonWidth / 2, backButtonY, buttonWidth, buttonHeight).build());
    }
    
    private void createPlaylistTrackButtons(int x, int y, int width, int buttonHeight, int buttonSpacing) {
        // Удаляем старые кнопки
        playlistTrackButtons.forEach(this::removeWidget);
        playlistTrackButtons.clear();
        
        List<CustomMusicTrack> tracks = playlist.getTracks();
        // Если список пуст, просто выходим - кнопки будут созданы при добавлении треков
        
        int buttonY = y;
        for (int i = 0; i < tracks.size(); i++) {
            CustomMusicTrack track = tracks.get(i);
            final int index = i;
            
            Button button = Button.builder(
                Component.literal(track.getName()),
                (btn) -> {
                    selectedPlaylistTrackIndex = index;
                    updateButtonStyles();
                }
            ).bounds(x, buttonY, width, buttonHeight).build();
            
            playlistTrackButtons.add(button);
            this.addRenderableWidget(button);
            buttonY += buttonHeight + buttonSpacing;
        }
    }
    
    private void createAvailableTrackButtons(int x, int y, int width, int buttonHeight, int buttonSpacing) {
        // Удаляем старые кнопки
        availableTrackButtons.forEach(this::removeWidget);
        availableTrackButtons.clear();
        
        List<CustomMusicTrack> availableTracks = getAvailableTracks();
        if (availableTracks.isEmpty()) {
            return;
        }
        
        int buttonY = y;
        for (int i = 0; i < availableTracks.size(); i++) {
            CustomMusicTrack track = availableTracks.get(i);
            final int index = i;
            
            Button button = Button.builder(
                Component.literal(track.getName()),
                (btn) -> {
                    selectedAvailableTrackIndex = index;
                    updateButtonStyles();
                }
            ).bounds(x, buttonY, width, buttonHeight).build();
            
            availableTrackButtons.add(button);
            this.addRenderableWidget(button);
            buttonY += buttonHeight + buttonSpacing;
        }
    }
    
    private List<CustomMusicTrack> getAvailableTracks() {
        List<CustomMusicTrack> allTracks = musicLoader.getTracks();
        List<String> playlistTrackNames = playlist.getTracks().stream()
            .map(CustomMusicTrack::getName)
            .collect(Collectors.toList());
        
        return allTracks.stream()
            .filter(track -> !playlistTrackNames.contains(track.getName()))
            .collect(Collectors.toList());
    }
    
    private void addTrackToPlaylist(CustomMusicTrack track) {
        List<CustomMusicTrack> tracks = new ArrayList<>(playlist.getTracks());
        tracks.add(track);
        playlist.setTracks(tracks);
        
        // Сохраняем координаты перед пересозданием кнопок
        int leftListX = playlistTrackButtons.isEmpty() ? 10 : (int) playlistTrackButtons.get(0).getX();
        int listTopY = playlistTrackButtons.isEmpty() ? 70 : (int) playlistTrackButtons.get(0).getY();
        int listWidth = playlistTrackButtons.isEmpty() ? 200 : playlistTrackButtons.get(0).getWidth();
        int buttonHeight = playlistTrackButtons.isEmpty() ? 20 : playlistTrackButtons.get(0).getHeight();
        int buttonSpacing = 2;
        
        createPlaylistTrackButtons(leftListX, listTopY, listWidth, buttonHeight, buttonSpacing);
        
        // Пересоздаем список доступных треков
        if (!availableTrackButtons.isEmpty()) {
            int rightListX = (int) availableTrackButtons.get(0).getX();
            createAvailableTrackButtons(rightListX, listTopY, listWidth, buttonHeight, buttonSpacing);
        } else {
            // Если список доступных треков пуст, используем координаты из init()
            int rightListX = this.width / 2 + 5;
            createAvailableTrackButtons(rightListX, listTopY, listWidth, buttonHeight, buttonSpacing);
        }
        
        selectedPlaylistTrackIndex = -1;
        selectedAvailableTrackIndex = -1;
        updateButtonStyles();
    }
    
    private void removeTrackFromPlaylist(int index) {
        List<CustomMusicTrack> tracks = new ArrayList<>(playlist.getTracks());
        if (index >= 0 && index < tracks.size()) {
            tracks.remove(index);
            playlist.setTracks(tracks);
            
            // Пересоздаем кнопки
            int leftListX = playlistTrackButtons.isEmpty() ? 10 : (int) playlistTrackButtons.get(0).getX();
            int listTopY = playlistTrackButtons.isEmpty() ? 70 : (int) playlistTrackButtons.get(0).getY();
            int listWidth = playlistTrackButtons.isEmpty() ? 200 : playlistTrackButtons.get(0).getWidth();
            int buttonHeight = 20;
            int buttonSpacing = 2;
            
            createPlaylistTrackButtons(leftListX, listTopY, listWidth, buttonHeight, buttonSpacing);
            
            // Пересоздаем список доступных треков
            if (!availableTrackButtons.isEmpty()) {
                int rightListX = (int) availableTrackButtons.get(0).getX();
                createAvailableTrackButtons(rightListX, listTopY, listWidth, buttonHeight, buttonSpacing);
            }
            
            selectedPlaylistTrackIndex = -1;
            selectedAvailableTrackIndex = -1;
            updateButtonStyles();
        }
    }
    
    private void updateButtonStyles() {
        // Обновляем стили кнопок треков в плейлисте
        for (int i = 0; i < playlistTrackButtons.size(); i++) {
            Button button = playlistTrackButtons.get(i);
            if (i == selectedPlaylistTrackIndex) {
                button.setFGColor(0xFFFFFF);
            } else {
                button.setFGColor(0xCCCCCC);
            }
        }
        
        // Обновляем стили кнопок доступных треков
        for (int i = 0; i < availableTrackButtons.size(); i++) {
            Button button = availableTrackButtons.get(i);
            if (i == selectedAvailableTrackIndex) {
                button.setFGColor(0xFFFFFF);
            } else {
                button.setFGColor(0xCCCCCC);
            }
        }
    }
    
    private void savePlaylist() {
        try {
            playlistManager.savePlaylist(playlist).get();
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Ошибка при сохранении плейлиста", e);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Заголовки списков
        int headerY = 50;
        int leftListX = 10;
        int rightListX = this.width / 2 + 5;
        int listWidth = (this.width - 30) / 2;
        
        guiGraphics.drawString(this.font, 
            Component.translatable("audiocontroller.gui.playlist_tracks"), 
            leftListX, headerY, 0xFFFFFF);
        guiGraphics.drawString(this.font, 
            Component.translatable("audiocontroller.gui.available_tracks"), 
            rightListX, headerY, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

