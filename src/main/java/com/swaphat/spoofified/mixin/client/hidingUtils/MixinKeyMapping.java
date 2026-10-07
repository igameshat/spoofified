package com.swaphat.spoofified.mixin.client.hidingUtils;

import com.mojang.blaze3d.platform.InputConstants;
import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class MixinKeyMapping {

    @Shadow public abstract InputConstants.Key getDefaultKey();
    @Shadow public abstract KeyMapping.Category getCategory();

    @Inject(method = "getTranslatedKeyMessage", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$protectKeybindData(CallbackInfoReturnable<Component> cir) {
        if (!ClientSpooferOptions.ENABLED) return;

        KeyMapping.Category categoryRecord = this.getCategory();

        // Use the id() accessor to get the namespace and path
        String namespace = categoryRecord.id().getNamespace();
        String path = categoryRecord.id().getPath();

        // Vanilla keybinds use 'minecraft' namespace
        if (!namespace.equals("minecraft")) {
            boolean isAllowed = false;

            if (ClientSpooferOptions.shouldPreventFingerprinting()) {
                for (String mod : ClientSpooferOptions.MOD_FILTER_LIST) {
                    // Match the category namespace or path against allowed mods
                    if (namespace.equalsIgnoreCase(mod) || path.toLowerCase().startsWith(mod.toLowerCase())) {
                        isAllowed = true;
                        break;
                    }
                }
            }

            // If it's a hidden mod force the server to get fallback option instead
            if (!isAllowed) {
                cir.setReturnValue(InputConstants.UNKNOWN.getDisplayName());
                return;
            }
        }

        // If key is from vanilla return the default mapping instead of the actual layout
        if (ClientSpooferOptions.FAKE_KEYBINDS_LAYOUT) {
            cir.setReturnValue(this.getDefaultKey().getDisplayName());
        }
    }
}