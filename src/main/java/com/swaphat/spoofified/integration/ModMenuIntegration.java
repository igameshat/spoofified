package com.swaphat.spoofified.integration;

import com.swaphat.spoofified.gui.SpoofifiedConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SpoofifiedConfigScreen::new;
    }
}
