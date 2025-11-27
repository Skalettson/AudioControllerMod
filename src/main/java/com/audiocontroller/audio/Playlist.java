package com.audiocontroller.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Playlist {
    private String name;
    private List<String> trackNames;
    private boolean shuffle;
    private boolean repeat;
    private int currentIndex;
    private transient List<CustomMusicTrack> tracks;

    public Playlist(String name) {
        this.name = name;
        this.trackNames = new ArrayList<>();
        this.shuffle = false;
        this.repeat = true;
        this.currentIndex = 0;
        this.tracks = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTrackNames() {
        return trackNames;
    }

    public void setTrackNames(List<String> trackNames) {
        this.trackNames = trackNames;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public List<CustomMusicTrack> getTracks() {
        return tracks;
    }

    public void setTracks(List<CustomMusicTrack> tracks) {
        this.tracks = tracks;
        this.trackNames = new ArrayList<>();
        for (CustomMusicTrack track : tracks) {
            this.trackNames.add(track.getName());
        }
    }

    public CustomMusicTrack getCurrentTrack() {
        if (tracks == null || tracks.isEmpty()) {
            return null;
        }
        if (currentIndex >= 0 && currentIndex < tracks.size()) {
            return tracks.get(currentIndex);
        }
        return tracks.get(0);
    }

    public CustomMusicTrack getNextTrack(Random random) {
        if (tracks == null || tracks.isEmpty()) {
            return null;
        }
        
        if (shuffle) {
            currentIndex = random.nextInt(tracks.size());
        } else {
            currentIndex++;
            if (currentIndex >= tracks.size()) {
                if (repeat) {
                    currentIndex = 0;
                } else {
                    return null;
                }
            }
        }
        
        return getCurrentTrack();
    }
    
    public CustomMusicTrack getNextTrack() {
        return getNextTrack(new Random());
    }

    public CustomMusicTrack getPreviousTrack() {
        if (tracks == null || tracks.isEmpty()) {
            return null;
        }
        
        if (shuffle) {
            Random random = new Random();
            currentIndex = random.nextInt(tracks.size());
        } else {
            currentIndex--;
            if (currentIndex < 0) {
                if (repeat) {
                    currentIndex = tracks.size() - 1;
                } else {
                    return null;
                }
            }
        }
        
        return getCurrentTrack();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("shuffle", shuffle);
        json.addProperty("repeat", repeat);
        json.addProperty("currentIndex", currentIndex);
        
        JsonArray tracksArray = new JsonArray();
        for (String trackName : trackNames) {
            tracksArray.add(trackName);
        }
        json.add("tracks", tracksArray);
        
        return json;
    }

    public static Playlist fromJson(JsonObject json) {
        Playlist playlist = new Playlist(json.get("name").getAsString());
        playlist.setShuffle(json.has("shuffle") && json.get("shuffle").getAsBoolean());
        playlist.setRepeat(json.has("repeat") && json.get("repeat").getAsBoolean());
        playlist.setCurrentIndex(json.has("currentIndex") ? json.get("currentIndex").getAsInt() : 0);
        
        JsonArray tracksArray = json.getAsJsonArray("tracks");
        List<String> trackNames = new ArrayList<>();
        for (int i = 0; i < tracksArray.size(); i++) {
            trackNames.add(tracksArray.get(i).getAsString());
        }
        playlist.setTrackNames(trackNames);
        
        return playlist;
    }
}

