package de.fabiexe.clientspoofer;

import de.fabiexe.clientspoofer.util.LogCensor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class ClientSpooferPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // Load the config FIRST so the censor knows what to hide
        ClientSpoofer.CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("clientspoofer.json");
        ClientSpooferOptions.load(ClientSpoofer.CONFIG_FILE);

        // Turn on the censor before Minecraft starts
        LogCensor.register();
    }
}