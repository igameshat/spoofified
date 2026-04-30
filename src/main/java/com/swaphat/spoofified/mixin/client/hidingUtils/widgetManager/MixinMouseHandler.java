package com.swaphat.spoofified.mixin.client.hidingUtils.widgetManager;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$globalClick(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        // THE SHIELD: Prevent spoofing logic on config screens
        if (ClientSpooferOptions.isProtectedScreen()) return;

        if (action == 1) { // 1 is GLFW_PRESS
            AbstractWidget owner = ClientSpooferOptions.ACTIVE_MENU_OWNER;
            if (owner != null) {
                Minecraft client = Minecraft.getInstance();

                // Calculate GUI Scaled Coordinates
                double mouseX = client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getScreenWidth();
                double mouseY = client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getScreenHeight();

                int mx = ClientSpooferOptions.MENU_X;
                int my = ClientSpooferOptions.MENU_Y;

                boolean inMainMenu = mouseX >= mx && mouseX <= mx + 80 && mouseY >= my && mouseY <= my + 60;
                boolean inSubMenu = mouseX >= mx + 80 && mouseX <= mx + 180 && mouseY >= my + 40 && mouseY <= my + 120;

                if (inMainMenu) {
                    if (rawButtonInfo.button() == 0) { // 0 is Left Click
                        // Use Unique ID instead of raw string
                        String uniqueId = ClientSpooferOptions.getWidgetId(owner);

                        if (mouseY < my + 20) {
                            // Toggle Hide/Reveal and Save
                            if (ClientSpooferOptions.HIDDEN_WIDGETS.contains(uniqueId)) {
                                ClientSpooferOptions.HIDDEN_WIDGETS.remove(uniqueId);
                            } else {
                                ClientSpooferOptions.HIDDEN_WIDGETS.add(uniqueId);
                            }
                            if (ClientSpoofer.CONFIG_FILE != null) ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);

                            // -> RECALCULATE UI <-
                            if (ClientSpooferOptions.AUTO_RECALCULATE_UI && client.gui.screen() != null) {
                                ((ScreenAccessor) client.gui.screen()).clientspoofer$invokeRebuildWidgets();
                            }

                        } else if (mouseY < my + 40) {
                            // Delete and Save
                            ClientSpooferOptions.DELETED_WIDGETS.add(uniqueId);
                            if (ClientSpoofer.CONFIG_FILE != null) ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);

                            // -> RECALCULATE UI <-
                            if (ClientSpooferOptions.AUTO_RECALCULATE_UI && client.gui.screen() != null) {
                                client.gui.screen().resize(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                            }
                        }
                    }
                    ClientSpooferOptions.ACTIVE_MENU_OWNER = null;
                    ci.cancel();
                }
                else if (inSubMenu) {
                    // Clicks inside the sub-menu consume the event but keep it open (for scrolling)
                    ci.cancel();
                }
                else {
                    // Clicked outside: Close menu
                    ClientSpooferOptions.ACTIVE_MENU_OWNER = null;
                }
            }
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$globalScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        // THE SHIELD
        if (ClientSpooferOptions.isProtectedScreen()) return;

        AbstractWidget owner = ClientSpooferOptions.ACTIVE_MENU_OWNER;
        if (owner != null) {
            Minecraft client = Minecraft.getInstance();
            double mouseX = client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getScreenWidth();
            double mouseY = client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getScreenHeight();

            int subX = ClientSpooferOptions.MENU_X + 80;
            int subY = ClientSpooferOptions.MENU_Y + 40;

            // Check if hovering the Edit sub-menu boundaries
            if (mouseX >= subX && mouseX <= subX + 100 && mouseY >= subY && mouseY <= subY + 80) {
                int scrollAmount = yoffset > 0 ? 1 : -1;

                // Use CTRL for the 10x multiplier
                boolean isCtrlDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
                if (isCtrlDown) scrollAmount *= 10;

                // Adjust Bounds
                if (mouseY < subY + 20) owner.setX(owner.getX() + scrollAmount);
                else if (mouseY < subY + 40) owner.setY(owner.getY() + scrollAmount);
                else if (mouseY < subY + 60) owner.setWidth(owner.getWidth() + scrollAmount);
                else {
                    try {
                        // Reflection fallback for setHeight (often doesn't have a public setter)
                        for (Method m : AbstractWidget.class.getDeclaredMethods()) {
                            if (m.getName().equals("setHeight") || m.getName().equals("m_93674_")) {
                                m.setAccessible(true);
                                m.invoke(owner, owner.getHeight() + scrollAmount);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // SAVE THE NEW BOUNDS using Unique ID
                String uniqueId = ClientSpooferOptions.getWidgetId(owner);
                ClientSpooferOptions.CUSTOM_BOUNDS.put(uniqueId, new ClientSpooferOptions.WidgetBounds(
                        owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight()
                ));

                if (ClientSpoofer.CONFIG_FILE != null) ClientSpooferOptions.save(ClientSpoofer.CONFIG_FILE);

                ci.cancel(); // Stop the scroll from affecting the actual screen content
            }
        }
    }
}