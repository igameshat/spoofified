package com.swaphat.spoofified.mixin.client.hidingUtils;

import com.google.common.collect.ListMultimap;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.util.mod.Mod;
import com.swaphat.spoofified.ClientSpooferOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

import static com.swaphat.spoofified.ClientSpooferOptions.ALLOWED_MODS;
import static com.swaphat.spoofified.ClientSpooferOptions.HIDDEN_MODS;

@Mixin(ModMenu.class)
public abstract class MixinModMain {
    @Shadow @Final public static Map<String, Mod> MODS;
    @Shadow @Final public static Map<String, Mod> ROOT_MODS;
    @Shadow @Final public static ListMultimap<Mod, Mod> PARENT_MAP;

    // Create backups to hold the original, unmodified lists
    @Unique private static final Map<String, Mod> clientspoofer$originalMods = new HashMap<>();
    @Unique private static final Map<String, Mod> clientspoofer$originalRootMods = new HashMap<>();

    @Inject(method = "onInitializeClient", at = @At("TAIL"), remap = false)
    private void initializeHeadInjection(CallbackInfo ci) {
        // 1. Back up the original maps exactly as ModMenu loaded them
        clientspoofer$originalMods.putAll(MODS);
        clientspoofer$originalRootMods.putAll(ROOT_MODS);

        // 2. Define the refresh logic and assign it to our options hook
        ClientSpooferOptions.onConfigChanged = () -> {

            // Restore everything from the backups
            MODS.clear();
            MODS.putAll(clientspoofer$originalMods);

            ROOT_MODS.clear();
            ROOT_MODS.putAll(clientspoofer$originalRootMods);

            // Re-apply the hidden mods filter safely
            MODS.entrySet().removeIf(entry -> HIDDEN_MODS.contains(entry.getKey()));
            ROOT_MODS.entrySet().removeIf(entry -> HIDDEN_MODS.contains(entry.getKey()));

            PARENT_MAP.clear();
        };

        // 3. Run it once for the initial load
        ClientSpooferOptions.onConfigChanged.run();
    }

    @Inject(method = "getDisplayedModCount", at = @At("RETURN"), cancellable = true, remap = false)
    private static void getDisplayedModCountTailInjection(CallbackInfoReturnable<String> cir) {
        // Note: MODS.size() is already reduced by your hidden mods at this point!
        // Subtracting ALLOWED_MODS.size() here might give you wonky math.
        cir.setReturnValue(Integer.toString(MODS.size() - ALLOWED_MODS.size()));
    }
}

