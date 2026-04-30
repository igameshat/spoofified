package com.swaphat.spoofified.mixin.client.hidingUtils.widgetManager;

import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GridLayout.RowHelper.class)
public class MixinGridLayout {

    @Inject(method = "addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;ILnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$skipDeletedGridItems(LayoutElement widget, int columnWidth, LayoutSettings layoutSettings, CallbackInfoReturnable<LayoutElement> cir) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        if (widget instanceof AbstractWidget abstractWidget) {
            String uniqueId = ClientSpooferOptions.getWidgetId(abstractWidget);

            if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
                abstractWidget.setX(-5000);
                abstractWidget.setY(-5000);

                cir.setReturnValue(widget);
            }
        }
    }
}