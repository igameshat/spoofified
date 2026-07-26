package com.swaphat.spoofified.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.ComponentUtils;
import com.swaphat.spoofified.util.ToastUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {

    @WrapOperation(
            method = {"createResult"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;")
    )
    public String wrapGetString(Component instance, Operation<String> original) {
        if (ClientSpooferOptions.hideMods() || !ClientSpooferOptions.ENABLED) {
            String str = ComponentUtils.getString(instance);
            if (!str.equals(original.call(instance))) {
                ToastUtils.showServerAttemptedReadingModsToast();
            }
            return str;
        } else {
            return original.call(instance);
        }
    }
}