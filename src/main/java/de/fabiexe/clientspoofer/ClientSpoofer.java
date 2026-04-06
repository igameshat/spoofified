package de.fabiexe.clientspoofer;

import net.fabricmc.api.ClientModInitializer;

import java.nio.file.Path;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class ClientSpoofer implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("clientspoofer");
    public static Path CONFIG_FILE;

    @Override
    public void onInitializeClient() {
        CONFIG_FILE = Minecraft.getInstance().gameDirectory.toPath().resolve("config/clientspoofer.json");
        ClientSpooferOptions.load(CONFIG_FILE);

        SuggestionProvider<FabricClientCommandSource> suggestHideableMods = (_, builder) -> SharedSuggestionProvider.suggest(
                FabricLoader.getInstance().getAllMods().stream()
                        .map(mod -> mod.getMetadata().getId())
                        .filter(id -> !ClientSpooferOptions.HIDDEN_MODS.contains(id))
                        .toList(),
                builder
        );

        SuggestionProvider<FabricClientCommandSource> suggestRevealableMods = (_, builder) -> SharedSuggestionProvider.suggest(
                ClientSpooferOptions.HIDDEN_MODS,
                builder
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> {

            var commandNode = literal("..")
                    .requires(_ -> !ClientSpooferOptions.PANIC_MODE)
                    .executes(ctx -> {
                        ClientSpooferOptions.PANIC_MODE = true;
                        ClientSpooferOptions.ENABLED = false;
                        ClientSpooferOptions.HIDDEN_MODS.add("clientspoofer");

                        if (ClientSpooferOptions.onConfigChanged != null) {
                            ClientSpooferOptions.onConfigChanged.run();
                        }

                        // ==========================================
                        // THE SILENT MEMORY WIPE (No Server Spam)
                        // ==========================================
                        if (Minecraft.getInstance().getConnection() != null) {
                            var connectionDispatcher = Minecraft.getInstance().getConnection().getCommands();
                            var root = connectionDispatcher.getRoot();

                            try {
                                // 1. Force access to the locked 'children' map and delete the command
                                java.lang.reflect.Field childrenField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("children");
                                childrenField.setAccessible(true);
                                ((java.util.Map<?, ?>) childrenField.get(root)).remove("..");

                                // 2. Force access to the locked 'literals' map and delete the command
                                java.lang.reflect.Field literalsField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("literals");
                                literalsField.setAccessible(true);
                                ((java.util.Map<?, ?>) literalsField.get(root)).remove("..");

                            } catch (Exception e) {
                                ClientSpoofer.LOGGER.error("Failed to wipe command from memory", e);
                            }
                        }

                        ctx.getSource().sendFeedback(Component.literal("§c[ClientSpoofer] Self-Destructing: Mod completely hidden."));
                        return 1;
                    });

            if (ClientSpooferOptions.ENABLED && !ClientSpooferOptions.PANIC_MODE) {

                commandNode.then(argument("e", BoolArgumentType.bool())
                        .executes(ctx -> {
                            ClientSpooferOptions.ENABLED = BoolArgumentType.getBool(ctx, "e");
                            ClientSpooferOptions.save(CONFIG_FILE);
                            ctx.getSource().sendFeedback(Component.literal("[ClientSpoofer] Client Spoofer enabled: " + ClientSpooferOptions.ENABLED));
                            return 1;
                        })
                );

                commandNode.then(literal("H")
                        .then(argument("modid", StringArgumentType.word())
                                .suggests(suggestHideableMods)
                                .executes(ctx -> {
                                    String modid = StringArgumentType.getString(ctx, "modid");
                                    if (ClientSpooferOptions.HIDDEN_MODS.add(modid)) {
                                        ClientSpooferOptions.save(CONFIG_FILE);
                                        if(ClientSpooferOptions.onConfigChanged != null) ClientSpooferOptions.onConfigChanged.run();
                                        ctx.getSource().sendFeedback(Component.literal("[ClientSpoofer] Added to hidden mods: " + modid));
                                    } else {
                                        ctx.getSource().sendError(Component.literal("[ClientSpoofer] Mod is already hidden: " + modid));
                                    }
                                    return 1;
                                })
                        )
                );

                commandNode.then(literal("R")
                        .then(argument("modid", StringArgumentType.word())
                                .suggests(suggestRevealableMods)
                                .executes(ctx -> {
                                    String modid = StringArgumentType.getString(ctx, "modid");
                                    if (ClientSpooferOptions.HIDDEN_MODS.remove(modid)) {
                                        ClientSpooferOptions.save(CONFIG_FILE);
                                        if(ClientSpooferOptions.onConfigChanged != null) ClientSpooferOptions.onConfigChanged.run();
                                        ctx.getSource().sendFeedback(Component.literal("[ClientSpoofer] Removed from hidden mods: " + modid));
                                    } else {
                                        ctx.getSource().sendError(Component.literal("[ClientSpoofer] Mod was not hidden: " + modid));
                                    }
                                    return 1;
                                })
                        )
                );
            }

            dispatcher.register(commandNode);
            FabricLoader.getInstance().getAllMods();
        });
    }
}