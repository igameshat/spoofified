package com.swaphat.spoofified.mixin.client.hidingUtils;

import com.mojang.brigadier.tree.CommandNode;
import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    /**
     * Strips matching commands from the client-side Brigadier suggestion tree
     * immediately after server synchronization and Fabric command registration.
     */
    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void spoofified$onHandleCommands(ClientboundCommandsPacket packet, CallbackInfo ci) {
        if (!ClientSpooferOptions.ENABLED || ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS.isEmpty()) {
            return;
        }

        var dispatcher = ((ClientPacketListener) (Object) this).getCommands();
        var root = dispatcher.getRoot();

        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> children = (Map<String, ?>) childrenField.get(root);

            Field literalsField = CommandNode.class.getDeclaredField("literals");
            literalsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> literals = (Map<String, ?>) literalsField.get(root);

            List<String> nodesToRemove = new ArrayList<>();

            for (String nodeName : children.keySet()) {
                if (spoofified$isCommandMatching(nodeName)) {
                    nodesToRemove.add(nodeName);
                }
            }

            for (String key : nodesToRemove) {
                children.remove(key);
                literals.remove(key);
            }
        } catch (Exception e) {
            ClientSpoofer.LOGGER.error("[Spoofified] Failed to filter command tree from memory", e);
        }
    }

    /**
     * Blocks outgoing command packets if the command is hidden, stopping execution
     * and displaying an "Unknown command" error to mimic non-existence.
     */
    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void spoofified$onSendCommand(String command, CallbackInfo ci) {
        if (!ClientSpooferOptions.ENABLED || ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS.isEmpty()) {
            return;
        }

        String cleaned = command.startsWith("/") ? command.substring(1) : command;
        String rootCommand = cleaned.split(" ")[0];

        if (spoofified$isCommandMatching(rootCommand)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.translatable("spoofified.command.unknown_command", cleaned)
                );
            }

            ci.cancel();
        }
    }

    /**
     * Checks if a command matches any rule in CUSTOM_HIDDEN_COMMANDS.
     * - "help" -> matches "help", "minecraft:help", "fabric:help", etc.
     * - "fabric.help" -> matches only "fabric:help" or "fabric.help"
     */
    @Unique
    private boolean spoofified$isCommandMatching(String commandName) {
        String lowerCommand = commandName.toLowerCase();

        for (String hiddenEntry : ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS) {
            if (hiddenEntry == null || hiddenEntry.isBlank()) continue;

            String cleanEntry = hiddenEntry.startsWith("/") ? hiddenEntry.substring(1) : hiddenEntry;
            String lowerEntry = cleanEntry.toLowerCase();

            if (lowerEntry.contains(".")) {
                // Specific mod targeting (e.g. "worldedit.set" -> "worldedit:set")
                String namespaced = lowerEntry.replace('.', ':');
                if (lowerCommand.equals(lowerEntry) || lowerCommand.equals(namespaced)) {
                    return true;
                }
            } else {
                // Global targeting: matches exact name or any namespace prefix (e.g., "*:help")
                if (lowerCommand.equals(lowerEntry) || lowerCommand.endsWith(":" + lowerEntry)) {
                    return true;
                }
            }
        }
        return false;
    }
}