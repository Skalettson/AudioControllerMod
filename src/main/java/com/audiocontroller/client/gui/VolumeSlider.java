package com.audiocontroller.client.gui;

import com.audiocontroller.audio.AudioManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class VolumeSlider extends AbstractSliderButton {
    public VolumeSlider(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message, value);
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.translatable("audiocontroller.gui.volume")
            .append(": " + (int)(this.value * 100) + "%"));
    }

    @Override
    protected void applyValue() {
        // Изменяем громкость музыки в настройках Minecraft напрямую
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            try {
                // В Minecraft 1.20.1 используем рефлексию для доступа к полю musicVolume
                java.lang.reflect.Field musicVolumeField = mc.options.getClass().getDeclaredField("musicVolume");
                musicVolumeField.setAccessible(true);
                // В Minecraft Options громкость хранится как Double
                musicVolumeField.set(mc.options, (double) this.value);
            } catch (Exception e) {
                // Если рефлексия не работает, просто обновляем через AudioManager
                // Громкость будет обновляться автоматически при следующем тике
            }
            // Обновляем громкость в AudioManager
            AudioManager.getInstance().setVolume((float) this.value);
        }
    }
}

