package com.audiocontroller.audio;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class AudioManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static AudioManager instance;
    
    private final MusicLoader musicLoader;
    private final PlaylistManager playlistManager;
    private final Random random = new Random();
    
    private Playlist activePlaylist;
    private CustomMusicTrack currentTrack;
    private LWJGLDirectAudioPlayer audioPlayer;
    private int ticksSinceLastMusic = 0;
    private int ticksSinceTrackStart = 0;
    private static final int MIN_TICKS_BETWEEN_MUSIC = 12000; // ~10 минут
    private static final int MIN_TICKS_BEFORE_CHECK = 60; // ~3 секунды - минимальное время перед проверкой окончания
    private static final String ACTIVE_PLAYLIST_FILE = "config/audiocontroller/active_playlist.txt";
    
    private AudioManager() {
        this.musicLoader = new MusicLoader();
        this.playlistManager = new PlaylistManager(musicLoader);
        this.audioPlayer = new LWJGLDirectAudioPlayer();
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public MusicLoader getMusicLoader() {
        return musicLoader;
    }

    public PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public void setActivePlaylist(Playlist playlist) {
        if (playlist == null) {
            stop();
            activePlaylist = null;
            // Удаляем файл активного плейлиста
            try {
                Files.deleteIfExists(Paths.get(ACTIVE_PLAYLIST_FILE));
            } catch (IOException e) {
                LOGGER.error("Ошибка при удалении файла активного плейлиста", e);
            }
            LOGGER.info("Активный плейлист сброшен");
            return;
        }
        
        // Загружаем треки в плейлист, если они еще не загружены
        if (playlist.getTracks().isEmpty() && !playlist.getTrackNames().isEmpty()) {
            LOGGER.info("Загружаем треки в плейлист: {}", playlist.getName());
            List<CustomMusicTrack> tracks = new ArrayList<>();
            for (String trackName : playlist.getTrackNames()) {
                musicLoader.getTrackByName(trackName).ifPresent(tracks::add);
            }
            playlist.setTracks(tracks);
            LOGGER.info("Загружено треков в плейлист: {}", tracks.size());
        }
        
        if (playlist.getTracks().isEmpty()) {
            LOGGER.warn("Плейлист {} пуст или треки не найдены", playlist.getName());
            return;
        }
        
        activePlaylist = playlist;
        playlistManager.setCurrentPlaylist(playlist);
        stop();
        
        // Сохраняем имя активного плейлиста
        saveActivePlaylistName(playlist.getName());
        
        // Сбрасываем счетчик тиков, чтобы музыка запустилась сразу
        ticksSinceLastMusic = MIN_TICKS_BETWEEN_MUSIC;
        
        // Автоматически запускаем случайный трек из плейлиста
        playRandomTrackFromPlaylist();
        LOGGER.info("Активный плейлист установлен: {} (треков: {})", playlist.getName(), playlist.getTracks().size());
    }
    
    private void saveActivePlaylistName(String playlistName) {
        try {
            Path file = Paths.get(ACTIVE_PLAYLIST_FILE);
            Files.createDirectories(file.getParent());
            Files.writeString(file, playlistName);
            LOGGER.info("Сохранено имя активного плейлиста: {} в файл: {}", playlistName, file.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Ошибка при сохранении активного плейлиста", e);
        }
    }
    
    public void loadActivePlaylist() {
        try {
            Path file = Paths.get(ACTIVE_PLAYLIST_FILE);
            if (!Files.exists(file)) {
                return;
            }
            
            String playlistName = Files.readString(file).trim();
            if (playlistName.isEmpty()) {
                return;
            }
            
            // Загружаем плейлисты, если они еще не загружены
            try {
                playlistManager.loadPlaylists().get();
            } catch (Exception e) {
                LOGGER.error("Ошибка при загрузке плейлистов", e);
                return;
            }
            
            // Ищем плейлист по имени
            Optional<Playlist> playlistOpt = playlistManager.getPlaylist(playlistName);
            if (playlistOpt.isPresent()) {
                Playlist playlist = playlistOpt.get();
                LOGGER.info("Загружен активный плейлист: {}", playlistName);
                // Устанавливаем плейлист, но не запускаем воспроизведение сразу
                // Воспроизведение запустится автоматически при входе в мир
                activePlaylist = playlist;
                playlistManager.setCurrentPlaylist(playlist);
                // Сбрасываем счетчик, чтобы музыка запустилась сразу при входе в мир
                ticksSinceLastMusic = MIN_TICKS_BETWEEN_MUSIC;
            } else {
                LOGGER.warn("Активный плейлист {} не найден", playlistName);
                // Удаляем файл, если плейлист не найден
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при загрузке активного плейлиста", e);
        }
    }

    public Optional<Playlist> getActivePlaylist() {
        return Optional.ofNullable(activePlaylist);
    }

    private void playRandomTrackFromPlaylist() {
        if (activePlaylist == null || activePlaylist.getTracks().isEmpty()) {
            return;
        }
        
        List<CustomMusicTrack> tracks = activePlaylist.getTracks();
        CustomMusicTrack randomTrack;
        
        if (activePlaylist.isShuffle()) {
            randomTrack = tracks.get(random.nextInt(tracks.size()));
        } else {
            // Используем текущий индекс или случайный, если индекс невалиден
            int index = activePlaylist.getCurrentIndex();
            if (index < 0 || index >= tracks.size()) {
                index = random.nextInt(tracks.size());
                activePlaylist.setCurrentIndex(index);
            }
            randomTrack = tracks.get(index);
        }
        
        playTrack(randomTrack);
    }

    public void playTrack(CustomMusicTrack track) {
        if (track == null) {
            LOGGER.warn("Попытка воспроизвести null трек");
            return;
        }

        stop();
        currentTrack = track;
        
        try {
            Path filePath = track.getFilePath();
            if (!Files.exists(filePath)) {
                LOGGER.error("Файл не найден: {}", filePath);
                return;
            }

            // Воспроизводим OGG файл напрямую через LWJGLDirectAudioPlayer
            playOGGFile(filePath);
            
        } catch (Exception e) {
            LOGGER.error("Ошибка при воспроизведении трека: {}", track.getName(), e);
        }
    }

    private void playOGGFile(Path filePath) {
        if (audioPlayer == null) {
            LOGGER.error("LWJGLDirectAudioPlayer не инициализирован");
            return;
        }
        try {
            audioPlayer.loadOGGFile(filePath);
            updateVolumeFromMinecraftSettings(); // Устанавливаем громкость перед воспроизведением
            audioPlayer.play();
            LOGGER.info("Запуск воспроизведения трека: {} (файл: {}) через LWJGL OpenAL", currentTrack.getName(), filePath);
        } catch (Exception e) {
            LOGGER.error("Ошибка при воспроизведении OGG файла через LWJGL OpenAL", e);
        }
    }

    public void stop() {
        if (audioPlayer != null) {
            audioPlayer.stop();
        }
        currentTrack = null;
        ticksSinceLastMusic = 0;
        ticksSinceTrackStart = 0;
        LOGGER.debug("Воспроизведение остановлено");
    }
    
    /**
     * Ставит музыку на паузу
     */
    public void pause() {
        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();
            LOGGER.debug("Музыка поставлена на паузу");
        }
    }
    
    /**
     * Возобновляет воспроизведение музыки
     */
    public void resume() {
        if (audioPlayer != null && currentTrack != null) {
            audioPlayer.resume();
        }
    }

    public void setVolume(float volume) {
        // Этот метод больше не используется напрямую
        // Громкость теперь берется из настроек Minecraft
        updateVolumeFromMinecraftSettings();
    }

    public float getVolume() {
        // Получаем громкость из настроек Minecraft
        return getMinecraftMusicVolume();
    }
    
    /**
     * Получает громкость музыки из настроек Minecraft
     * Учитывает как громкость музыки, так и общую громкость (master volume)
     */
    private float getMinecraftMusicVolume() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            try {
                // Получаем громкость музыки из настроек Minecraft
                float musicVolume = (float) mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC);
                // Получаем общую громкость (master volume)
                float masterVolume = (float) mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER);
                // Итоговая громкость = громкость музыки * общая громкость
                return musicVolume * masterVolume;
            } catch (Exception e) {
                // Если метод не найден, пробуем через рефлексию
                try {
                    java.lang.reflect.Field musicVolumeField = mc.options.getClass().getDeclaredField("musicVolume");
                    musicVolumeField.setAccessible(true);
                    Object musicVolumeValue = musicVolumeField.get(mc.options);
                    float musicVolume = 1.0f;
                    if (musicVolumeValue instanceof Double) {
                        musicVolume = ((Double) musicVolumeValue).floatValue();
                    } else if (musicVolumeValue instanceof Float) {
                        musicVolume = (Float) musicVolumeValue;
                    }
                    
                    // Пробуем получить общую громкость
                    try {
                        java.lang.reflect.Field masterVolumeField = mc.options.getClass().getDeclaredField("masterVolume");
                        masterVolumeField.setAccessible(true);
                        Object masterVolumeValue = masterVolumeField.get(mc.options);
                        float masterVolume = 1.0f;
                        if (masterVolumeValue instanceof Double) {
                            masterVolume = ((Double) masterVolumeValue).floatValue();
                        } else if (masterVolumeValue instanceof Float) {
                            masterVolume = (Float) masterVolumeValue;
                        }
                        return musicVolume * masterVolume;
                    } catch (Exception ex) {
                        // Если не удалось получить общую громкость, возвращаем только громкость музыки
                        return musicVolume;
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Не удалось получить громкость музыки из настроек Minecraft", ex);
                }
                return 1.0f;
            }
        }
        return 1.0f;
    }
    
    /**
     * Обновляет громкость аудиоплеера из настроек Minecraft
     */
    private void updateVolumeFromMinecraftSettings() {
        float minecraftVolume = getMinecraftMusicVolume();
        if (audioPlayer != null) {
            audioPlayer.setVolume(minecraftVolume);
        }
    }

    public boolean isPlaying() {
        return audioPlayer != null && audioPlayer.isPlaying();
    }

    public Optional<CustomMusicTrack> getCurrentTrack() {
        return Optional.ofNullable(currentTrack);
    }

    public void tick() {
        if (activePlaylist == null || activePlaylist.getTracks().isEmpty()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        // Проверяем, что мы в мире и игра не на паузе
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        
        // Обновляем громкость из настроек Minecraft каждый тик
        // (на случай, если пользователь изменил настройки)
        updateVolumeFromMinecraftSettings();
        
        // Проверяем, закончился ли текущий трек
        if (audioPlayer != null && currentTrack != null) {
            ticksSinceTrackStart++;
            
            // Проверяем окончание трека после минимальной задержки
            if (ticksSinceTrackStart >= MIN_TICKS_BEFORE_CHECK) {
                // Проверяем, играет ли ещё звук через OpenAL
                if (!audioPlayer.isPlaying()) {
                    // Трек закончился, переходим к следующему
                    LOGGER.info("Трек {} закончился, переходим к следующему", currentTrack.getName());
                    ticksSinceTrackStart = 0;
                    playNextTrackFromPlaylist();
                    return; // Выходим, чтобы не запускать новую музыку сразу
                }
            }
        }
        
        // Если музыка не играет и прошло достаточно времени, запускаем новую
        if (!isPlaying()) {
            ticksSinceLastMusic++;
            // Если счетчик уже достиг минимума или больше, запускаем музыку сразу
            if (ticksSinceLastMusic >= MIN_TICKS_BETWEEN_MUSIC) {
                playRandomTrackFromPlaylist();
                ticksSinceLastMusic = 0;
            }
        } else {
            ticksSinceLastMusic = 0;
        }
    }

    private void playNextTrackFromPlaylist() {
        if (activePlaylist == null || activePlaylist.getTracks().isEmpty()) {
            return;
        }
        
        CustomMusicTrack nextTrack = activePlaylist.getNextTrack(random);
        if (nextTrack != null) {
            playTrack(nextTrack);
        } else {
            // Плейлист закончился и repeat выключен
            LOGGER.info("Плейлист {} закончился", activePlaylist.getName());
        }
    }

    public void shutdown() {
        stop();
        if (audioPlayer != null) {
            audioPlayer.cleanup();
        }
        musicLoader.shutdown();
    }
}
