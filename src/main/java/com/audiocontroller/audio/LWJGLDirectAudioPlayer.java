package com.audiocontroller.audio;

import com.mojang.logging.LogUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Класс для прямого воспроизведения OGG файлов через LWJGL OpenAL
 * Обходит SoundManager Minecraft и использует OpenAL напрямую
 */
public class LWJGLDirectAudioPlayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    int buffer = -1; // Package-private для доступа из AudioManager
    int source = -1; // Package-private для доступа из AudioManager
    private float volume = 1.0f;
    private Path currentFile = null;
    
    /**
     * Загружает и декодирует OGG файл
     * @param filePath путь к OGG файлу
     * @return true если файл успешно загружен
     */
    public boolean loadOGGFile(Path filePath) {
        if (!Files.exists(filePath)) {
            LOGGER.error("Файл не найден: {}", filePath);
            return false;
        }
        
        // Освобождаем предыдущие ресурсы
        cleanup();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);
            
            String filePathStr = filePath.toAbsolutePath().toString();
            ShortBuffer rawAudioBuffer = STBVorbis.stb_vorbis_decode_filename(filePathStr, channelsBuffer, sampleRateBuffer);
            
            if (rawAudioBuffer == null) {
                LOGGER.error("Не удалось декодировать OGG файл: {}", filePath);
                return false;
            }
            
            int channels = channelsBuffer.get();
            int sampleRate = sampleRateBuffer.get();
            
            // Определяем формат на основе количества каналов
            int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            
            // Создаем буфер OpenAL
            IntBuffer bufferBuffer = stack.mallocInt(1);
            AL10.alGenBuffers(bufferBuffer);
            buffer = bufferBuffer.get();
            
            if (buffer == 0) {
                LOGGER.error("Не удалось создать буфер OpenAL");
                MemoryUtil.memFree(rawAudioBuffer);
                return false;
            }
            
            // Загружаем аудиоданные в буфер
            AL10.alBufferData(buffer, format, rawAudioBuffer, sampleRate);
            
            // Освобождаем память, выделенную STB
            MemoryUtil.memFree(rawAudioBuffer);
            
            // Создаем источник OpenAL
            IntBuffer sourceBuffer = stack.mallocInt(1);
            AL10.alGenSources(sourceBuffer);
            source = sourceBuffer.get();
            
            if (source == 0) {
                LOGGER.error("Не удалось создать источник OpenAL");
                AL10.alDeleteBuffers(buffer);
                buffer = -1;
                return false;
            }
            
            // Привязываем буфер к источнику
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            
            // Устанавливаем громкость
            AL10.alSourcef(source, AL10.AL_GAIN, volume);
            
            // Устанавливаем источник как музыку (не 3D позиционирование)
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
            
            currentFile = filePath;
            LOGGER.info("Успешно загружен OGG файл: {} (каналы: {}, частота: {} Hz)", filePath, channels, sampleRate);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Ошибка при загрузке OGG файла: {}", filePath, e);
            cleanup();
            return false;
        }
    }
    
    /**
     * Начинает воспроизведение загруженного файла
     */
    public void play() {
        if (source == -1) {
            LOGGER.warn("Попытка воспроизведения без загруженного файла");
            return;
        }
        
        int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
        if (state == AL10.AL_PLAYING) {
            // Уже играет
            return;
        }
        
        if (state == AL10.AL_PAUSED) {
            // Возобновляем с паузы
            AL10.alSourcePlay(source);
        } else {
            // Начинаем воспроизведение с начала
            AL10.alSourcePlay(source);
        }
        LOGGER.debug("Начато воспроизведение через OpenAL");
    }
    
    /**
     * Останавливает воспроизведение
     */
    public void stop() {
        if (source != -1) {
            AL10.alSourceStop(source);
            LOGGER.debug("Остановлено воспроизведение через OpenAL");
        }
    }
    
    /**
     * Ставит воспроизведение на паузу
     */
    public void pause() {
        if (source != -1 && isPlaying()) {
            AL10.alSourcePause(source);
            LOGGER.debug("Поставлено на паузу через OpenAL");
        }
    }
    
    /**
     * Возобновляет воспроизведение с паузы
     */
    public void resume() {
        if (source != -1) {
            try {
                int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                if (state == AL10.AL_PAUSED) {
                    AL10.alSourcePlay(source);
                    LOGGER.debug("Возобновлено воспроизведение через OpenAL");
                }
            } catch (Exception e) {
                LOGGER.warn("Ошибка при возобновлении воспроизведения", e);
            }
        }
    }
    
    /**
     * Проверяет, играет ли звук в данный момент
     */
    public boolean isPlaying() {
        if (source == -1) {
            return false;
        }
        
        try {
            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            return (state == AL10.AL_PLAYING);
        } catch (Exception e) {
            // Если произошла ошибка при проверке состояния, считаем что не играет
            LOGGER.warn("Ошибка при проверке состояния воспроизведения OpenAL", e);
            return false;
        }
    }
    
    /**
     * Устанавливает громкость (0.0 - 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        if (source != -1) {
            AL10.alSourcef(source, AL10.AL_GAIN, this.volume);
        }
    }
    
    /**
     * Получает текущую громкость
     */
    public float getVolume() {
        return volume;
    }
    
    /**
     * Освобождает все ресурсы OpenAL
     */
    public void cleanup() {
        if (source != -1) {
            AL10.alSourceStop(source);
            AL10.alDeleteSources(source);
            source = -1;
        }
        
        if (buffer != -1) {
            AL10.alDeleteBuffers(buffer);
            buffer = -1;
        }
        
        currentFile = null;
    }
    
    /**
     * Получает путь к текущему файлу
     */
    public Path getCurrentFile() {
        return currentFile;
    }
}

