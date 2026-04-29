package de.fabiexe.clientspoofer;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientSpooferOptions {
    // Standard GSON instance for clean, readable JSON files
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static SpoofMode SPOOF_MODE = SpoofMode.VANILLA;
    public static String CUSTOM_CLIENT = "fabric";
    public static boolean HIDE_MODS = true;
    public static boolean DISABLE_CUSTOM_PAYLOADS = true;
    public static boolean PREVENT_FINGERPRINTING = true;
    public static Set<String> ALLOWED_MODS = new HashSet<>();
    public static Set<String> HIDDEN_MODS = new HashSet<>();
    public static Set<String> ALLOWED_CUSTOM_PAYLOAD_CHANNELS = new HashSet<>();
    public static boolean ENABLED = true;

    public static boolean PANIC_MODE = false;

    // Custom Dictionaries
    public static Set<String> CUSTOM_HIDDEN_KEYS = new HashSet<>();
    public static Set<String> CUSTOM_HIDDEN_COMMANDS = new HashSet<>();

    // --- PERSISTENT DATA ---
    public static Set<String> DELETED_WIDGETS = new HashSet<>();
    public static Set<String> HIDDEN_WIDGETS = new HashSet<>();
    public static Map<String, WidgetBounds> CUSTOM_BOUNDS = new HashMap<>();

    public static boolean AUTO_RECALCULATE_UI = true;

    public static final Set<String> LOCKED_WIDGETS = new HashSet<>(List.of(
            "PauseScreen:Crash Out"
    ));

    public static class WidgetBounds {
        public int x, y, width, height;
        public WidgetBounds(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static boolean isProtectedScreen() {
        Screen currentScreen = Minecraft.getInstance().gui.screen();
        if (currentScreen == null) return false;
        String name = currentScreen.getClass().getSimpleName();
        return name.equals("ClientSpooferOptionsScreen") ||
                name.equals("WidgetRestoreScreen") ||
                name.equals("ClientModSpoofingScreen");
    }

    // --- TRANSIENT DATA ---
    public static AbstractWidget ACTIVE_MENU_OWNER = null;
    public static int MENU_X = 0;
    public static int MENU_Y = 0;

    public static Runnable onConfigChanged = () -> {};

    public static void load(Path path) {
        if (!Files.exists(path)) {
            save(path);
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            if (json.has("enabled")) ENABLED = json.get("enabled").getAsBoolean();
            if (json.has("spoof-mode")) SPOOF_MODE = SpoofMode.valueOf(json.get("spoof-mode").getAsString().toUpperCase());
            if (json.has("custom-client")) CUSTOM_CLIENT = json.get("custom-client").getAsString();
            if (json.has("hide-mods")) HIDE_MODS = json.get("hide-mods").getAsBoolean();
            if (json.has("disable-custom-payloads")) DISABLE_CUSTOM_PAYLOADS = json.get("disable-custom-payloads").getAsBoolean();
            if (json.has("prevent-fingerprinting")) PREVENT_FINGERPRINTING = json.get("prevent-fingerprinting").getAsBoolean();

            if (json.has("allowed-mods")) loadSet(json.getAsJsonArray("allowed-mods"), ALLOWED_MODS);
            if (json.has("hidden-mods")) loadSet(json.getAsJsonArray("hidden-mods"), HIDDEN_MODS);
            if (json.has("custom-hidden-keys")) loadSet(json.getAsJsonArray("custom-hidden-keys"), CUSTOM_HIDDEN_KEYS);
            if (json.has("custom-hidden-commands")) loadSet(json.getAsJsonArray("custom-hidden-commands"), CUSTOM_HIDDEN_COMMANDS);
            if (json.has("allowed-custom-payload-channels")) loadSet(json.getAsJsonArray("allowed-custom-payload-channels"), ALLOWED_CUSTOM_PAYLOAD_CHANNELS);

            // UI Logic Sets
            if (json.has("deleted-widgets")) loadSet(json.getAsJsonArray("deleted-widgets"), DELETED_WIDGETS);
            if (json.has("hidden-widgets")) loadSet(json.getAsJsonArray("hidden-widgets"), HIDDEN_WIDGETS);
            if(json.has("auto-recalculate-ui")) AUTO_RECALCULATE_UI = json.get("auto-recalculate-ui").getAsBoolean();

            if (json.has("custom-bounds")) {
                CUSTOM_BOUNDS.clear();
                JsonObject boundsMap = json.getAsJsonObject("custom-bounds");
                for (Map.Entry<String, JsonElement> entry : boundsMap.entrySet()) {
                    JsonObject b = entry.getValue().getAsJsonObject();
                    CUSTOM_BOUNDS.put(entry.getKey(), new WidgetBounds(
                            b.get("x").getAsInt(), b.get("y").getAsInt(),
                            b.get("width").getAsInt(), b.get("height").getAsInt()
                    ));
                }
            }
        } catch (Exception e) {
            ClientSpoofer.LOGGER.error("Failed to load config, saving defaults.", e);
            save(path);
        }
    }

    private static void loadSet(JsonArray array, Set<String> set) {
        set.clear();
        for (JsonElement e : array) set.add(e.getAsString());
    }

    public static void save(Path path) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", ENABLED);
            json.addProperty("spoof-mode", SPOOF_MODE.name().toLowerCase());
            json.addProperty("custom-client", CUSTOM_CLIENT);
            json.addProperty("hide-mods", HIDE_MODS);
            json.addProperty("disable-custom-payloads", DISABLE_CUSTOM_PAYLOADS);
            json.addProperty("prevent-fingerprinting", PREVENT_FINGERPRINTING);

            json.add("allowed-mods", setToArray(ALLOWED_MODS));
            json.add("hidden-mods", setToArray(HIDDEN_MODS));
            json.add("custom-hidden-keys", setToArray(CUSTOM_HIDDEN_KEYS));
            json.add("custom-hidden-commands", setToArray(CUSTOM_HIDDEN_COMMANDS));
            json.add("allowed-custom-payload-channels", setToArray(ALLOWED_CUSTOM_PAYLOAD_CHANNELS));

            // Widget Persistence
            json.add("deleted-widgets", setToArray(DELETED_WIDGETS));
            json.add("hidden-widgets", setToArray(HIDDEN_WIDGETS));
            json.addProperty("auto-recalculate-ui", AUTO_RECALCULATE_UI);

            JsonObject boundsMap = new JsonObject();
            for (var entry : CUSTOM_BOUNDS.entrySet()) {
                JsonObject b = new JsonObject();
                b.addProperty("x", entry.getValue().x);
                b.addProperty("y", entry.getValue().y);
                b.addProperty("width", entry.getValue().width);
                b.addProperty("height", entry.getValue().height);
                boundsMap.add(entry.getKey(), b);
            }
            json.add("custom-bounds", boundsMap);

            // Using GSON.toJson ensures the file is flushed correctly and pretty-printed
            Files.writeString(path, GSON.toJson(json));
        } catch (IOException e) {
            ClientSpoofer.LOGGER.error("Failed to save ClientSpoofer config!", e);
        }
    }

    private static JsonArray setToArray(Set<String> set) {
        JsonArray array = new JsonArray();
        set.forEach(array::add);
        return array;
    }

    public static boolean hideMods() {
        return switch (SPOOF_MODE) {
            case VANILLA, MODDED -> true;
            case CUSTOM -> HIDE_MODS;
            case OFF -> false;
        };
    }

    public static boolean preventFingerprinting() {
        return switch (SPOOF_MODE) {
            case VANILLA, MODDED -> true;
            case CUSTOM -> PREVENT_FINGERPRINTING;
            case OFF -> false;
        };
    }

    public static String getWidgetId(AbstractWidget widget) {
        String buttonText = widget.getMessage().getString().trim();
        Screen currentScreen = Minecraft.getInstance().gui.screen();

        // If there's no screen (rare), just use the text
        if (currentScreen == null) return buttonText;

        String screenName = currentScreen.getClass().getSimpleName();
        return screenName + ":" + buttonText;
    }
}