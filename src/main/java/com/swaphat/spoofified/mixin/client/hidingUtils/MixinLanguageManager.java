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

            String previousLanguage = this.currentCode;

            ClientSpooferOptions.ENABLED = true;
            ClientSpooferOptions.PANIC_MODE = false;
            ClientSpooferOptions.HIDDEN_MODS.remove("spoofified");

            if (ClientSpoofer.CONFIG_FILE != null) {
                ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);
            }

            client.options.languageCode = previousLanguage;

            LanguageManager manager = (LanguageManager) (Object) this;
            manager.setSelected(previousLanguage);

            if (ClientSpooferOptions.onConfigChanged != null) {
                ClientSpooferOptions.onConfigChanged.run();
            }

            client.options.save();
            client.reloadResourcePacks();

            ci.cancel();

            System.out.println("[Spoofified] Recovery Triggered: Restoring " + previousLanguage);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.translatable("spoofified.command.recovery"));
            }
        }
    }
}