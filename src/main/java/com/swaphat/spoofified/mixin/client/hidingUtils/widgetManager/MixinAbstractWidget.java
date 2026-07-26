package com.swaphat.spoofified.mixin.client.hidingUtils.widgetManager;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$checkStates(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
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

    @Inject(method = "render", at = @At("TAIL"))
    private void clientspoofer$drawNormalMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        if (ClientSpooferOptions.ACTIVE_MENU_OWNER != null) {
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                List<AbstractWidget> widgets = Screens.getButtons(screen);

                AbstractWidget lastVisible = null;
                for (int i = widgets.size() - 1; i >= 0; i--) {
                    if (widgets.get(i).visible) {
                        lastVisible = widgets.get(i);
                        break;
                    }
                }

                if (lastVisible == (Object) this) {
                    clientspoofer$drawContextMenu(graphics, ClientSpooferOptions.ACTIVE_MENU_OWNER);
                }
            }
        }
    }

    @Unique
    private void clientspoofer$drawContextMenu(GuiGraphics graphics, AbstractWidget owner) {
        int mx = ClientSpooferOptions.MENU_X;
        int my = ClientSpooferOptions.MENU_Y;
        String uniqueId = ClientSpooferOptions.getWidgetId(owner);
        boolean isHidden = ClientSpooferOptions.HIDDEN_WIDGETS.contains(uniqueId);

        graphics.fill(mx, my, mx + 80, my + 65, 0xEE000000);
        graphics.renderOutline(mx, my, 80, 65, 0xFFFFFFFF);

        graphics.drawString(Minecraft.getInstance().font, isHidden ? "Reveal" : "Hide", mx + 5, my + 5, 0xFFFFFFFF);
        graphics.drawString(Minecraft.getInstance().font, "Delete", mx + 5, my + 25, 0xFFFF5555);
        graphics.drawString(Minecraft.getInstance().font, "Edit >", mx + 5, my + 45, 0xFF55FF55);

        Minecraft client = Minecraft.getInstance();
        double mouseX = client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getScreenHeight();

        boolean hoveringEdit = mouseX >= mx && mouseX <= mx + 80 && mouseY >= my + 40 && mouseY <= my + 65;
        boolean hoveringSubMenu = mouseX >= mx + 80 && mouseX <= mx + 180 && mouseY >= my + 40 && mouseY <= my + 120;

        if (hoveringEdit || hoveringSubMenu) {
            int subX = mx + 80;
            int subY = my + 40;

            graphics.fill(subX, subY, subX + 100, subY + 80, 0xEE000000);
            graphics.renderOutline(subX, subY, 100, 80, 0xFFFFFFFF);

            graphics.drawString(Minecraft.getInstance().font, "[Scroll] X: " + owner.getX(), subX + 5, subY + 5, 0xFFAAAAAA);
            graphics.drawString(Minecraft.getInstance().font, "[Scroll] Y: " + owner.getY(), subX + 5, subY + 25, 0xFFAAAAAA);
            graphics.drawString(Minecraft.getInstance().font, "[Scroll] W: " + owner.getWidth(), subX + 5, subY + 45, 0xFFAAAAAA);
            graphics.drawString(Minecraft.getInstance().font, "[Scroll] H: " + owner.getHeight(), subX + 5, subY + 65, 0xFFAAAAAA);
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

        boolean isCtrlDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);

        double mouseX = event.x();
        double mouseY = event.y();

        boolean isHovering = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        if (isHovering && event.buttonInfo().button() == 1 && isCtrlDown) {
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

        if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
            cir.setReturnValue(0);
        }
        else if (ClientSpooferOptions.CUSTOM_BOUNDS.containsKey(uniqueId)) {
            cir.setReturnValue(ClientSpooferOptions.CUSTOM_BOUNDS.get(uniqueId).width);
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$layoutHeight(CallbackInfoReturnable<Integer> cir) {
        if (ClientSpooferOptions.isProtectedScreen()) return;

        String uniqueId = ClientSpooferOptions.getWidgetId((AbstractWidget)(Object)this);

        if (ClientSpooferOptions.DELETED_WIDGETS.contains(uniqueId)) {
            cir.setReturnValue(0);
        }
        else if (ClientSpooferOptions.CUSTOM_BOUNDS.containsKey(uniqueId)) {
            cir.setReturnValue(ClientSpooferOptions.CUSTOM_BOUNDS.get(uniqueId).height);
        }
    }
}