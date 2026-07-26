package com.swaphat.spoofified;

import com.swaphat.spoofified.gui.ClientSpooferOptionsScreen;
import net.fabricmc.api.ClientModInitializer;

import java.nio.file.Path;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSpoofer implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("spoofified");
    public static Path CONFIG_FILE;

    @Override
    public void onInitializeClient() {
        Minecraft client = Minecraft.getInstance();
        CONFIG_FILE = client.gameDirectory.toPath().resolve("config/spoofified.json");
        ClientSpooferOptions.load(CONFIG_FILE);

        SuggestionProvider<FabricClientCommandSource> suggestHideableMods = (context, builder) -> SharedSuggestionProvider.suggest(
                FabricLoader.getInstance().getAllMods().stream()
                        .map(mod -> mod.getMetadata().getId())
                        .filter(id -> !ClientSpooferOptions.HIDDEN_MODS.contains(id))
                        .toList(),
                builder
        );

        SuggestionProvider<FabricClientCommandSource> suggestRevealableMods = (context, builder) -> SharedSuggestionProvider.suggest(
                ClientSpooferOptions.HIDDEN_MODS,
                builder
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            var commandNode = ClientCommandManager.literal("..")
                    .requires(source -> !ClientSpooferOptions.PANIC_MODE) // Replaced '_'
                    .executes(ctx -> {
                        ClientSpooferOptions.PANIC_MODE = true;
                        ClientSpooferOptions.ENABLED = false;
                        ClientSpooferOptions.HIDDEN_MODS.add("spoofified");

                        if (ClientSpooferOptions.onConfigChanged != null) {
                            ClientSpooferOptions.onConfigChanged.run();
                        }

                        if (client.getConnection() != null) {
                            var connectionDispatcher = client.getConnection().getCommands();
                            var root = connectionDispatcher.getRoot();

                            try {
                                java.lang.reflect.Field childrenField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("children");
                                childrenField.setAccessible(true);
                                ((java.util.Map<?, ?>) childrenField.get(root)).remove("..");

                                java.lang.reflect.Field literalsField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("literals");
                                literalsField.setAccessible(true);
                                ((java.util.Map<?, ?>) literalsField.get(root)).remove("..");

                            } catch (Exception e) {
                                ClientSpoofer.LOGGER.error("Failed to wipe command from memory", e);
                            }
                        }

                        ctx.getSource().sendFeedback(Component.literal("[Spoofified] Self-Destructing: Mod completely hidden.").withStyle(ChatFormatting.RED));
                        return 1;
                    });

            if (ClientSpooferOptions.ENABLED && !ClientSpooferOptions.PANIC_MODE) {

                commandNode.then(ClientCommandManager.argument("e", BoolArgumentType.bool())
                        .executes(ctx -> {
                            ClientSpooferOptions.ENABLED = BoolArgumentType.getBool(ctx, "e");
                            ClientSpooferOptions.save(CONFIG_FILE);
                            ctx.getSource().sendFeedback(Component.literal("[Spoofified] Client Spoofer enabled: " + ClientSpooferOptions.ENABLED));
                            return 1;
                        })
                );

                commandNode.then(ClientCommandManager.literal("H")
                        .then(ClientCommandManager.argument("modid", StringArgumentType.word())
                                .suggests(suggestHideableMods)
                                .executes(ctx -> {
                                    String modid = StringArgumentType.getString(ctx, "modid");
                                    if (ClientSpooferOptions.HIDDEN_MODS.add(modid)) {
                                        ClientSpooferOptions.save(CONFIG_FILE);
                                        if(ClientSpooferOptions.onConfigChanged != null) ClientSpooferOptions.onConfigChanged.run();
                                        ctx.getSource().sendFeedback(Component.literal("[Spoofified] Added to hidden mods: " + modid));
                                    } else {
                                        ctx.getSource().sendError(Component.literal("[Spoofified] Mod is already hidden: " + modid));
                                    }
                                    return 1;
                                })
                        )
                );

                commandNode.then(ClientCommandManager.literal("R")
                        .then(ClientCommandManager.argument("modid", StringArgumentType.word())
                                .suggests(suggestRevealableMods)
                                .executes(ctx -> {
                                    String modid = StringArgumentType.getString(ctx, "modid");
                                    if (ClientSpooferOptions.HIDDEN_MODS.remove(modid)) {
                                        ClientSpooferOptions.save(CONFIG_FILE);
                                        if(ClientSpooferOptions.onConfigChanged != null) ClientSpooferOptions.onConfigChanged.run();
                                        ctx.getSource().sendFeedback(Component.literal("[Spoofified] Removed from hidden mods: " + modid));
                                    } else {
                                        ctx.getSource().sendError(Component.literal("[Spoofified] Mod was not hidden: " + modid));
                                    }
                                    return 1;
                                })
                        )
                );

                commandNode.then(ClientCommandManager.literal("open")
                        .executes(ctx -> {
                            client.execute(() -> client.setScreen(new ClientSpooferOptionsScreen(null)));
                            return 1;
                        })
                );
            }

            dispatcher.register(commandNode);
            FabricLoader.getInstance().getAllMods();
        });
    }
}