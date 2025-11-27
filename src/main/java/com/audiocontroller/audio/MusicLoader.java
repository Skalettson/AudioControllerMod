package com.audiocontroller.audio;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MUSIC_FOLDER = "config/audiocontroller/music";
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".ogg");
    
    private final List<CustomMusicTrack> tracks = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AudioController-MusicLoader");
        t.setDaemon(true);
        return t;
    });
    private boolean isLoading = false;

    public MusicLoader() {
        ensureMusicDirectoryExists();
    }

    private void ensureMusicDirectoryExists() {
        try {
            Path musicDir = Paths.get(MUSIC_FOLDER);
            if (!Files.exists(musicDir)) {
                Files.createDirectories(musicDir);
                LOGGER.info("Создана папка для музыки: {}", musicDir.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при создании папки для музыки", e);
        }
    }

    public CompletableFuture<Void> scanMusicFiles() {
        if (isLoading) {
            return CompletableFuture.completedFuture(null);
        }

        isLoading = true;
        return CompletableFuture.runAsync(() -> {
            try {
                List<CustomMusicTrack> newTracks = new ArrayList<>();
                Path musicDir = Paths.get(MUSIC_FOLDER);

                if (!Files.exists(musicDir)) {
                    ensureMusicDirectoryExists();
                    isLoading = false;
                    return;
                }

                Files.walkFileTree(musicDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String fileName = file.getFileName().toString().toLowerCase();
                        String extension = fileName.substring(fileName.lastIndexOf('.'));
                        
                        if (SUPPORTED_EXTENSIONS.contains(extension)) {
                            try {
                                long fileSize = Files.size(file);
                                String trackName = file.getFileName().toString();
                                
                                // Убираем расширение из имени
                                if (trackName.endsWith(extension)) {
                                    trackName = trackName.substring(0, trackName.length() - extension.length());
                                }
                                
                                CustomMusicTrack track = new CustomMusicTrack(trackName, file, fileSize);
                                newTracks.add(track);
                                LOGGER.debug("Найден трек: {}", trackName);
                            } catch (IOException e) {
                                LOGGER.warn("Ошибка при чтении файла: {}", file, e);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                synchronized (tracks) {
                    tracks.clear();
                    tracks.addAll(newTracks);
                }

                LOGGER.info("Загружено треков: {}", newTracks.size());
            } catch (IOException e) {
                LOGGER.error("Ошибка при сканировании папки с музыкой", e);
            } finally {
                isLoading = false;
            }
        }, executorService);
    }

    public List<CustomMusicTrack> getTracks() {
        synchronized (tracks) {
            return new ArrayList<>(tracks);
        }
    }

    public Optional<CustomMusicTrack> getTrackByName(String name) {
        synchronized (tracks) {
            return tracks.stream()
                    .filter(track -> track.getName().equals(name))
                    .findFirst();
        }
    }

    public Path getMusicDirectory() {
        return Paths.get(MUSIC_FOLDER);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public boolean isLoading() {
        return isLoading;
    }
}

