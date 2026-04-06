package de.fabiexe.clientspoofer;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientSpooferOptions {
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
    public static boolean IS_CAPTURING_SCREENSHOT = false;

    // Custom Dictionaries
    public static Set<String> CUSTOM_HIDDEN_KEYS = new HashSet<>();
    public static Set<String> CUSTOM_HIDDEN_COMMANDS = new HashSet<>();

    // --- PERSISTENT DATA (Make sure your JSON config saves these!) ---
    public static Set<String> DELETED_WIDGETS = new HashSet<>();
    public static Set<String> HIDDEN_WIDGETS = new HashSet<>();
    public static Map<String, WidgetBounds> CUSTOM_BOUNDS = new HashMap<>();

    // Container for our saved edits
    public static class WidgetBounds {
        public int x, y, width, height;
        public WidgetBounds(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static boolean isProtectedScreen() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen == null) return false;

        String screenName = currentScreen.getClass().getSimpleName();
        return screenName.equals("ClientSpooferOptionsScreen") ||
                screenName.equals("WidgetRestoreScreen") ||
                screenName.equals("ClientModSpoofingScreen");
    }

    // --- TRANSIENT DATA (Do not save these to JSON) ---
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

            if (json.has("enabled")) {
                ENABLED = json.get("enabled").getAsBoolean();
            } else { save(path); }

            if (json.has("spoof-mode")) {
                SPOOF_MODE = SpoofMode.valueOf(json.get("spoof-mode").getAsString().toUpperCase());
            } else { save(path); }

            if (json.has("custom-client")) {
                CUSTOM_CLIENT = json.get("custom-client").getAsString();
            } else { save(path); }

            if (json.has("hide-mods")) {
                HIDE_MODS = json.get("hide-mods").getAsBoolean();
            } else { save(path); }

            if (json.has("disable-custom-payloads")) {
                DISABLE_CUSTOM_PAYLOADS = json.get("disable-custom-payloads").getAsBoolean();
            } else { save(path); }

            if (json.has("prevent-fingerprinting")) {
                PREVENT_FINGERPRINTING = json.get("prevent-fingerprinting").getAsBoolean();
            } else { save(path); }

            if (json.has("allowed-mods")) {
                ALLOWED_MODS.clear();
                for (var element : json.getAsJsonArray("allowed-mods")) {
                    ALLOWED_MODS.add(element.getAsString());
                }
            } else { save(path); }

            if (json.has("hidden-mods")) {
                HIDDEN_MODS.clear();
                for (var element : json.getAsJsonArray("hidden-mods")) {
                    HIDDEN_MODS.add(element.getAsString());
                }
            } else { save(path); }

            if (json.has("custom-hidden-keys")) {
                CUSTOM_HIDDEN_KEYS.clear();
                for (var element : json.getAsJsonArray("custom-hidden-keys")) {
                    CUSTOM_HIDDEN_KEYS.add(element.getAsString());
                }
            } else { save(path); }

            if (json.has("custom-hidden-commands")) {
                CUSTOM_HIDDEN_COMMANDS.clear();
                for (var element : json.getAsJsonArray("custom-hidden-commands")) {
                    CUSTOM_HIDDEN_COMMANDS.add(element.getAsString());
                }
            } else { save(path); }

            if (json.has("allowed-custom-payload-channels")) {
                ALLOWED_CUSTOM_PAYLOAD_CHANNELS.clear();
                for (var element : json.getAsJsonArray("allowed-custom-payload-channels")) {
                    ALLOWED_CUSTOM_PAYLOAD_CHANNELS.add(element.getAsString());
                }
            } else { save(path); }

            // ==========================================
            // WIDGET SAVING / LOADING IMPLEMENTATION
            // ==========================================
            if (json.has("deleted-widgets")) {
                DELETED_WIDGETS.clear();
                for (var element : json.getAsJsonArray("deleted-widgets")) {
                    DELETED_WIDGETS.add(element.getAsString());
                }
            } else { save(path); }

            if (json.has("hidden-widgets")) {
                HIDDEN_WIDGETS.clear();
                for (var element : json.getAsJsonArray("hidden-widgets")) {
                    HIDDEN_WIDGETS.add(element.getAsString());
                }
            } else { save(path); }

            if (json.has("custom-bounds")) {
                CUSTOM_BOUNDS.clear();
                JsonObject boundsObject = json.getAsJsonObject("custom-bounds");
                for (Map.Entry<String, JsonElement> entry : boundsObject.entrySet()) {
                    JsonObject boundsJson = entry.getValue().getAsJsonObject();
                    int x = boundsJson.get("x").getAsInt();
                    int y = boundsJson.get("y").getAsInt();
                    int width = boundsJson.get("width").getAsInt();
                    int height = boundsJson.get("height").getAsInt();
                    CUSTOM_BOUNDS.put(entry.getKey(), new WidgetBounds(x, y, width, height));
                }
            } else { save(path); }

        } catch(IOException | JsonParseException e) {
            ClientSpoofer.LOGGER.error("Failed to load ClientSpoofer options", e);
        }
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

            JsonArray allowedModsArray = new JsonArray();
            ALLOWED_MODS.forEach(allowedModsArray::add);
            json.add("allowed-mods", allowedModsArray);

            JsonArray hiddenModsArray = new JsonArray();
            HIDDEN_MODS.forEach(hiddenModsArray::add);
            json.add("hidden-mods", hiddenModsArray);

            JsonArray customKeysArray = new JsonArray();
            CUSTOM_HIDDEN_KEYS.forEach(customKeysArray::add);
            json.add("custom-hidden-keys", customKeysArray);

            JsonArray customCommandsArray = new JsonArray();
            CUSTOM_HIDDEN_COMMANDS.forEach(customCommandsArray::add);
            json.add("custom-hidden-commands", customCommandsArray);

            JsonArray allowedCustomPayloadChannelsArray = new JsonArray();
            ALLOWED_CUSTOM_PAYLOAD_CHANNELS.forEach(allowedCustomPayloadChannelsArray::add);
            json.add("allowed-custom-payload-channels", allowedCustomPayloadChannelsArray);

            // ==========================================
            // WIDGET SAVING IMPLEMENTATION
            // ==========================================
            JsonArray deletedWidgetsArray = new JsonArray();
            DELETED_WIDGETS.forEach(deletedWidgetsArray::add);
            json.add("deleted-widgets", deletedWidgetsArray);

            JsonArray hiddenWidgetsArray = new JsonArray();
            HIDDEN_WIDGETS.forEach(hiddenWidgetsArray::add);
            json.add("hidden-widgets", hiddenWidgetsArray);

            JsonObject customBoundsObject = new JsonObject();
            for (Map.Entry<String, WidgetBounds> entry : CUSTOM_BOUNDS.entrySet()) {
                JsonObject boundsJson = new JsonObject();
                boundsJson.addProperty("x", entry.getValue().x);
                boundsJson.addProperty("y", entry.getValue().y);
                boundsJson.addProperty("width", entry.getValue().width);
                boundsJson.addProperty("height", entry.getValue().height);
                customBoundsObject.add(entry.getKey(), boundsJson);
            }
            json.add("custom-bounds", customBoundsObject);

            Files.writeString(path, json.toString());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public static boolean hideMods() {
        return switch (SPOOF_MODE) {
            case SpoofMode.VANILLA, SpoofMode.MODDED -> true;
            case SpoofMode.CUSTOM -> HIDE_MODS;
            case SpoofMode.OFF -> false;
        };
    }

    public static boolean preventFingerprinting() {
        return switch (SPOOF_MODE) {
            case SpoofMode.VANILLA, SpoofMode.MODDED -> true;
            case SpoofMode.CUSTOM -> PREVENT_FINGERPRINTING;
            case SpoofMode.OFF -> false;
        };
    }
}