package com.swaphat.spoofified.util;

import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

public class WarnUtils {
    // Tracks alerts as "ip:feature" to prevent spam
    private static final Set<String> warnedServersCache = new HashSet<>();

    /**
     * Sends a localized warning to the client based on their configuration preferences.
     * @param featureKey A unique string for the alert type (e.g., "IP_SCAN", "MOD_READ")
     */
    public static void notify(String featureKey, Component title, Component message) {
        ClientSpooferOptions.NotificationMode mode = ClientSpooferOptions.ALERT_MODE;
        if (mode == ClientSpooferOptions.NotificationMode.NOTHING) {
            return;
        }

        // Deduplication: Only warn once per server, per session, per feature.
        ServerData serverData = Minecraft.getInstance().getCurrentServer();
        if (serverData != null) {
            String cacheKey = serverData.ip + ":" + featureKey;
            if (warnedServersCache.contains(cacheKey)) {
                return;
            }
            warnedServersCache.add(cacheKey);
        }

        if (mode == ClientSpooferOptions.NotificationMode.TOAST || mode == ClientSpooferOptions.NotificationMode.BOTH) {
            SystemToast.SystemToastId id = new SystemToast.SystemToastId(10000L);
            Minecraft.getInstance().gui.toastManager().addToast(new SystemToast(id, title, message));
        }

        if (mode == ClientSpooferOptions.NotificationMode.CHAT || mode == ClientSpooferOptions.NotificationMode.BOTH) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.literal("§c[§bSpoofified§c]§r ").append(message)
                );
            }
        }
    }

    // Legacy/Wrapper Methods

    public static void showServerAttemptedReadingModsToast() {
        notify("MOD_READ",
                Component.translatable("spoofified.toast.title"),
                Component.translatable("clientspoofer.toast.server_attempted_reading_mods")
        );
    }

    public static void clearCache() {
        warnedServersCache.clear();
    }
}