package com.swaphat.spoofified.mixin.client.hidingUtils;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageManager.class)
public abstract class MixinLanguageManager {


    @Shadow private String currentCode;

    @Inject(method = "setSelected", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$interceptRecoveryLanguage(String code, CallbackInfo ci) {
        if ("en_secret".equals(code)) {
            Minecraft client = Minecraft.getInstance();

            // 1. CAPTURE PREVIOUS LANGUAGE
            // This grabs the code (e.g., 'en_us' or 'he_il') before it changes
            String previousLanguage = this.currentCode;

            // 2. RESET MOD STATE
            ClientSpooferOptions.ENABLED = true;
            ClientSpooferOptions.PANIC_MODE = false;
            ClientSpooferOptions.HIDDEN_MODS.remove("spoofified");

            // 3. SAVE CONFIG
            if (ClientSpoofer.CONFIG_FILE != null) {
                ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);
            }

            // 4. RESTORE PREVIOUS LANGUAGE
            // We set the option back to what it was
            client.options.languageCode = previousLanguage;

            // Cast 'this' to manager to call the method again with the old code
            LanguageManager manager = (LanguageManager) (Object) this;
            manager.setSelected(previousLanguage);

            // 5. REFRESH & PERSIST
            if (ClientSpooferOptions.onConfigChanged != null) {
                ClientSpooferOptions.onConfigChanged.run();
            }

            client.options.save();
            client.reloadResourcePacks();

            // 6. CANCEL "EN_SECRET"
            ci.cancel();

            // 7. STEALTH FEEDBACK
            System.out.println("[Spoofified] Recovery Triggered: Restoring " + previousLanguage);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("§c[Spoofified] System Recovered."));
            }
        }
    }
}