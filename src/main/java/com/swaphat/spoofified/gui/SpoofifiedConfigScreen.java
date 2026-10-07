package com.swaphat.spoofified.gui;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.SpoofMode;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.network.chat.CommonComponents.OPTION_OFF;
import static net.minecraft.network.chat.CommonComponents.OPTION_ON;

/**
 * ClientSpoofer settings screen.
 * Layout: a category sidebar on the left and ONE vanilla scrolling list on the right. Every category is just a
 * different set of rows in that list, so every category scrolls with vanilla physics and nothing can fall off the
 * bottom of the screen. Every control reads/writes the real {@link ClientSpooferOptions} field directly; there is no
 * second copy of the configuration.
 */
public class SpoofifiedConfigScreen extends Screen {

    // ---------------------------------------------------------------- layout
    private static final int SIDE_MARGIN = 10;
    private static final int SIDEBAR_MIN_WIDTH = 80;
    private static final int SIDEBAR_MAX_WIDTH = 120;
    private static final int COLUMN_GAP = 8;
    private static final int MAX_CONTENT_WIDTH = 360;
    private static final int MIN_CONTENT_WIDTH = 120;
    private static final int TITLE_Y = 10;
    private static final int TOP = 28;
    private static final int BOTTOM_BAR = 32;
    private static final int DONE_WIDTH = 200;
    private static final int DONE_BOTTOM_OFFSET = 26;
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;
    private static final int MIN_TAB_HEIGHT = 14;
    private static final int TAB_GAP = 4;
    private static final int SEARCH_ROW_HEIGHT = 24;
    private static final int SCROLLBAR_PADDING = 24;
    private static final int ENTRY_INSET = 4;
    private static final int INLINE_GAP = 4;
    private static final int ADD_BUTTON_WIDTH = 50;
    private static final int REMOVE_BUTTON_WIDTH = 60;
    private static final int MIN_LIST_HEIGHT = 40;

    private static final int MAX_FIELD_LENGTH = 1024;
    private static final int MAX_CLIENT_LENGTH = 128;

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_HEADER = 0xFFFFFF55;
    private static final int COLOR_TEXT = 0xFFCCCCCC;

    // ------------------------------------------------- explicit cycle orders
    private static final SpoofMode[] SPOOF_MODES = {
            SpoofMode.VANILLA, SpoofMode.MODDED, SpoofMode.CUSTOM, SpoofMode.LUNAR, SpoofMode.BADLION, SpoofMode.OFF
    };
    private static final ClientSpooferOptions.NotificationMode[] ALERT_MODES = {
            ClientSpooferOptions.NotificationMode.NOTHING, ClientSpooferOptions.NotificationMode.TOAST,
            ClientSpooferOptions.NotificationMode.CHAT, ClientSpooferOptions.NotificationMode.BOTH
    };
    private static final ClientSpooferOptions.FilterStrategy[] FILTER_STRATEGIES = {
            ClientSpooferOptions.FilterStrategy.ALLOWLIST, ClientSpooferOptions.FilterStrategy.BLOCKLIST
    };
    private static final ClientSpooferOptions.PayloadPolicy[] PAYLOAD_POLICIES = {
            ClientSpooferOptions.PayloadPolicy.ALLOW_ALL, ClientSpooferOptions.PayloadPolicy.ALLOWLIST,
            ClientSpooferOptions.PayloadPolicy.BLOCKLIST
    };
    private static final ClientSpooferOptions.PackStripMode[] PACK_STRIP_MODES = {
            ClientSpooferOptions.PackStripMode.NONE, ClientSpooferOptions.PackStripMode.SHADERS_ONLY,
            ClientSpooferOptions.PackStripMode.VISUALS_ONLY, ClientSpooferOptions.PackStripMode.FULL
    };

    // ----------------------------------------------------------------- state
    private enum Category {
        GENERAL("spoofified.screen.config.category.general"),
        MODS("spoofified.screen.config.category.mods"),
        PAYLOADS("spoofified.screen.config.category.payloads"),
        KEYBINDS("spoofified.screen.config.category.keybinds"),
        RESOURCE_PACKS("spoofified.screen.config.category.resource_packs"),
        PRIVACY("spoofified.screen.config.category.privacy"),
        UI("spoofified.screen.config.category.ui");

        final String key;

        Category(String key) {
            this.key = key;
        }
    }

    private record ModInfo(String id, String name) {}

    private final Screen previous;
    private Category currentCategory = Category.GENERAL;

    // Temporary GUI-only state (never persisted)
    private String modSearch = "";
    private final Map<String, String> drafts = new HashMap<>();

    private int contentLeft;
    private int contentWidth;
    private int entryWidth;
    private ConfigList list;

    public SpoofifiedConfigScreen(Screen previous) {
        super(Component.translatable("spoofified.screen.config.title"));
        this.previous = previous;
    }

    // ------------------------------------------------------------- lifecycle

    @Override
    protected void init() {
        int sidebarWidth = Math.clamp(this.width / 4, SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH);
        int available = this.width - SIDE_MARGIN * 2 - sidebarWidth - COLUMN_GAP;
        contentWidth = Math.clamp(available, MIN_CONTENT_WIDTH, MAX_CONTENT_WIDTH);
        contentLeft = SIDE_MARGIN + sidebarWidth + COLUMN_GAP + Math.max(0, (available - contentWidth) / 2);
        entryWidth = contentWidth - SCROLLBAR_PADDING - ENTRY_INSET;

        int bottom = this.height - BOTTOM_BAR;

        // Category sidebar (shrinks its buttons on very short screens instead of overflowing)
        Category[] categories = Category.values();
        int tabHeight = Math.clamp(
                (bottom - TOP - (long) TAB_GAP * (categories.length - 1)) / categories.length, MIN_TAB_HEIGHT, WIDGET_HEIGHT);
        int tabY = TOP;
        for (Category category : categories) {
            Component label = category == currentCategory
                    ? Component.translatable(category.key).copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE)
                    : Component.translatable(category.key);
            addRenderableWidget(Button.builder(label, _ -> switchCategory(category))
                    .bounds(SIDE_MARGIN, tabY, sidebarWidth, tabHeight).build());
            tabY += tabHeight + TAB_GAP;
        }

        int doneWidth = Math.min(DONE_WIDTH, this.width - SIDE_MARGIN * 2);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), _ -> onClose())
                .bounds(this.width / 2 - doneWidth / 2, this.height - DONE_BOTTOM_OFFSET, doneWidth, WIDGET_HEIGHT).build());

        // The mod search box lives outside the list so it keeps focus while the list is refilled
        int listTop = TOP;
        if (currentCategory == Category.MODS) {
            EditBox search = getEditBox();
            addRenderableWidget(search);
            listTop += SEARCH_ROW_HEIGHT;
        }

        list = new ConfigList(minecraft, contentWidth, Math.max(MIN_LIST_HEIGHT, bottom - listTop), listTop);
        list.setX(contentLeft);
        addRenderableWidget(list);
        populate();
    }

    private @NonNull EditBox getEditBox() {
        EditBox search = new EditBox(font, contentLeft, TOP, contentWidth - SCROLLBAR_PADDING, WIDGET_HEIGHT,
                Component.translatable("spoofified.screen.mod_spoofing.search_hint"));
        search.setHint(Component.translatable("spoofified.screen.mod_spoofing.search_hint"));
        search.setMaxLength(MAX_FIELD_LENGTH);
        search.setValue(modSearch);
        search.setResponder(value -> {
            modSearch = value;
            refill();
        });
        return search;
    }

    private void switchCategory(Category category) {
        this.currentCategory = category;
        rebuildWidgets(); // new list instance => scroll position resets to the top
    }

    /** Re-creates the rows of the current category in place (scroll position is kept by the list). */
    private void refill() {
        if (list == null) return;
        list.clearEntries();
        populate();
    }

    private void populate() {
        switch (currentCategory) {
            case GENERAL -> buildGeneral();
            case MODS -> buildMods();
            case PAYLOADS -> buildPayloads();
            case KEYBINDS -> buildKeybinds();
            case RESOURCE_PACKS -> buildResourcePacks();
            case PRIVACY -> buildPrivacy();
            case UI -> buildUi();
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        graphics.text(this.font, this.getTitle(), (this.width - this.font.width(this.getTitle())) / 2, TITLE_Y, COLOR_TITLE, false);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void onClose() {
        if (ClientSpoofer.CONFIG_FILE != null) {
            ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);
        }
        if (ClientSpooferOptions.onConfigChanged != null) {
            ClientSpooferOptions.onConfigChanged.run();
        }
        Minecraft.getInstance().setScreenAndShow(previous);
        super.onClose();
    }

    // ------------------------------------------------------------ categories

    private void buildGeneral() {
        addWidget(toggle("spoofified.screen.config.enabled",
                () -> ClientSpooferOptions.ENABLED, v -> ClientSpooferOptions.ENABLED = v, false));

        addWidget(cycle("clientspoofer.option.spoof_mode", SPOOF_MODES,
                () -> ClientSpooferOptions.SPOOF_MODE, v -> ClientSpooferOptions.SPOOF_MODE = v,
                SpoofifiedConfigScreen::spoofModeName, true));

        // Custom-only settings are shown only while the Custom spoof mode is selected
        if (ClientSpooferOptions.SPOOF_MODE == SpoofMode.CUSTOM) {
            addLabel("clientspoofer.option.custom_client");
            addWidget(textField("clientspoofer.option.custom_client", null,
                    () -> ClientSpooferOptions.CUSTOM_CLIENT, v -> ClientSpooferOptions.CUSTOM_CLIENT = v,
                    MAX_CLIENT_LENGTH, true));
            addWidget(toggle("clientspoofer.option.custom_hide_mods",
                    () -> ClientSpooferOptions.CUSTOM_HIDE_MODS, v -> ClientSpooferOptions.CUSTOM_HIDE_MODS = v, false));
            addWidget(toggle("clientspoofer.option.custom_prevent_fingerprinting",
                    () -> ClientSpooferOptions.CUSTOM_PREVENT_FINGERPRINTING,
                    v -> ClientSpooferOptions.CUSTOM_PREVENT_FINGERPRINTING = v, false));
        }

        addWidget(cycle("spoofified.screen.config.alert_mode", ALERT_MODES,
                () -> ClientSpooferOptions.ALERT_MODE, v -> ClientSpooferOptions.ALERT_MODE = v,
                v -> enumName("spoofified.screen.config.alert_mode.", v), false));

        addWidget(toggle("spoofified.screen.config.panic_mode",
                () -> ClientSpooferOptions.PANIC_MODE, v -> ClientSpooferOptions.PANIC_MODE = v, false));
    }

    private void buildMods() {
        addWidget(cycle("spoofified.screen.config.mod_filter_strategy", FILTER_STRATEGIES,
                () -> ClientSpooferOptions.MOD_FILTER_STRATEGY, v -> ClientSpooferOptions.MOD_FILTER_STRATEGY = v,
                v -> enumName("spoofified.screen.config.strategy.", v), false));
        addLabel("spoofified.screen.config.mod_filter_list");

        Set<String> filter = ClientSpooferOptions.MOD_FILTER_LIST;
        List<ModInfo> mods = new ArrayList<>();
        Set<String> installedIds = new TreeSet<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getType().equals("builtin")) continue;
            installedIds.add(mod.getMetadata().getId());
            mods.add(new ModInfo(mod.getMetadata().getId(), mod.getMetadata().getName()));
        }
        // IDs that are in the filter but not installed stay visible so they can still be removed
        for (String id : new TreeSet<>(filter)) {
            if (!installedIds.contains(id)) mods.add(new ModInfo(id, id));
        }
        mods.sort(Comparator.comparing(m -> m.name().toLowerCase(Locale.ROOT)));

        for (ModInfo mod : mods) {
            if (matchesSearch(mod)) {
                list.addEntry(new ModEntry(mod, filter));
            }
        }
    }

    private boolean matchesSearch(ModInfo mod) {
        String name = mod.name().toLowerCase(Locale.ROOT);
        String id = mod.id().toLowerCase(Locale.ROOT);
        for (String term : modSearch.toLowerCase(Locale.ROOT).split(" ")) {
            if (!term.isBlank() && !name.contains(term) && !id.contains(term)) return false;
        }
        return true;
    }

    private void buildPayloads() {
        addWidget(cycle("spoofified.screen.config.payload_policy", PAYLOAD_POLICIES,
                () -> ClientSpooferOptions.PAYLOAD_POLICY, v -> ClientSpooferOptions.PAYLOAD_POLICY = v,
                v -> enumName("spoofified.screen.config.policy.", v), false));
        addSetEditor("payload_channels", "spoofified.screen.config.payload_channels",
                "spoofified.screen.config.type_channel", ClientSpooferOptions.PAYLOAD_CHANNELS);
    }

    private void buildKeybinds() {
        addWidget(toggle("spoofified.screen.config.fake_keybinds_layout",
                () -> ClientSpooferOptions.FAKE_KEYBINDS_LAYOUT, v -> ClientSpooferOptions.FAKE_KEYBINDS_LAYOUT = v, false));
        addSetEditor("hidden_keybinds", "spoofified.screen.config.hidden_keybinds",
                "spoofified.screen.config.type_keybind", ClientSpooferOptions.CUSTOM_HIDDEN_KEYS);
        addSetEditor("hidden_commands", "spoofified.screen.config.hidden_commands",
                "spoofified.screen.config.type_command", ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS);
    }

    private void buildResourcePacks() {
        addWidget(cycle("spoofified.screen.config.pack_strip_mode", PACK_STRIP_MODES,
                () -> ClientSpooferOptions.PACK_STRIP_MODE, v -> ClientSpooferOptions.PACK_STRIP_MODE = v,
                v -> enumName("spoofified.screen.config.pack_strip.", v), false));

        // Structural: toggling re-creates the rows so the URL box switches between enabled and disabled
        addWidget(toggle("spoofified.screen.config.proxy_resource_packs",
                () -> ClientSpooferOptions.PROXY_RESOURCE_PACKS, v -> ClientSpooferOptions.PROXY_RESOURCE_PACKS = v, true));
        addLabel("spoofified.screen.config.proxy_url");
        // A disabled box keeps (and never clears) the stored URL
        addWidget(textField("spoofified.screen.config.proxy_url", "spoofified.screen.config.proxy_url_hint",
                () -> ClientSpooferOptions.PROXY_URL, v -> ClientSpooferOptions.PROXY_URL = v,
                MAX_FIELD_LENGTH, ClientSpooferOptions.PROXY_RESOURCE_PACKS));

        addWidget(toggle("spoofified.screen.config.block_local_packs",
                () -> ClientSpooferOptions.BLOCK_LOCAL_PACKS, v -> ClientSpooferOptions.BLOCK_LOCAL_PACKS = v, false));

        // GLOBAL_PACK_PROFILE (PackFilterProfile) is intentionally not editable here: its structure lives in
        // com.swaphat.spoofified.util.filter and cannot be represented safely without touching that class.
    }

    private void buildPrivacy() {
        addWidget(toggle("spoofified.screen.config.block_telemetry",
                () -> ClientSpooferOptions.BLOCK_TELEMETRY, v -> ClientSpooferOptions.BLOCK_TELEMETRY = v, false));
        addWidget(toggle("spoofified.screen.config.block_friends",
                () -> ClientSpooferOptions.BLOCK_FRIENDS, v -> ClientSpooferOptions.BLOCK_FRIENDS = v, false));
        addWidget(toggle("spoofified.screen.config.block_realms",
                () -> ClientSpooferOptions.BLOCK_REALMS, v -> ClientSpooferOptions.BLOCK_REALMS = v, false));
        addWidget(toggle("spoofified.screen.config.block_server_cookies",
                () -> ClientSpooferOptions.BLOCK_SERVER_COOKIES, v -> ClientSpooferOptions.BLOCK_SERVER_COOKIES = v, false));
        addWidget(toggle("spoofified.screen.config.run_continuous_watchdog",
                () -> ClientSpooferOptions.RUN_CONTINUOUS_WATCHDOG, v -> ClientSpooferOptions.RUN_CONTINUOUS_WATCHDOG = v, false));
    }

    private void buildUi() {
        addWidget(toggle("spoofified.screen.config.auto_recalculate_ui",
                () -> ClientSpooferOptions.AUTO_RECALCULATE_UI, v -> ClientSpooferOptions.AUTO_RECALCULATE_UI = v, false));
        addWidget(toggle("spoofified.screen.config.hide_chat_mentions",
                () -> ClientSpooferOptions.HIDE_CHAT_MENTIONS, v -> ClientSpooferOptions.HIDE_CHAT_MENTIONS = v, false));

        // INVISIBLE_WIDGETS, REMOVED_WIDGETS and CUSTOM_BOUNDS are keyed by "<ScreenClass>:<button text>" ids that are
        // only meaningful while the owning screen is open, so they are managed by the existing WidgetRestoreScreen
        // (reset/restore) instead of being re-implemented here. LOCKED_WIDGETS is never exposed or modified.
        addWidget(Button.builder(Component.translatable("spoofified.screen.mod_spoofing.manage_custom_changes"),
                        _ -> minecraft.setScreenAndShow(new WidgetRestoreScreen(this)))
                .bounds(0, 0, entryWidth, WIDGET_HEIGHT).build());
        addWidget(Button.builder(Component.translatable("spoofified.screen.mod_spoofing.manage_log_history"),
                        _ -> minecraft.setScreenAndShow(new LogBrowserScreen()))
                .bounds(0, 0, entryWidth, WIDGET_HEIGHT).build());
    }

    // --------------------------------------------------------------- helpers

    private void addWidget(AbstractWidget widget) {
        list.addEntry(new RowEntry(widget));
    }

    private void addLabel(String translationKey) {
        list.addEntry(new LabelEntry(Component.translatable(translationKey)));
    }

    private static Component optionText(String nameKey, Component value) {
        return Component.translatable("options.generic_value", Component.translatable(nameKey), value);
    }

    private static Component enumName(String keyPrefix, Enum<?> value) {
        return Component.translatable(keyPrefix + value.name().toLowerCase(Locale.ROOT));
    }

    private static Component spoofModeName(SpoofMode mode) {
        return switch (mode) {
            case VANILLA -> Component.translatable("clientspoofer.option.spoof_mode.vanilla");
            case MODDED -> Component.translatable("clientspoofer.option.spoof_mode.modded");
            case CUSTOM -> Component.translatable("clientspoofer.option.spoof_mode.custom");
            case LUNAR -> Component.translatable("clientspoofer.option.spoof_mode.lunar");
            case BADLION -> Component.translatable("clientspoofer.option.spoof_mode.badlion");
            case OFF -> OPTION_OFF;
        };
    }

    /** ON/OFF button. When {@code structural} is true the rows are rebuilt (used for dependent settings). */
    private Button toggle(String nameKey, BooleanSupplier getter, Consumer<Boolean> setter, boolean structural) {
        return Button.builder(optionText(nameKey, getter.getAsBoolean() ? OPTION_ON : OPTION_OFF), button -> {
            setter.accept(!getter.getAsBoolean());
            if (structural) {
                refill();
            } else {
                button.setMessage(optionText(nameKey, getter.getAsBoolean() ? OPTION_ON : OPTION_OFF));
            }
        }).bounds(0, 0, entryWidth, WIDGET_HEIGHT).build();
    }

    /** Button cycling through {@code order}; displays translated names, never raw enum identifiers. */
    private <T> Button cycle(String nameKey, T[] order, Supplier<T> getter, Consumer<T> setter,
                             Function<T, Component> displayName, boolean structural) {
        return Button.builder(optionText(nameKey, displayName.apply(getter.get())), button -> {
            int index = -1;
            T current = getter.get();
            for (int i = 0; i < order.length; i++) {
                if (order[i] == current) {
                    index = i;
                    break;
                }
            }
            T next = order[(index + 1) % order.length];
            setter.accept(next);
            if (structural) {
                refill();
            } else {
                button.setMessage(optionText(nameKey, displayName.apply(next)));
            }
        }).bounds(0, 0, entryWidth, WIDGET_HEIGHT).build();
    }

    private EditBox textField(String labelKey, String hintKey, Supplier<String> getter, Consumer<String> setter,
                              int maxLength, boolean enabled) {
        EditBox box = new EditBox(font, 0, 0, entryWidth, WIDGET_HEIGHT, Component.translatable(labelKey));
        box.setMaxLength(maxLength);
        if (hintKey != null) box.setHint(Component.translatable(hintKey));
        box.setValue(getter.get());
        box.setResponder(setter);
        box.setEditable(enabled);
        box.active = enabled;
        return box;
    }

    /** Add-row followed by one removable row per entry. Operates directly on the real Set (no duplicates possible). */
    private void addSetEditor(String draftKey, String labelKey, String hintKey, Set<String> target) {
        addLabel(labelKey);

        EditBox input = new EditBox(font, 0, 0, entryWidth - ADD_BUTTON_WIDTH - INLINE_GAP, WIDGET_HEIGHT,
                Component.translatable(labelKey));
        input.setMaxLength(MAX_FIELD_LENGTH);
        input.setHint(Component.translatable(hintKey));
        input.setValue(drafts.getOrDefault(draftKey, ""));
        input.setResponder(value -> drafts.put(draftKey, value));

        Button add = Button.builder(Component.translatable("spoofified.screen.config.add"), _ -> {
            String value = input.getValue().trim();
            if (!value.isEmpty()) {
                target.add(value);
                drafts.remove(draftKey);
                refill();
            }
        }).bounds(0, 0, ADD_BUTTON_WIDTH, WIDGET_HEIGHT).build();
        list.addEntry(new RowEntry(input, add));

        List<String> sorted = new ArrayList<>(target);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        for (String value : sorted) {
            Button remove = Button.builder(Component.translatable("spoofified.screen.config.remove"), _ -> {
                target.remove(value);
                refill();
            }).bounds(0, 0, REMOVE_BUTTON_WIDTH, WIDGET_HEIGHT).build();
            list.addEntry(new TextRowEntry(value, remove));
        }
    }

    // ------------------------------------------------------------------ list

    private static class ConfigList extends AbstractSelectionList<ConfigEntry> {
        ConfigList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ROW_HEIGHT);
        }

        @Override
        protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {}

        @Override
        public int getRowWidth() {
            return this.width - SCROLLBAR_PADDING;
        }

        @Override
        public void clearEntries() {
            super.clearEntries();
        }

        @Override
        public int addEntry(@NonNull ConfigEntry entry) {
            return super.addEntry(entry);
        }
    }

    private abstract static class ConfigEntry extends ContainerObjectSelectionList.Entry<ConfigEntry> {}

    /** One or two widgets on a row: the first is left-aligned, the second right-aligned. */
    private class RowEntry extends ConfigEntry {
        private final List<AbstractWidget> widgets;

        RowEntry(AbstractWidget... widgets) {
            this.widgets = List.of(widgets);
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return widgets;
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX();
            int y = getContentY();
            widgets.getFirst().setPosition(x, y);
            if (widgets.size() > 1) {
                AbstractWidget last = widgets.getLast();
                last.setPosition(x + entryWidth - last.getWidth(), y);
            }
            for (AbstractWidget widget : widgets) {
                widget.extractRenderState(graphics, mouseX, mouseY, delta);
            }
        }
    }

    /** Dynamic text (a set value) on the left with a button on the right. */
    private class TextRowEntry extends ConfigEntry {
        private final String text;
        private final Button button;

        TextRowEntry(String text, Button button) {
            this.text = text;
            this.button = button;
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return List.of(button);
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX();
            int y = getContentY();
            button.setPosition(x + entryWidth - button.getWidth(), y);
            button.extractRenderState(graphics, mouseX, mouseY, delta);

            String fitted = font.plainSubstrByWidth(text, entryWidth - button.getWidth() - INLINE_GAP * 2);
            graphics.text(font, fitted, x + INLINE_GAP, y + (WIDGET_HEIGHT - font.lineHeight) / 2, COLOR_TEXT, false);
        }
    }

    /** Section label. */
    private class LabelEntry extends ConfigEntry {
        private final Component text;

        LabelEntry(Component text) {
            this.text = text;
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            graphics.text(font, text, getContentX() + INLINE_GAP,
                    getContentY() + (WIDGET_HEIGHT - font.lineHeight) / 2, COLOR_HEADER, false);
        }
    }

    /** Vanilla checkbox bound directly to membership of the mod id in the real filter set. */
    private class ModEntry extends ConfigEntry {
        private final Checkbox checkbox;

        ModEntry(ModInfo mod, Set<String> target) {
            this.checkbox = Checkbox.builder(Component.literal(mod.name()), font)
                    .selected(target.contains(mod.id()))
                    .onValueChange((_, value) -> {
                        if (value) target.add(mod.id());
                        else target.remove(mod.id());
                    })
                    .build();
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return List.of(checkbox);
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return List.of(checkbox);
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            checkbox.setPosition(getContentX() + INLINE_GAP, getContentY());
            checkbox.extractContents(graphics, mouseX, mouseY, delta);
        }
    }
}