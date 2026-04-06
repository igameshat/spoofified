package de.fabiexe.clientspoofer.mixin.client.hidingUtils.widgetManager;

import de.fabiexe.clientspoofer.ClientSpooferOptions;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractWidget.class)
public abstract class MixinAbstractWidget {

    @Shadow public abstract int getX();
    @Shadow public abstract int getY();
    @Shadow public abstract int getWidth();
    @Shadow public abstract int getHeight();
    @Shadow public abstract void setX(int x);
    @Shadow public abstract void setY(int y);
    @Shadow public abstract void setWidth(int width);
    @Shadow protected abstract void setHeight(int height); // Shadowed so we can apply height
    @Shadow public abstract Component getMessage();

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$checkStates(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // THE SHIELD: Abort all spoofing logic if on a protected screen
        if (ClientSpooferOptions.isProtectedScreen()) return;

        String widgetName = this.getMessage().getString();

        // 1. APPLY PERSISTENT EDITS (Overrides native screen layouts!)
        if (ClientSpooferOptions.CUSTOM_BOUNDS.containsKey(widgetName)) {
            ClientSpooferOptions.WidgetBounds bounds = ClientSpooferOptions.CUSTOM_BOUNDS.get(widgetName);
            this.setX(bounds.x);
            this.setY(bounds.y);
            this.setWidth(bounds.width);
            this.setHeight(bounds.height);
        }

        // 2. DELETE CHECK
        if (ClientSpooferOptions.DELETED_WIDGETS.contains(widgetName)) {
            ci.cancel();
            return;
        }

        // 3. HIDE CHECK
        if (ClientSpooferOptions.HIDDEN_WIDGETS.contains(widgetName)) {
            ci.cancel();
            if (ClientSpooferOptions.ACTIVE_MENU_OWNER == (Object) this) {
                clientspoofer$drawContextMenu(graphics);
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void clientspoofer$drawNormalMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // THE SHIELD
        if (ClientSpooferOptions.isProtectedScreen()) return;

        String widgetName = this.getMessage().getString();
        if (!ClientSpooferOptions.HIDDEN_WIDGETS.contains(widgetName) && ClientSpooferOptions.ACTIVE_MENU_OWNER == (Object) this) {
            clientspoofer$drawContextMenu(graphics);
        }
    }

    @Unique
    private void clientspoofer$drawContextMenu(GuiGraphicsExtractor graphics) {
        int mx = ClientSpooferOptions.MENU_X;
        int my = ClientSpooferOptions.MENU_Y;
        boolean isHidden = ClientSpooferOptions.HIDDEN_WIDGETS.contains(this.getMessage().getString());

        graphics.text(Minecraft.getInstance().font, isHidden ? "Reveal" : "Hide", mx + 5, my + 5, 0xFFFFFFFF);
        graphics.text(Minecraft.getInstance().font, "Delete", mx + 5, my + 25, 0xFFFF5555);
        graphics.text(Minecraft.getInstance().font, "Edit >", mx + 5, my + 45, 0xFF55FF55);

        Minecraft client = Minecraft.getInstance();
        double mouseX = client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getScreenHeight();

        boolean hoveringEdit = mouseX >= mx && mouseX <= mx + 80 && mouseY >= my + 40 && mouseY <= my + 60;
        boolean hoveringSubMenu = mouseX >= mx + 80 && mouseX <= mx + 180 && mouseY >= my + 40 && mouseY <= my + 120;

        if (hoveringEdit || hoveringSubMenu) {
            int subX = mx + 80;
            int subY = my + 40;

            graphics.fill(subX, subY, subX + 100, subY + 80, 0xDD000000);
            graphics.outline(subX, subY, 100, 80, 0xFFFFFFFF);

            graphics.text(Minecraft.getInstance().font, "[Scroll] X: " + this.getX(), subX + 5, subY + 5, 0xFFAAAAAA);
            graphics.text(Minecraft.getInstance().font, "[Scroll] Y: " + this.getY(), subX + 5, subY + 25, 0xFFAAAAAA);
            graphics.text(Minecraft.getInstance().font, "[Scroll] W: " + this.getWidth(), subX + 5, subY + 45, 0xFFAAAAAA);
            graphics.text(Minecraft.getInstance().font, "[Scroll] H: " + this.getHeight(), subX + 5, subY + 65, 0xFFAAAAAA);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$handleMenuClicks(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        // THE SHIELD
        if (ClientSpooferOptions.isProtectedScreen()) return;

        if (ClientSpooferOptions.DELETED_WIDGETS.contains(this.getMessage().getString())) {
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
}