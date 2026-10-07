package com.swaphat.spoofified.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LunarVersionFetcher {

    // Lunar Client's CDN endpoint for the latest launcher version
    private static final String LUNAR_UPDATE_URL = "https://launcherupdates.lunarclientcdn.com/latest.yml";

    // Fallback brand string in case the HTTP request fails or hasn't finished yet
    private static String cachedBrand = "lunarclient:latest";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Asynchronously fetches the latest Lunar Client launcher version.
     * Call this once during mod initialization (e.g., in ClientSpooferPreLaunch).
     */
    public static void fetchLatestVersionAsync() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LUNAR_UPDATE_URL))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        // Loop through the YAML lines
                        for (String line : response.body().split("\n")) {

                            // Look for the exact "version: 3.4.9" line
                            if (line.startsWith("version:")) {

                                // Split by the colon and grab the second part
                                String version = line.split(":")[1].trim();

                                // Format it for the ClientBrandRetriever
                                cachedBrand = "lunarclient:v" + version;
                                System.out.println("[Spoofified] Loaded Lunar Client version: " + cachedBrand);
                                break;
                            }
                        }
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[Spoofified] Failed to fetch Lunar Client version: " + ex.getMessage());
                    return null;
                });
    }

    /**
     * Synchronously returns the spoofed brand string.
     * Use this inside ClientBrandRetrieverMixin.
     */
    public static String getLunarClientBrand() {
        return cachedBrand;
    }
}