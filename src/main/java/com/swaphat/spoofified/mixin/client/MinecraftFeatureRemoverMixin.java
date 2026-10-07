package com.swaphat.spoofified.mixin.client;

import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftFeatureRemoverMixin {

    @Inject(method = "allowsTelemetry", at = @At("HEAD"), cancellable = true)
    private void spoofified$disableTelemetry(CallbackInfoReturnable<Boolean> info) {
        if (ClientSpooferOptions.BLOCK_TELEMETRY) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "friendsEnabled", at = @At("HEAD"), cancellable = true)
    private void spoofified$disableFriends(CallbackInfoReturnable<Boolean> info) {
        if (ClientSpooferOptions.BLOCK_FRIENDS) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "allowsRealms", at = @At("HEAD"), cancellable = true)
    private void spoofified$disableRealms(CallbackInfoReturnable<Boolean> info) {
        if (ClientSpooferOptions.BLOCK_REALMS) {
            info.setReturnValue(false);
        }
    }
}
