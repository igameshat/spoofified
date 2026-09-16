package com.swaphat.spoofified.mixin.client;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.ComponentUtils;
import com.swaphat.spoofified.util.ToastUtils;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @Shadow @Final private String[] messages;
    @Shadow @Final private SignText.Mutable text;

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V",
            at = @At("TAIL"))
    private void init(SignBlockEntity sign, SignTextSlot slot, boolean shouldFilter, Component title, CallbackInfo ci) {
        SignText currentText = sign.getText(slot);
        List<Component> currentLines = currentText.getMessages(shouldFilter);

        for (int i = 0; i < 4; i++) {
            if (ClientSpooferOptions.hideMods() || !ClientSpooferOptions.ENABLED) {
                Component message = i < currentLines.size() ? currentLines.get(i) : Component.empty();
                String str = ComponentUtils.getString(message);

                if (!str.equals(message.getString())) {
                    ToastUtils.showServerAttemptedReadingModsToast();
                }

                this.messages[i] = str;
                this.text.setLine(i, Component.literal(str));
            }
        }
    }
}