package com.audiocontroller.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlaylistManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLAYLIST_FOLDER = "config/audiocontroller/playlists";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final MusicLoader musicLoader;
    private final Map<String, Playlist> playlists = new HashMap<>();
    private Playlist currentPlaylist;

    public PlaylistManager(MusicLoader musicLoader) {
        this.musicLoader = musicLoader;
        ensurePlaylistDirectoryExists();
    }

    private void ensurePlaylistDirectoryExists() {
        try {
            Path playlistDir = Paths.get(PLAYLIST_FOLDER);
            if (!Files.exists(playlistDir)) {
                Files.createDirectories(playlistDir);
                LOGGER.info("Создана папка для плейлистов: {}", playlistDir.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при создании папки для плейлистов", e);
        }
    }

    public CompletableFuture<Void> loadPlaylists() {
        return CompletableFuture.runAsync(() -> {
            try {
                playlists.clear();
                Path playlistDir = Paths.get(PLAYLIST_FOLDER);
                
                if (!Files.exists(playlistDir)) {
                    ensurePlaylistDirectoryExists();
                    return;
                }

                Files.list(playlistDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String jsonContent = Files.readString(path);
                            JsonObject json = GSON.fromJson(jsonContent, JsonObject.class);
                            Playlist playlist = Playlist.fromJson(json);
                            
                            // Загружаем треки из имен
                            List<CustomMusicTrack> tracks = new ArrayList<>();
                            for (String trackName : playlist.getTrackNames()) {
                                musicLoader.getTrackByName(trackName).ifPresent(tracks::add);
                            }
                            playlist.setTracks(tracks);
                            
                            playlists.put(playlist.getName(), playlist);
                            LOGGER.debug("Загружен плейлист: {}", playlist.getName());
                        } catch (IOException e) {
                            LOGGER.error("Ошибка при загрузке плейлиста: {}", path, e);
                        }
                    });

                LOGGER.info("Загружено плейлистов: {}", playlists.size());
            } catch (IOException e) {
                LOGGER.error("Ошибка при сканировании папки с плейлистами", e);
            }
        });
    }

    public CompletableFuture<Void> savePlaylist(Playlist playlist) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path playlistDir = Paths.get(PLAYLIST_FOLDER);
                ensurePlaylistDirectoryExists();
                
                String fileName = playlist.getName().replaceAll("[^a-zA-Z0-9_]", "_") + ".json";
                Path filePath = playlistDir.resolve(fileName);
                
                JsonObject json = playlist.toJson();
                String jsonContent = GSON.toJson(json);
                
                Files.writeString(filePath, jsonContent);
                playlists.put(playlist.getName(), playlist);
                
                LOGGER.info("Плейлист сохранён: {}", playlist.getName());
            } catch (IOException e) {
                LOGGER.error("Ошибка при сохранении плейлиста: {}", playlist.getName(), e);
            }
        });
    }

    public CompletableFuture<Void> deletePlaylist(String name) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path playlistDir = Paths.get(PLAYLIST_FOLDER);
                String fileName = name.replaceAll("[^a-zA-Z0-9_]", "_") + ".json";
                Path filePath = playlistDir.resolve(fileName);
                
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    playlists.remove(name);
                    if (currentPlaylist != null && currentPlaylist.getName().equals(name)) {
                        currentPlaylist = null;
                    }
                    LOGGER.info("Плейлист удалён: {}", name);
                }
            } catch (IOException e) {
                LOGGER.error("Ошибка при удалении плейлиста: {}", name, e);
            }
        });
    }

    public Playlist createPlaylist(String name) {
        if (playlists.containsKey(name)) {
            LOGGER.warn("Плейлист с именем {} уже существует", name);
            return playlists.get(name);
        }
        
        Playlist playlist = new Playlist(name);
        playlists.put(name, playlist);
        savePlaylist(playlist);
        return playlist;
    }

    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists.values());
    }

    public Optional<Playlist> getPlaylist(String name) {
        return Optional.ofNullable(playlists.get(name));
    }

    public void setCurrentPlaylist(Playlist playlist) {
        this.currentPlaylist = playlist;
    }

    public Optional<Playlist> getCurrentPlaylist() {
        return Optional.ofNullable(currentPlaylist);
    }

    public Path getPlaylistDirectory() {
        return Paths.get(PLAYLIST_FOLDER);
    }
}

