package com.swaphat.spoofified.gui;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PackConsentScreen extends Screen {
    private final Screen parent;

    public PackConsentScreen(Screen parent) {
        super(Component.literal("Resource Pack Security"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // LEFT BUTTON: Strip Visuals
        this.addRenderableWidget(Button.builder(Component.literal("Strip Visuals"), b -> {
            ClientSpooferOptions.PACK_STRIP_MODE = ClientSpooferOptions.PackStripMode.VISUALS_ONLY;
            ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);

            // Safely close the screen. Vanilla will automatically handle the reload
            // once the background download finishes.
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX - 160, centerY, 150, 20).build());

        // RIGHT BUTTON: Load Normally
        this.addRenderableWidget(Button.builder(Component.literal("Load Normally"), b -> {
            ClientSpooferOptions.PACK_STRIP_MODE = ClientSpooferOptions.PackStripMode.NONE;
            ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);

            // Safely close the screen. Vanilla will automatically handle the reload
            // once the background download finishes.
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX + 10, centerY, 150, 20).build());
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        // Draw a dark, semi-transparent shade over the entire screen (ARGB format)

        Component title = Component.literal("Server Resource Pack Downloading").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component subtitle = Component.literal("How would you like Spoofified to handle the assets?").withStyle(ChatFormatting.GRAY);

        graphics.centeredText(this.font, title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        graphics.centeredText(this.font, subtitle, this.width / 2, this.height / 2 - 20, 0xFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}