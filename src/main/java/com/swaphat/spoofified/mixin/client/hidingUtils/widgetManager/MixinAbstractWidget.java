package com.swaphat.spoofified.mixin.client.hidingUtils.widgetManager;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractWidget.class)
public abstract class MixinAbstractWidget {

    @Shadow public abstract int getX();
    @Shadow public abstract int getY();
    @Shadow public abstract int getWidth();
    @Shadow public abstract int getHeight();
    @Shadow public abstract void setX(int x);
    @Shadow public abstract void setY(int y);
    @Shadow public abstract void setWidth(int width);
    @Shadow public abstract void setHeight(int height);
    @Shadow public abstract Component getMessage();
    @Shadow public boolean visible;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$checkStates(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        String uniqueId = ClientSpooferOptions.getWidgetId((AbstractWidget)(Object)this);

        if (ClientSpooferOptions.LOCKED_WIDGETS.contains(uniqueId)) return;

        // 1. APPLY PERSISTENT EDITS
        if (ClientSpooferOptions.CUSTOM_BOUNDS.containsKey(uniqueId)) {
            ClientSpooferOptions.WidgetBounds bounds = ClientSpooferOptions.CUSTOM_BOUNDS.get(uniqueId);
            this.setX(bounds.x);
            this.setY(bounds.y);
            this.setWidth(bounds.width);
            this.setHeight(bounds.height);
        }

        // 2. DELETE CHECK
        if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
            this.setX(-5000);
            this.setY(-5000);
            ci.cancel();
            return;
        }

        // 3. HIDE CHECK
        if (ClientSpooferOptions.HIDDEN_WIDGETS.contains(uniqueId)) {
            ci.cancel();
            if (ClientSpooferOptions.ACTIVE_MENU_OWNER != (Object) this) {
                this.setX(-5000);
                this.setY(-5000);
            }
        }
    }

    // THE FIX: The "Hijack Trick" to ensure the menu renders on top
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void clientspoofer$drawNormalMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        // If a menu is supposed to be open...
        if (ClientSpooferOptions.ACTIVE_MENU_OWNER != null) {
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen != null) {
                List<AbstractWidget> widgets = Screens.getWidgets(screen);

                // Find the very last visible widget on the screen
                AbstractWidget lastVisible = null;
                for (int i = widgets.size() - 1; i >= 0; i--) {
                    if (widgets.get(i).visible) {
                        lastVisible = widgets.get(i);
                        break;
                    }
                }

                // If THIS specific widget is the last one in the list, hijack it!
                // It draws our menu after everything else is finished.
                if (lastVisible == (Object) this) {
                    clientspoofer$drawContextMenu(graphics, ClientSpooferOptions.ACTIVE_MENU_OWNER);
                }
            }
        }
    }

    @Unique
    private void clientspoofer$drawContextMenu(GuiGraphicsExtractor graphics, AbstractWidget owner) {
        int mx = ClientSpooferOptions.MENU_X;
        int my = ClientSpooferOptions.MENU_Y;
        String uniqueId = ClientSpooferOptions.getWidgetId(owner);
        boolean isHidden = ClientSpooferOptions.HIDDEN_WIDGETS.contains(uniqueId);

        // Draw Dark Background & White Border for Main Menu
        graphics.fill(mx, my, mx + 80, my + 65, 0xEE000000);
        graphics.outline(mx, my, 80, 65, 0xFFFFFFFF);

        graphics.text(Minecraft.getInstance().font, isHidden ? "Reveal" : "Hide", mx + 5, my + 5, 0xFFFFFFFF);
        graphics.text(Minecraft.getInstance().font, "Delete", mx + 5, my + 25, 0xFFFF5555);
        graphics.text(Minecraft.getInstance().font, "Edit >", mx + 5, my + 45, 0xFF55FF55);

        Minecraft client = Minecraft.getInstance();
        double mouseX = client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getScreenHeight();

        boolean hoveringEdit = mouseX >= mx && mouseX <= mx + 80 && mouseY >= my + 40 && mouseY <= my + 65;
        boolean hoveringSubMenu = mouseX >= mx + 80 && mouseX <= mx + 180 && mouseY >= my + 40 && mouseY <= my + 120;

        if (hoveringEdit || hoveringSubMenu) {
            int subX = mx + 80;
            int subY = my + 40;

            // Draw Dark Background & White Border for Sub Menu
            graphics.fill(subX, subY, subX + 100, subY + 80, 0xEE000000);
            graphics.outline(subX, subY, 100, 80, 0xFFFFFFFF);

            graphics.text(Minecraft.getInstance().font, "[Scroll] X: " + owner.getX(), subX + 5, subY + 5, 0xFFAAAAAA);
            graphics.text(Minecraft.getInstance().font, "[Scroll] Y: " + owner.getY(), subX + 5, subY + 25, 0xFFAAAAAA);
            graphics.text(Minecraft.getInstance().font, "[Scroll] W: " + owner.getWidth(), subX + 5, subY + 45, 0xFFAAAAAA);
            graphics.text(Minecraft.getInstance().font, "[Scroll] H: " + owner.getHeight(), subX + 5, subY + 65, 0xFFAAAAAA);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$handleMenuClicks(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        String uniqueId = ClientSpooferOptions.getWidgetId((AbstractWidget)(Object)this);

        if (ClientSpooferOptions.LOCKED_WIDGETS.contains(uniqueId)) return;

        if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
            cir.setReturnValue(false);
            return;
        }

        Minecraft client = Minecraft.getInstance();
        double mouseX = client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getScreenHeight();

        boolean isCtrlDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean isHovering = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        if (isHovering && event.button() == 1 && isCtrlDown) {
            if (ClientSpooferOptions.ACTIVE_MENU_OWNER == (Object) this) {
                ClientSpooferOptions.ACTIVE_MENU_OWNER = null;
                return;
            } else {
                ClientSpooferOptions.ACTIVE_MENU_OWNER = (AbstractWidget) (Object) this;
                ClientSpooferOptions.MENU_X = (int) mouseX;
                ClientSpooferOptions.MENU_Y = (int) mouseY;
                cir.setReturnValue(true);
            }
        }
    }


    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$layoutWidth(CallbackInfoReturnable<Integer> cir) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        String uniqueId = ClientSpooferOptions.getWidgetId((AbstractWidget)(Object)this);

        // If deleted, tell the layout engine it takes up 0 horizontal space
        if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
            cir.setReturnValue(0);
        }
        // If edited, tell the layout engine to use your custom width
        else if (ClientSpooferOptions.CUSTOM_BOUNDS.containsKey(uniqueId)) {
            cir.setReturnValue(ClientSpooferOptions.CUSTOM_BOUNDS.get(uniqueId).width);
        }
        // Hidden widgets fall through here and return normal size, preserving the gap!
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$layoutHeight(CallbackInfoReturnable<Integer> cir) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        String uniqueId = ClientSpooferOptions.getWidgetId((AbstractWidget)(Object)this);

        // If deleted, tell the layout engine it takes up 0 vertical space
        if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
            cir.setReturnValue(0);
        }
        // If edited, tell the layout engine to use your custom height
        else if (ClientSpooferOptions.CUSTOM_BOUNDS.containsKey(uniqueId)) {
            cir.setReturnValue(ClientSpooferOptions.CUSTOM_BOUNDS.get(uniqueId).height);
        }
    }
}