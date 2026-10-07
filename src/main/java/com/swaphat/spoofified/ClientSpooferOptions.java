package com.swaphat.spoofified;

import com.google.gson.*;
import com.swaphat.spoofified.util.filter.PackFilterProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClientSpooferOptions {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Core Mod State
    public static boolean ENABLED = true;
    public static boolean PANIC_MODE = false;
    public static SpoofMode SPOOF_MODE = SpoofMode.VANILLA;
    public static String CUSTOM_CLIENT = "fabric";

    public static boolean CUSTOM_HIDE_MODS = true;
    public static boolean CUSTOM_PREVENT_FINGERPRINTING = true;

    // Mod & Payload Filtering
    public enum FilterStrategy { ALLOWLIST, BLOCKLIST }

    public static FilterStrategy MOD_FILTER_STRATEGY = FilterStrategy.ALLOWLIST;
    public static Set<String> MOD_FILTER_LIST = new HashSet<>();

    public enum PayloadPolicy { ALLOW_ALL, ALLOWLIST, BLOCKLIST }
    public static PayloadPolicy PAYLOAD_POLICY = PayloadPolicy.ALLOWLIST;
    public static Set<String> PAYLOAD_CHANNELS = new HashSet<>();

    // Keybinds & Command Hiding
    public static Set<String> CUSTOM_HIDDEN_KEYS = new HashSet<>();
    public static boolean FAKE_KEYBINDS_LAYOUT = true;
    public static Set<String> CUSTOM_HIDDEN_COMMANDS = new HashSet<>();

    // Privacy & Mojang Telemetry
    public static boolean BLOCK_TELEMETRY = true;
    public static boolean BLOCK_FRIENDS = true;
    public static boolean BLOCK_REALMS = true;
    public static boolean BLOCK_SERVER_COOKIES = true; // Cross-session tracking cookie protection
    public static boolean RUN_CONTINUOUS_WATCHDOG = false;

    // Resource Pack Stripping & Proxy
    public enum PackStripMode { NONE, SHADERS_ONLY, VISUALS_ONLY, FULL }
    public static PackStripMode PACK_STRIP_MODE = PackStripMode.NONE;

    public static boolean PROXY_RESOURCE_PACKS = false;
    public static String PROXY_URL = "";
    public static boolean BLOCK_LOCAL_PACKS = true;

    // Granular ACL rule profile (for per-directory permissions)
    public static PackFilterProfile GLOBAL_PACK_PROFILE = new PackFilterProfile();

    // UI & Widget Customization
    public static Set<String> REMOVED_WIDGETS = new HashSet<>();
    public static Set<String> INVISIBLE_WIDGETS = new HashSet<>();
    public static Map<String, WidgetBounds> CUSTOM_BOUNDS = new HashMap<>();
    public static boolean AUTO_RECALCULATE_UI = true;
    public static boolean HIDE_CHAT_MENTIONS = false;

    public static final Set<String> LOCKED_WIDGETS = new HashSet<>(List.of("PauseScreen:Crash Out"));

    // Notifications
    public enum NotificationMode { NOTHING, TOAST, CHAT, BOTH }
    public static NotificationMode ALERT_MODE = NotificationMode.BOTH;

    public static Runnable onConfigChanged = () -> {};

    public static class WidgetBounds {
        public int x, y, width, height;
        public WidgetBounds(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    // Helper Evaluation Methods
    public static boolean shouldHideMods() {
        if (!ENABLED || PANIC_MODE) return false;
        return switch (SPOOF_MODE) {
            case VANILLA, LUNAR, BADLION -> true;
            case CUSTOM -> CUSTOM_HIDE_MODS;
            default -> false;
        };
    }

    public static boolean shouldPreventFingerprinting() {
        if (!ENABLED || PANIC_MODE) return false;
        return switch (SPOOF_MODE) {
            case VANILLA, LUNAR, BADLION -> true;
            case CUSTOM -> CUSTOM_PREVENT_FINGERPRINTING;
            default -> false;
        };
    }

    public static String getWidgetId(AbstractWidget widget) {
        String buttonText = widget.getMessage().getString().trim();
        Screen currentScreen = Minecraft.getInstance().gui.screen();
        if (currentScreen == null) return buttonText;
        return currentScreen.getClass().getSimpleName() + ":" + buttonText;
    }

    public static boolean isProtectedScreen() {
        Screen currentScreen = Minecraft.getInstance().gui.screen();
        if (currentScreen == null) return false;
        String name = currentScreen.getClass().getSimpleName();
        return name.equals("ClientSpooferOptionsScreen") ||
                name.equals("WidgetRestoreScreen") ||
                name.equals("ClientModSpoofingScreen");
    }

    // Serialization
    public static void load(Path path) {
        if (!Files.exists(path)) {
            save(path);
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            if (json.has("enabled")) ENABLED = json.get("enabled").getAsBoolean();
            if (json.has("panic-mode")) PANIC_MODE = json.get("panic-mode").getAsBoolean();
            if (json.has("spoof-mode")) SPOOF_MODE = SpoofMode.valueOf(json.get("spoof-mode").getAsString().toUpperCase());
            if (json.has("custom-client")) CUSTOM_CLIENT = json.get("custom-client").getAsString();
            if (json.has("custom-hide-mods")) CUSTOM_HIDE_MODS = json.get("custom-hide-mods").getAsBoolean();
            if (json.has("custom-prevent-fingerprinting")) CUSTOM_PREVENT_FINGERPRINTING = json.get("custom-prevent-fingerprinting").getAsBoolean();
            if (json.has("alert-mode")) ALERT_MODE = NotificationMode.valueOf(json.get("alert-mode").getAsString().toUpperCase());

            // Mod & Payload filtering
            if (json.has("mod-filter-strategy")) MOD_FILTER_STRATEGY = FilterStrategy.valueOf(json.get("mod-filter-strategy").getAsString().toUpperCase());
            if (json.has("mod-filter-list")) loadSet(json.getAsJsonArray("mod-filter-list"), MOD_FILTER_LIST);
            if (json.has("payload-policy")) PAYLOAD_POLICY = PayloadPolicy.valueOf(json.get("payload-policy").getAsString().toUpperCase());
            if (json.has("payload-channels")) loadSet(json.getAsJsonArray("payload-channels"), PAYLOAD_CHANNELS);

            // Telemetry & Privacy
            if (json.has("block-telemetry")) BLOCK_TELEMETRY = json.get("block-telemetry").getAsBoolean();
            if (json.has("block-friends")) BLOCK_FRIENDS = json.get("block-friends").getAsBoolean();
            if (json.has("block-realms")) BLOCK_REALMS = json.get("block-realms").getAsBoolean();
            if (json.has("block-server-cookies")) BLOCK_SERVER_COOKIES = json.get("block-server-cookies").getAsBoolean();
            if (json.has("run-continuous-watchdog")) RUN_CONTINUOUS_WATCHDOG = json.get("run-continuous-watchdog").getAsBoolean();

            // Resource Packs
            if (json.has("pack-strip-mode")) PACK_STRIP_MODE = PackStripMode.valueOf(json.get("pack-strip-mode").getAsString().toUpperCase());
            if (json.has("proxy-resource-packs")) PROXY_RESOURCE_PACKS = json.get("proxy-resource-packs").getAsBoolean();
            if (json.has("proxy-url")) PROXY_URL = json.get("proxy-url").getAsString();
            if (json.has("block-local-packs")) BLOCK_LOCAL_PACKS = json.get("block-local-packs").getAsBoolean();

            // Keys & Commands
            if (json.has("custom-hidden-keys")) loadSet(json.getAsJsonArray("custom-hidden-keys"), CUSTOM_HIDDEN_KEYS);
            if (json.has("fake-keybinds-layout")) FAKE_KEYBINDS_LAYOUT = json.get("fake-keybinds-layout").getAsBoolean();
            if (json.has("custom-hidden-commands")) loadSet(json.getAsJsonArray("custom-hidden-commands"), CUSTOM_HIDDEN_COMMANDS);

            // UI
            if (json.has("hidden-widgets")) loadSet(json.getAsJsonArray("hidden-widgets"), INVISIBLE_WIDGETS);
            if (json.has("auto-recalculate-ui")) AUTO_RECALCULATE_UI = json.get("auto-recalculate-ui").getAsBoolean();
            if (json.has("hide-chat-mentions")) HIDE_CHAT_MENTIONS = json.get("hide-chat-mentions").getAsBoolean();

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

    public static void save(Path path) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", ENABLED);
            json.addProperty("panic-mode", PANIC_MODE);
            json.addProperty("spoof-mode", SPOOF_MODE.name().toLowerCase());
            json.addProperty("custom-client", CUSTOM_CLIENT);
            json.addProperty("custom-hide-mods", CUSTOM_HIDE_MODS);
            json.addProperty("custom-prevent-fingerprinting", CUSTOM_PREVENT_FINGERPRINTING);
            json.addProperty("alert-mode", ALERT_MODE.name().toLowerCase());

            json.addProperty("mod-filter-strategy", MOD_FILTER_STRATEGY.name().toLowerCase());
            json.add("mod-filter-list", setToArray(MOD_FILTER_LIST));
            json.addProperty("payload-policy", PAYLOAD_POLICY.name().toLowerCase());
            json.add("payload-channels", setToArray(PAYLOAD_CHANNELS));

            json.addProperty("block-telemetry", BLOCK_TELEMETRY);
            json.addProperty("block-friends", BLOCK_FRIENDS);
            json.addProperty("block-realms", BLOCK_REALMS);
            json.addProperty("block-server-cookies", BLOCK_SERVER_COOKIES);
            json.addProperty("run-continuous-watchdog", RUN_CONTINUOUS_WATCHDOG);

            json.addProperty("pack-strip-mode", PACK_STRIP_MODE.name().toLowerCase());
            json.addProperty("proxy-resource-packs", PROXY_RESOURCE_PACKS);
            json.addProperty("proxy-url", PROXY_URL);
            json.addProperty("block-local-packs", BLOCK_LOCAL_PACKS);

            json.add("custom-hidden-keys", setToArray(CUSTOM_HIDDEN_KEYS));
            json.addProperty("fake-keybinds-layout", FAKE_KEYBINDS_LAYOUT);
            json.add("custom-hidden-commands", setToArray(CUSTOM_HIDDEN_COMMANDS));

            json.add("hidden-widgets", setToArray(INVISIBLE_WIDGETS));
            json.addProperty("auto-recalculate-ui", AUTO_RECALCULATE_UI);
            json.addProperty("hide-chat-mentions", HIDE_CHAT_MENTIONS);

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

            Files.writeString(path, GSON.toJson(json));
        } catch (IOException e) {
            ClientSpoofer.LOGGER.error("Failed to save ClientSpoofer config!", e);
        }
    }

    private static void loadSet(JsonArray array, Set<String> set) {
        set.clear();
        for (JsonElement e : array) set.add(e.getAsString());
    }

    private static JsonArray setToArray(Set<String> set) {
        JsonArray array = new JsonArray();
        set.forEach(array::add);
        return array;
    }
}