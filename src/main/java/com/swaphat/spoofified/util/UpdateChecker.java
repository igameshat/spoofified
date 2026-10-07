package com.swaphat.spoofified.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateChecker.class);
    private static final String API_URL = "https://api.github.com/repos/igameshat/spoofified/releases/latest";

    private static String latestVersion = null;
    private static boolean hasNotified = false;

    /**
     * Call this in your preLaunch or onInitializeClient endpoint.
     * @param currentVersion The version string (e.g. "2.3.0-MC26.3")
     */
    public static void checkAsync(String currentVersion) {
        // Run in a lightweight virtual thread so we don't block the main Minecraft client thread
        Thread.ofVirtual().name("Spoofified-Update-Checker").start(() -> {

            // The try-with-resources block safely closes the client when the thread finishes
            try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "SpoofifiedUpdateChecker")
                        .GET()
                        .build();

                // Use the synchronous send() because we are already in a background thread
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("tag_name")) {
                        String fetchedTag = json.get("tag_name").getAsString();

                        if (fetchedTag.toLowerCase().startsWith("v")) {
                            fetchedTag = fetchedTag.substring(1);
                        }

                        if (shouldUpdate(currentVersion, fetchedTag)) {
                            latestVersion = fetchedTag;
                            LOGGER.info("[Spoofified] A new version is available: {}", latestVersion);
                        } else {
                            LOGGER.info("[Spoofified] Mod is up to date.");
                        }
                    }
                } else {
                    LOGGER.warn("[Spoofified] Update check failed with HTTP {}", response.statusCode());
                }

            } catch (Exception e) {
                LOGGER.warn("[Spoofified] Failed to check for updates: {}", e.getMessage());
            }
        });
    }

    private static boolean shouldUpdate(String current, String fetched) {
        try {
            String[] currentParts = current.split("-MC");
            String[] fetchedParts = fetched.split("-MC");

            if (currentParts.length != 2 || fetchedParts.length != 2) {
                return false;
            }

            String currentModVer = currentParts[0];
            String currentMcVer = currentParts[1];

            String fetchedModVer = fetchedParts[0];
            String fetchedMcVer = fetchedParts[1];

            if (!currentMcVer.equals(fetchedMcVer)) {
                return false;
            }

            return isVersionHigher(fetchedModVer, currentModVer);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isVersionHigher(String fetched, String current) {
        String[] fParts = fetched.split("\\.");
        String[] cParts = current.split("\\.");
        int length = Math.max(fParts.length, cParts.length);

        for (int i = 0; i < length; i++) {
            int f = (i < fParts.length) ? Integer.parseInt(fParts[i]) : 0;
            int c = (i < cParts.length) ? Integer.parseInt(cParts[i]) : 0;
            if (f > c) return true;
            if (f < c) return false;
        }
        return false;
    }

    public static void onWorldJoin() {
        if (latestVersion != null && !hasNotified) {
            hasNotified = true;
            WarnUtils.notify(
                    "UPDATE_AVAILABLE",
                    Component.literal("§bSpoofified Update"),
                    Component.literal("Version §a" + latestVersion + "§r is ready to download!")
            );
        }
    }
}