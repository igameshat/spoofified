package com.swaphat.spoofified;

import com.swaphat.spoofified.util.LogCensor;
import com.swaphat.spoofified.util.LunarVersionFetcher;
import com.swaphat.spoofified.util.RuntimeSecurityWatchdog;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class ClientSpooferPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // Load the config FIRST so the censor knows what to hide
        ClientSpoofer.CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("spoofified.json");
        ClientSpooferOptions.load(ClientSpoofer.CONFIG_FILE);

        // Turn on the censor before Minecraft starts
        LogCensor.register();
        RuntimeSecurityWatchdog.initializeGuard();
        LunarVersionFetcher.fetchLatestVersionAsync();
    }
}