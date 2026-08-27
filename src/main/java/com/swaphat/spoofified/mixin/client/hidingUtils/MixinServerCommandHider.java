package com.swaphat.spoofified.mixin.client.hidingUtils;

import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientPacketListener.class)
public class MixinServerCommandHider {

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void onHandleCommands(ClientboundCommandsPacket packet, CallbackInfo ci) {
        if (!ClientSpooferOptions.ENABLED) return;

        var dispatcher = ((ClientPacketListener) (Object) this).getCommands();
        var root = dispatcher.getRoot();

        try {
            java.lang.reflect.Field childrenField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            var children = (java.util.Map<String, ?>) childrenField.get(root);

            java.lang.reflect.Field literalsField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            var literals = (java.util.Map<String, ?>) literalsField.get(root);

            List<String> nodesToRemove = new ArrayList<>();

            for (String nodeName : children.keySet()) {
                for (String hiddenCmd : ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS) {
                    if (hiddenCmd.contains(".")) {
                        String namespaced = hiddenCmd.replace('.', ':');
                        if (nodeName.equalsIgnoreCase(hiddenCmd) || nodeName.equalsIgnoreCase(namespaced)) {
                            nodesToRemove.add(nodeName);
                        }
                    } else {
                        if (nodeName.equalsIgnoreCase(hiddenCmd) || nodeName.toLowerCase().endsWith(":" + hiddenCmd.toLowerCase())) {
                            nodesToRemove.add(nodeName);
                        }
                    }
                }
            }

            for (String key : nodesToRemove) {
                children.remove(key);
                literals.remove(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}