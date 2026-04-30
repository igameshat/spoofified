package com.swaphat.spoofified.mixin.client.hidingUtils.widgetManager;

import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LinearLayout.class)
public class MixinLinearLayout {

    @Inject(method = "addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$skipDeletedLinearItems(LayoutElement child, LayoutSettings layoutSettings, CallbackInfoReturnable<LayoutElement> cir) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        if (child instanceof AbstractWidget abstractWidget) {
            String uniqueId = ClientSpooferOptions.getWidgetId(abstractWidget);

            if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
                abstractWidget.setX(-5000);
                abstractWidget.setY(-5000);

                cir.setReturnValue(child);
            }
        }
    }
}