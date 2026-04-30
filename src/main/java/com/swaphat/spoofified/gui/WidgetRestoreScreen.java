package com.swaphat.spoofified.gui;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class WidgetRestoreScreen extends Screen {

    private final Screen previous;
    private Category currentCategory = Category.HIDDEN;
    private RestoreList restoreList;

    private enum Category {
        HIDDEN("Hidden"),
        EDITED("Edited"),
        DELETED("Deleted");

        final String title;
        Category(String title) { this.title = title; }
    }

    public WidgetRestoreScreen(Screen previous) {
        super(Component.literal("Manage Custom UI Widgets"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        int spacing = 10;
        int startX = this.width / 2 - (buttonWidth * 3 + spacing * 2) / 2;

        // ==========================================
        // THE NEW TOGGLE BUTTON
        // ==========================================
        addRenderableWidget(Button.builder(
                Component.literal("Auto-Recalculate: " + (ClientSpooferOptions.AUTO_RECALCULATE_UI ? "ON" : "OFF")),
                button -> {
                    // Flip the boolean
                    ClientSpooferOptions.AUTO_RECALCULATE_UI = !ClientSpooferOptions.AUTO_RECALCULATE_UI;

                    // Update the button text dynamically
                    button.setMessage(Component.literal("Auto-Recalculate: " + (ClientSpooferOptions.AUTO_RECALCULATE_UI ? "ON" : "OFF")));

                    // Save the new state to config
                    if (ClientSpoofer.CONFIG_FILE != null) {
                        ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);
                    }
                }
        ).bounds(10, 10, 150, 20).build());


        // Category Tabs
        addRenderableWidget(Button.builder(Component.literal(Category.HIDDEN.title), _ -> switchCategory(Category.HIDDEN))
                .bounds(startX, 10, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(Category.EDITED.title), _ -> switchCategory(Category.EDITED))
                .bounds(startX + buttonWidth + spacing, 10, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(Category.DELETED.title), _ -> switchCategory(Category.DELETED))
                .bounds(startX + (buttonWidth + spacing) * 2, 10, buttonWidth, 20).build());

        restoreList = new RestoreList(minecraft, this.width, this.height - 80, 40);
        addRenderableWidget(restoreList);
        refreshList();

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), _ -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private void switchCategory(Category newCategory) {
        this.currentCategory = newCategory;
        refreshList();
    }

    private void refreshList() {
        restoreList.clearEntries();
        Set<String> targetSet = switch (currentCategory) {
            case HIDDEN -> ClientSpooferOptions.HIDDEN_WIDGETS;
            case EDITED -> ClientSpooferOptions.CUSTOM_BOUNDS.keySet();
            case DELETED -> ClientSpooferOptions.DELETED_WIDGETS;
        };

        for (String uniqueId : targetSet) {
            restoreList.addEntry(new RestoreEntry(uniqueId, currentCategory));
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(previous);
        super.onClose();
    }

    private class RestoreList extends AbstractSelectionList<RestoreEntry> {
        public RestoreList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 45);
        }

        @Override
        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int addEntry(@NotNull RestoreEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {}

        @Override
        public int getRowWidth() { return 300; }
    }

    private class RestoreEntry extends ContainerObjectSelectionList.Entry<RestoreEntry> {
        private final String uniqueId;
        private final String displayScreen;
        private final String displayWidget;
        private final Category category;
        private final Button resetButton;
        private final int borderColor;

        public RestoreEntry(String uniqueId, Category category) {
            this.uniqueId = uniqueId;
            this.category = category;

            if (uniqueId.contains(":")) {
                String[] parts = uniqueId.split(":", 2);
                this.displayScreen = parts[0];
                this.displayWidget = parts[1];
            } else {
                this.displayScreen = "Global / Legacy";
                this.displayWidget = uniqueId;
            }

            Random random = new Random();
            this.borderColor = 0xFF000000 | random.nextInt(0xFFFFFF);

            this.resetButton = Button.builder(Component.literal("Reset"), _ -> {
                if (category == Category.HIDDEN) ClientSpooferOptions.HIDDEN_WIDGETS.remove(uniqueId);
                else if (category == Category.DELETED) ClientSpooferOptions.DELETED_WIDGETS.remove(uniqueId);
                else if (category == Category.EDITED) ClientSpooferOptions.CUSTOM_BOUNDS.remove(uniqueId);

                if (ClientSpoofer.CONFIG_FILE != null) ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);

                // Tie the reset button's recalculation to the toggle as well
                if (ClientSpooferOptions.AUTO_RECALCULATE_UI && WidgetRestoreScreen.this.previous != null) {
                    WidgetRestoreScreen.this.previous.resize(
                            WidgetRestoreScreen.this.width,
                            WidgetRestoreScreen.this.height
                    );
                }

                refreshList();
            }).bounds(0, 0, 60, 20).build();
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return List.of(resetButton);
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX();
            int y = getContentY();
            int width = 300;
            int height = 40;

            graphics.outline(x, y, width, height, borderColor);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x55000000);

            graphics.text(minecraft.font, "Screen: " + displayScreen, x + 5, y + 5, 0xFFAAAAAA);
            graphics.text(minecraft.font, "Widget: " + displayWidget, x + 5, y + 20, 0xFFFFFFFF);

            resetButton.setPosition(x + width - 65, y + 10);
            resetButton.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of(resetButton);
        }
    }
}