package de.fabiexe.clientspoofer.mixin.client.hidingUtils.widgetManager;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {

    @Invoker("rebuildWidgets")
    void clientspoofer$invokeRebuildWidgets();

}