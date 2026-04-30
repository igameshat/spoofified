package com.swaphat.spoofified.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.swaphat.spoofified.gui.ClientSpooferOptionsScreen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClientSpooferOptionsScreen::new;
    }
}
