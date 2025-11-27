package com.audiocontroller.audio;

import java.nio.file.Path;
import java.time.Duration;

public class CustomMusicTrack {
    private final String name;
    private final Path filePath;
    private final long fileSize;
    private Duration duration;
    private boolean loaded;

    public CustomMusicTrack(String name, Path filePath, long fileSize) {
        this.name = name;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.loaded = false;
    }

    public String getName() {
        return name;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public String toString() {
        return name;
    }
}

