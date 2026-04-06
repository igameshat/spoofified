package de.fabiexe.clientspoofer.gui;

import de.fabiexe.clientspoofer.ClientSpoofer;
import de.fabiexe.clientspoofer.ClientSpooferOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;

import static net.minecraft.network.chat.CommonComponents.OPTION_OFF;
import static net.minecraft.network.chat.CommonComponents.OPTION_ON;

public class ClientModSpoofingScreen extends Screen {

    private final Screen previous;
    private String modSearch = "";
    private ModHideList modHideList;

    public ClientModSpoofingScreen(Screen previous) {
        super(Component.literal("Hidden Mods Configuration"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        int columnWidth = (this.width - 60) / 2;
        int leftColX = 20;
        int rightColX = 40 + columnWidth;

        // ==========================================
        // TOP CENTER: Global Spoofer Toggle
        // ==========================================
        Component enabledText = Component.literal("Spoofer Enabled: ")
                .append(ClientSpooferOptions.ENABLED ? OPTION_ON.copy().withStyle(ChatFormatting.GREEN) : OPTION_OFF.copy().withStyle(ChatFormatting.RED));

        addRenderableWidget(Button.builder(enabledText, _ -> {
            ClientSpooferOptions.ENABLED = !ClientSpooferOptions.ENABLED;
            rebuildWidgets();
        }).bounds(this.width / 2 - 100, 5, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Manage Custom UI"), _ -> {
            minecraft.setScreen(new WidgetRestoreScreen(this));
        }).bounds(this.width - 160, 5, 150, 20).build());

        // ==========================================
        // LEFT COLUMN: Hidden Mods List
        // ==========================================
        addRenderableWidget(new MultiLineTextWidget(
                leftColX, 35,
                Component.literal("Select Mods to Hide").withStyle(ChatFormatting.YELLOW),
                font).setMaxWidth(columnWidth));

        EditBox searchEditBox = new EditBox(font, leftColX, 50, columnWidth, 20, Component.literal(""));
        searchEditBox.setHint(Component.literal("Search mods..."));
        searchEditBox.setValue(modSearch);
        searchEditBox.setResponder(value -> {
            modSearch = value;
            fillModHideList();
        });
        addRenderableWidget(searchEditBox);

        modHideList = new ModHideList(minecraft, columnWidth, this.height - 110, 75);
        modHideList.setX(leftColX);
        fillModHideList();
        addRenderableWidget(modHideList);

        // ==========================================
        // RIGHT COLUMN: Custom Dictionaries
        // ==========================================
        int halfBoxHeight = (this.height - 120) / 2;

        // Custom Keys
        addRenderableWidget(new MultiLineTextWidget(
                rightColX, 35,
                Component.literal("Hidden Keybinds (One per line)").withStyle(ChatFormatting.YELLOW),
                font).setMaxWidth(columnWidth));

        MultiLineEditBox keysBox = MultiLineEditBox.builder().build(
                font, columnWidth, halfBoxHeight, Component.literal("Hidden Keybinds"));
        keysBox.setPosition(rightColX, 50);
        keysBox.setValue(String.join("\n", ClientSpooferOptions.CUSTOM_HIDDEN_KEYS));
        keysBox.setValueListener(value -> updateSetFromString(value, ClientSpooferOptions.CUSTOM_HIDDEN_KEYS));
        addRenderableWidget(keysBox);

        // Custom Commands
        int commandsY = 50 + halfBoxHeight + 20;
        addRenderableWidget(new MultiLineTextWidget(
                rightColX, commandsY - 15,
                Component.literal("Hidden Commands (One per line)").withStyle(ChatFormatting.YELLOW),
                font).setMaxWidth(columnWidth));

        MultiLineEditBox commandsBox = MultiLineEditBox.builder().build(
                font, columnWidth, halfBoxHeight, Component.literal("Hidden Commands"));
        commandsBox.setPosition(rightColX, commandsY);
        commandsBox.setValue(String.join("\n", ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS));
        commandsBox.setValueListener(value -> updateSetFromString(value, ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS));
        addRenderableWidget(commandsBox);

        // ==========================================
        // BOTTOM: Done Button
        // ==========================================
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), _ -> onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    private void updateSetFromString(String value, Set<String> targetSet) {
        targetSet.clear();
        for (String line : value.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                targetSet.add(trimmed);
            }
        }
    }

    @Override
    public void onClose() {
        ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);

        if (ClientSpooferOptions.onConfigChanged != null) {
            ClientSpooferOptions.onConfigChanged.run();
        }

        Minecraft.getInstance().setScreen(previous);
        super.onClose();
    }

    private void fillModHideList() {
        modHideList.clearEntries();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getType().equals("builtin")) continue;

            boolean matches = true;
            for (String term : modSearch.split(" ")) {
                if (!term.isBlank() &&
                        !mod.getMetadata().getName().toLowerCase().contains(term.toLowerCase()) &&
                        !mod.getMetadata().getId().toLowerCase().contains(term.toLowerCase())) {
                    matches = false;
                }
            }

            if (matches) {
                modHideList.addEntry(new ModHideEntry(mod));
            }
        }
    }

    // ==========================================
    // INNER CLASSES FOR MOD LIST
    // ==========================================
    private class ModHideList extends AbstractSelectionList<ModHideEntry> {
        public ModHideList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, 21);
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {}

        @Override
        protected int addEntry(@NonNull ModHideEntry entry) {
            return super.addEntry(entry);
        }

        @Override
        protected void clearEntries() {
            super.clearEntries();
        }
    }

    private class ModHideEntry extends ContainerObjectSelectionList.Entry<ModHideEntry> {
        private final Checkbox checkbox;

        public ModHideEntry(@NotNull ModContainer mod) {
            // Note: This modifies HIDDEN_MODS directly, unlike the main screen which modifies ALLOWED_MODS
            checkbox = Checkbox.builder(Component.literal(mod.getMetadata().getName()), font)
                    .selected(ClientSpooferOptions.HIDDEN_MODS.contains(mod.getMetadata().getId()))
                    .onValueChange((_, value) -> {
                        String modId = mod.getMetadata().getId();
                        if (value) {
                            ClientSpooferOptions.HIDDEN_MODS.add(modId);
                        } else {
                            ClientSpooferOptions.HIDDEN_MODS.remove(modId);
                        }
                    })
                    .build();
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return List.of(checkbox);
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            checkbox.setPosition(getContentX() + 11, getContentY());
            checkbox.extractContents(graphics, mouseX, mouseY, delta);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return List.of(checkbox);
        }
    }
}