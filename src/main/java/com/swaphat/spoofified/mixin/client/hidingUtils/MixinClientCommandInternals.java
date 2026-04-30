package com.swaphat.spoofified.mixin.client.hidingUtils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.swaphat.spoofified.ClientSpooferOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(targets = "net.fabricmc.fabric.impl.command.client.ClientCommandInternals")
public class MixinClientCommandInternals {

    // We suppress the generic warnings because we are forced to use raw types for the Mixin to compile
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(
            // We list the methods from your snippet. Require=1 ensures it doesn't crash if it only finds one.
            method = { "executeRootHelp", "executeArgumentHelp", "executeHelp" },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/CommandDispatcher;getSmartUsage(Lcom/mojang/brigadier/tree/CommandNode;Ljava/lang/Object;)Ljava/util/Map;"
            ),
            remap = false,
            require = 1
    )
    private static Map clientspoofer$filterFabricHelp(
            CommandDispatcher dispatcher,
            CommandNode node,
            Object source // FIXED: This MUST be Object because <S> erases to Object in bytecode!
    ) {

        // 1. Get the original list of commands Fabric wants to print (Using raw types)
        Map originalMap = dispatcher.getSmartUsage(node, source);

        if (!ClientSpooferOptions.ENABLED) {
            return originalMap;
        }

        // 2. Create a clean map to hold the safe commands
        Map safeMap = new LinkedHashMap();

        // 3. Loop through every command and check if it's blacklisted
        for (Object obj : originalMap.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            CommandNode cmdNode = (CommandNode) entry.getKey();

            String cmdName = cmdNode.getName().toLowerCase();
            boolean shouldHide = cmdName.equals("..");


            if (!shouldHide) {
                for (String hiddenCmd : ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS) {
                    String cleanCmd = hiddenCmd.toLowerCase().replace("/", "").trim();
                    if (!cleanCmd.isEmpty() && cmdName.equals(cleanCmd)) {
                        shouldHide = true;
                        break;
                    }
                }
            }

            // Check against the automatic Mod IDs
            if (!shouldHide) {
                for (String hiddenMod : ClientSpooferOptions.HIDDEN_MODS) {
                    String cleanMod = hiddenMod.toLowerCase().replace("-", "").replace("_", "").trim();
                    if (!cleanMod.isEmpty() && cmdName.contains(cleanMod)) {
                        shouldHide = true;
                        break;
                    }
                }
            }

            // If it passed all checks, put it in the safe map!
            if (!shouldHide) {
                safeMap.put(entry.getKey(), entry.getValue());
            }
        }

        // 4. Return the scrubbed map to Fabric
        return safeMap;
    }
}