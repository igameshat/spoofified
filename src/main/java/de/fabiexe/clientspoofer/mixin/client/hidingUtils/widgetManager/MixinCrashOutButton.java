package de.fabiexe.clientspoofer.mixin.client.hidingUtils.widgetManager;

import de.fabiexe.clientspoofer.gui.WidgetRestoreScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class MixinCrashOutButton extends Screen {

    protected MixinCrashOutButton(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addCrashOutButton(CallbackInfo ci) {
        assert Minecraft.getInstance().screen != null;
        this.addRenderableWidget(Button.builder(Component.literal("Crash Out"), (_) -> {

            Minecraft.getInstance().setScreen(new WidgetRestoreScreen(Minecraft.getInstance().screen));
        }).bounds(Minecraft.getInstance().screen.width-9, Minecraft.getInstance().screen.height-9, 10, 10).build());
    }
}
