package com.swaphat.spoofified.mixin;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.ComponentUtils;
import com.swaphat.spoofified.util.ToastUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Redirect(
            method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    public String getString(Component instance) {
        if (ClientSpooferOptions.hideMods() || !ClientSpooferOptions.ENABLED) {
            String str = ComponentUtils.getString(instance);
            if (!str.equals(instance.getString())) {
                ToastUtils.showServerAttemptedReadingModsToast();
            }
            return str;
        } else {
            return instance.getString();
        }
    }
}
