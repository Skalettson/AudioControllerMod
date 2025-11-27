package com.audiocontroller.client.gui;

import com.audiocontroller.audio.AudioManager;
import com.audiocontroller.audio.CustomMusicTrack;
import com.audiocontroller.audio.Playlist;
import com.audiocontroller.audio.PlaylistManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PlaylistScreen extends Screen {
    private final Screen parent;
    private final AudioManager audioManager;
    private final PlaylistManager playlistManager;
    private EditBox playlistNameBox;
    private Button createButton;
    private Button deleteButton;
    private Button editButton;
    private Button shuffleButton;
    private Button repeatButton;
    private int selectedPlaylistIndex = -1;
    private final List<Button> playlistButtons = new ArrayList<>();

    public PlaylistScreen(Screen parent) {
        super(Component.translatable("audiocontroller.gui.playlists"));
        this.parent = parent;
        this.audioManager = AudioManager.getInstance();
        this.playlistManager = audioManager.getPlaylistManager();
    }

    @Override
    protected void init() {
        super.init();
        
        // Загружаем плейлисты синхронно перед инициализацией GUI
        try {
            playlistManager.loadPlaylists().get(); // Блокирующее ожидание
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Ошибка при загрузке плейлистов", e);
        }
        
        // Адаптивные размеры и позиции
        int padding = Math.max(10, this.width / 40);
        int buttonWidth = Math.min(200, Math.max(150, this.width / 4));
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int topY = 40;
        int listTopY = topY + 60;
        int listHeight = Math.min(150, this.height - listTopY - 200);
        int listWidth = Math.min(this.width - padding * 2, 300);
        int listX = (this.width - listWidth) / 2;
        
        // Поле ввода имени плейлиста
        int inputY = topY + 5;
        this.playlistNameBox = new EditBox(this.font, centerX - buttonWidth / 2, inputY, buttonWidth, buttonHeight, Component.literal(""));
        this.playlistNameBox.setMaxLength(32);
        this.addRenderableWidget(this.playlistNameBox);
        
        // Кнопка создания плейлиста
        int createButtonY = inputY + buttonHeight + 5;
        this.createButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.create_playlist"),
            button -> {
                String name = playlistNameBox.getValue().trim();
                if (!name.isEmpty()) {
                    Playlist playlist = playlistManager.createPlaylist(name);
                    try {
                        // Дождаться завершения сохранения
                        playlistManager.savePlaylist(playlist).get();
                        refreshPlaylistList();
                        playlistNameBox.setValue("");
                    } catch (Exception e) {
                        com.mojang.logging.LogUtils.getLogger().error("Ошибка при сохранении плейлиста", e);
                    }
                }
            }
        ).bounds(centerX - buttonWidth / 2, createButtonY, buttonWidth, buttonHeight).build());
        
        // Список плейлистов будет создан через createPlaylistButtons()
        createPlaylistButtons();
        
        // Кнопки управления (адаптивные позиции)
        // Вычисляем позицию кнопок после списка плейлистов
        int maxButtons = playlistManager.getPlaylists().size();
        int listItemHeight = 20;
        int listItemSpacing = 2;
        int listBottomY = listTopY + (maxButtons * (listItemHeight + listItemSpacing));
        int buttonsY = listBottomY + 10;
        int controlButtonSpacing = 10;
        int smallButtonWidth = Math.min(90, (buttonWidth - controlButtonSpacing) / 2);
        
        // Кнопка выбора активного плейлиста
        this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.select_playlist"),
            button -> {
                if (selectedPlaylistIndex >= 0) {
                    List<Playlist> playlists = playlistManager.getPlaylists();
                    if (selectedPlaylistIndex < playlists.size()) {
                        Playlist playlist = playlists.get(selectedPlaylistIndex);
                        audioManager.setActivePlaylist(playlist);
                        // Закрываем экран после выбора
                        this.minecraft.setScreen(this.parent);
                    }
                }
            }
        ).bounds(centerX - buttonWidth / 2, buttonsY, buttonWidth, buttonHeight).build());
        
        // Кнопки управления
        int controlButtonsY = buttonsY + buttonHeight + 5;
        this.editButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.edit_playlist"),
            button -> {
                if (selectedPlaylistIndex >= 0) {
                    List<Playlist> playlists = playlistManager.getPlaylists();
                    if (selectedPlaylistIndex < playlists.size()) {
                        Playlist playlist = playlists.get(selectedPlaylistIndex);
                        // Загружаем треки в плейлист перед открытием экрана редактирования
                        loadPlaylistTracks(playlist);
                        this.minecraft.setScreen(new EditPlaylistScreen(this, playlist));
                    }
                }
            }
        ).bounds(centerX - buttonWidth / 2, controlButtonsY, smallButtonWidth, buttonHeight).build());
        
        this.deleteButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.delete_playlist"),
            button -> {
                if (selectedPlaylistIndex >= 0) {
                    List<Playlist> playlists = playlistManager.getPlaylists();
                    if (selectedPlaylistIndex < playlists.size()) {
                        Playlist playlist = playlists.get(selectedPlaylistIndex);
                        try {
                            // Дождаться завершения удаления
                            playlistManager.deletePlaylist(playlist.getName()).get();
                            refreshPlaylistList();
                            selectedPlaylistIndex = -1;
                        } catch (Exception e) {
                            com.mojang.logging.LogUtils.getLogger().error("Ошибка при удалении плейлиста", e);
                        }
                    }
                }
            }
        ).bounds(centerX - buttonWidth / 2 + smallButtonWidth + controlButtonSpacing, controlButtonsY, smallButtonWidth, buttonHeight).build());
        
        // Кнопки настроек
        int settingsButtonsY = controlButtonsY + buttonHeight + 5;
        this.shuffleButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.shuffle"),
            button -> {
                if (selectedPlaylistIndex >= 0) {
                    List<Playlist> playlists = playlistManager.getPlaylists();
                    if (selectedPlaylistIndex < playlists.size()) {
                        Playlist playlist = playlists.get(selectedPlaylistIndex);
                        playlist.setShuffle(!playlist.isShuffle());
                        try {
                            // Дождаться завершения сохранения
                            playlistManager.savePlaylist(playlist).get();
                            updateShuffleButton();
                        } catch (Exception e) {
                            com.mojang.logging.LogUtils.getLogger().error("Ошибка при сохранении плейлиста", e);
                        }
                    }
                }
            }
        ).bounds(centerX - buttonWidth / 2, settingsButtonsY, smallButtonWidth, buttonHeight).build());
        
        this.repeatButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.gui.repeat"),
            button -> {
                if (selectedPlaylistIndex >= 0) {
                    List<Playlist> playlists = playlistManager.getPlaylists();
                    if (selectedPlaylistIndex < playlists.size()) {
                        Playlist playlist = playlists.get(selectedPlaylistIndex);
                        playlist.setRepeat(!playlist.isRepeat());
                        try {
                            // Дождаться завершения сохранения
                            playlistManager.savePlaylist(playlist).get();
                            updateRepeatButton();
                        } catch (Exception e) {
                            com.mojang.logging.LogUtils.getLogger().error("Ошибка при сохранении плейлиста", e);
                        }
                    }
                }
            }
        ).bounds(centerX - buttonWidth / 2 + smallButtonWidth + controlButtonSpacing, settingsButtonsY, smallButtonWidth, buttonHeight).build());
        
        // Кнопка назад
        int backButtonY = Math.min(settingsButtonsY + buttonHeight + 10, this.height - buttonHeight - 10);
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.back"),
            button -> this.minecraft.setScreen(this.parent)
        ).bounds(centerX - buttonWidth / 2, backButtonY, buttonWidth, buttonHeight).build());
        
        updateButtons();
    }

    private void loadPlaylistTracks(Playlist playlist) {
        // Загружаем треки из имен, если они еще не загружены
        if (playlist.getTracks().isEmpty() && !playlist.getTrackNames().isEmpty()) {
            List<CustomMusicTrack> tracks = new ArrayList<>();
            for (String trackName : playlist.getTrackNames()) {
                audioManager.getMusicLoader().getTrackByName(trackName).ifPresent(tracks::add);
            }
            playlist.setTracks(tracks);
        }
    }
    
    private void refreshPlaylistList() {
        try {
            // Блокирующее ожидание завершения загрузки
            playlistManager.loadPlaylists().get();
            createPlaylistButtons();
            updateButtons();
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Ошибка при обновлении списка плейлистов", e);
        }
    }
    
    private void createPlaylistButtons() {
        // Удаляем старые кнопки
        playlistButtons.forEach(this::removeWidget);
        playlistButtons.clear();
        
        List<Playlist> playlists = playlistManager.getPlaylists();
        if (playlists.isEmpty()) {
            return;
        }
        
        // Адаптивные размеры
        int padding = Math.max(10, this.width / 40);
        int listWidth = Math.min(this.width - padding * 2, 300);
        int listX = (this.width - listWidth) / 2;
        int listTopY = 100;
        int buttonHeight = 20;
        int buttonSpacing = 2;
        int buttonY = listTopY;
        
        // Создаем кнопку для каждого плейлиста
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            final int index = i;
            
            String displayText = playlist.getName() + " (" + playlist.getTracks().size() + " треков)";
            Button button = Button.builder(
                Component.literal(displayText),
                (btn) -> {
                    selectedPlaylistIndex = index;
                    updateButtonStyles();
                    updateButtons();
                }
            ).bounds(listX, buttonY, listWidth, buttonHeight).build();
            
            playlistButtons.add(button);
            this.addRenderableWidget(button);
            buttonY += buttonHeight + buttonSpacing;
        }
        
        updateButtonStyles();
    }
    
    private void updateButtonStyles() {
        for (int i = 0; i < playlistButtons.size(); i++) {
            Button button = playlistButtons.get(i);
            if (i == selectedPlaylistIndex) {
                // Выбранная кнопка - белый текст
                button.setFGColor(0xFFFFFF);
            } else {
                // Невыбранная кнопка - серый текст
                button.setFGColor(0xCCCCCC);
            }
        }
    }

    private void updateButtons() {
        boolean hasSelection = selectedPlaylistIndex >= 0 && 
                              selectedPlaylistIndex < playlistManager.getPlaylists().size();
        // Кнопка выбора активного плейлиста всегда активна, если выбран плейлист
        this.editButton.active = hasSelection;
        this.deleteButton.active = hasSelection;
        this.shuffleButton.active = hasSelection;
        this.repeatButton.active = hasSelection;
        updateShuffleButton();
        updateRepeatButton();
    }

    private void updateShuffleButton() {
        if (selectedPlaylistIndex >= 0) {
            List<Playlist> playlists = playlistManager.getPlaylists();
            if (selectedPlaylistIndex < playlists.size()) {
                Playlist playlist = playlists.get(selectedPlaylistIndex);
                this.shuffleButton.setMessage(Component.translatable("audiocontroller.gui.shuffle")
                    .append(": " + (playlist.isShuffle() ? "✓" : "✗")));
            }
        }
    }

    private void updateRepeatButton() {
        if (selectedPlaylistIndex >= 0) {
            List<Playlist> playlists = playlistManager.getPlaylists();
            if (selectedPlaylistIndex < playlists.size()) {
                Playlist playlist = playlists.get(selectedPlaylistIndex);
                this.repeatButton.setMessage(Component.translatable("audiocontroller.gui.repeat")
                    .append(": " + (playlist.isRepeat() ? "✓" : "✗")));
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Рендерим остальные виджеты
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (playlistNameBox.isFocused()) {
            return playlistNameBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (playlistNameBox.isFocused()) {
            if (playlistNameBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}


