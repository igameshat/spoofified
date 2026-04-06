package de.fabiexe.clientspoofer.gui;

import de.fabiexe.clientspoofer.ClientSpoofer;
import de.fabiexe.clientspoofer.ClientSpooferOptions;
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

        // Top Navigation Tabs
        addRenderableWidget(Button.builder(Component.literal(Category.HIDDEN.title), _ -> switchCategory(Category.HIDDEN))
                .bounds(startX, 10, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(Category.EDITED.title), _ -> switchCategory(Category.EDITED))
                .bounds(startX + buttonWidth + spacing, 10, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(Category.DELETED.title), _ -> switchCategory(Category.DELETED))
                .bounds(startX + (buttonWidth + spacing) * 2, 10, buttonWidth, 20).build());

        // The Scrollable List
        restoreList = new RestoreList(minecraft, this.width, this.height - 80, 40);
        addRenderableWidget(restoreList);
        refreshList();

        // Bottom Done Button
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

        for (String widgetName : targetSet) {
            restoreList.addEntry(new RestoreEntry(widgetName, currentCategory));
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
        super.onClose();
    }

    // ==========================================
    // INNER CLASSES FOR THE LIST
    // ==========================================
    private class RestoreList extends AbstractSelectionList<RestoreEntry> {
        public RestoreList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 45); // 45 is the height of each row
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {}

        @Override
        protected int addEntry(@NonNull RestoreEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        protected void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return 300; // Width of the bordered boxes
        }
    }

    private class RestoreEntry extends ContainerObjectSelectionList.Entry<RestoreEntry> {
        private final String widgetName;
        private final Category category;
        private final Button resetButton;
        private final int borderColor;

        public RestoreEntry(String widgetName, Category category) {
            this.widgetName = widgetName;
            this.category = category;

            // Generate a random solid color for the border (max opacity)
            Random random = new Random();
            this.borderColor = 0xFF000000 | random.nextInt(0xFFFFFF);

            this.resetButton = Button.builder(Component.literal("Reset"), _ -> {
                // Remove the widget from the respective config map/set
                if (category == Category.HIDDEN) ClientSpooferOptions.HIDDEN_WIDGETS.remove(widgetName);
                else if (category == Category.DELETED) ClientSpooferOptions.DELETED_WIDGETS.remove(widgetName);
                else if (category == Category.EDITED) ClientSpooferOptions.CUSTOM_BOUNDS.remove(widgetName);

                // Save instantly and refresh the screen
                if (ClientSpoofer.CONFIG_FILE != null) ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);
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

            // Draw the random colored border
            graphics.outline(x, y, width, height, borderColor);
            // Optional: Draw a faint background inside the border so it's easier to read
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x55000000);

            // Draw the Texts
            graphics.text(minecraft.font, "Screen: Unknown (Requires Tracker Update)", x + 5, y + 5, 0xFFAAAAAA);
            graphics.text(minecraft.font, "Widget: " + widgetName, x + 5, y + 20, 0xFFFFFFFF);

            // Update button position and draw it
            resetButton.setPosition(x + width - 65, y + 10);
            resetButton.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of(resetButton);
        }
    }
}