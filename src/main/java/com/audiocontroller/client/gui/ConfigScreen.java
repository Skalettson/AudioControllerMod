package com.audiocontroller.client.gui;

import com.audiocontroller.config.ConfigHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private Button replaceMusicButton;
    private Button autoPlayButton;
    private Button doneButton;
    
    public ConfigScreen(Screen parent) {
        super(Component.translatable("audiocontroller.config.title"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;
        int startY = 60;
        
        // Кнопка "Заменить стандартную музыку"
        this.replaceMusicButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.config.replaceVanillaMusic")
                .append(": " + (ConfigHandler.REPLACE_VANILLA_MUSIC.get() ? 
                    Component.translatable("options.on").getString() : 
                    Component.translatable("options.off").getString())),
            button -> {
                boolean newValue = !ConfigHandler.REPLACE_VANILLA_MUSIC.get();
                ConfigHandler.REPLACE_VANILLA_MUSIC.set(newValue);
                button.setMessage(Component.translatable("audiocontroller.config.replaceVanillaMusic")
                    .append(": " + (newValue ? 
                        Component.translatable("options.on").getString() : 
                        Component.translatable("options.off").getString())));
                // Сохраняем конфигурацию
                ConfigHandler.SPEC.save();
            }
        ).bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());
        
        // Кнопка "Автозапуск"
        this.autoPlayButton = this.addRenderableWidget(Button.builder(
            Component.translatable("audiocontroller.config.autoPlay")
                .append(": " + (ConfigHandler.AUTO_PLAY.get() ? 
                    Component.translatable("options.on").getString() : 
                    Component.translatable("options.off").getString())),
            button -> {
                boolean newValue = !ConfigHandler.AUTO_PLAY.get();
                ConfigHandler.AUTO_PLAY.set(newValue);
                button.setMessage(Component.translatable("audiocontroller.config.autoPlay")
                    .append(": " + (newValue ? 
                        Component.translatable("options.on").getString() : 
                        Component.translatable("options.off").getString())));
                // Сохраняем конфигурацию
                ConfigHandler.SPEC.save();
            }
        ).bounds(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build());
        
        // Кнопка "Готово"
        this.doneButton = this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            button -> {
                ConfigHandler.SPEC.save();
                this.minecraft.setScreen(this.parent);
            }
        ).bounds(centerX - buttonWidth / 2, this.height - 30, buttonWidth, buttonHeight).build());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Информационный текст
        int infoY = this.height - 60;
        String infoText = Component.translatable("audiocontroller.config.info").getString();
        int maxWidth = this.width - 40;
        if (this.font.width(infoText) > maxWidth) {
            infoText = this.font.plainSubstrByWidth(infoText, maxWidth - 10) + "...";
        }
        guiGraphics.drawCenteredString(this.font, infoText, this.width / 2, infoY, 0xAAAAAA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        // Сохраняем конфигурацию при закрытии
        ConfigHandler.SPEC.save();
        this.minecraft.setScreen(this.parent);
    }
}

