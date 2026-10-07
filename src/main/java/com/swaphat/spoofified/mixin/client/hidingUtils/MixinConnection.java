package com.swaphat.spoofified.mixin.client.hidingUtils;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.WarnUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinConnection {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$interceptCustomPayloads(Packet<?> packet, CallbackInfo ci) {
        if (!ClientSpooferOptions.shouldPreventFingerprinting()) {
            return;
        }

        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            Identifier id = payload.type().id();
            String namespace = id.getNamespace();
            String fullChannel = id.toString().toLowerCase();

            if (namespace.equals("minecraft") || namespace.equals("brigadier") ||
                    namespace.equals("bungeecord") || namespace.equals("velocity") ||
                    namespace.equals("fml") || namespace.equals("forge") || namespace.equals("neo")) {
                return;
            }

            boolean isAllowed = false;

            if (ClientSpooferOptions.PAYLOAD_POLICY == ClientSpooferOptions.PayloadPolicy.ALLOW_ALL) {
                isAllowed = true;
            } else {
                boolean matchesList = ClientSpooferOptions.PAYLOAD_CHANNELS.stream()
                        .anyMatch(channel -> namespace.equalsIgnoreCase(channel) || fullChannel.startsWith(channel.toLowerCase()));

                if (ClientSpooferOptions.PAYLOAD_POLICY == ClientSpooferOptions.PayloadPolicy.ALLOWLIST) {
                    isAllowed = matchesList;
                } else if (ClientSpooferOptions.PAYLOAD_POLICY == ClientSpooferOptions.PayloadPolicy.BLOCKLIST) {
                    isAllowed = !matchesList;
                }
            }

            if (!isAllowed) {
                ClientSpoofer.LOGGER.info("[Spoofified] Dropped outgoing custom mod payload: {}", id);

                WarnUtils.notify("PAYLOAD_BLOCK_" + namespace,
                        Component.literal("Spoofified: Mod Payload Blocked"),
                        Component.literal("Hid outgoing mod packet: " + namespace)
                );

                ci.cancel();
            }
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void clientspoofer$interceptResourcePackConsent(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundResourcePackPacket packPacket) {

            if (packPacket.action() == ServerboundResourcePackPacket.Action.ACCEPTED) {

                Minecraft.getInstance().execute(() -> {

                    if (!(Minecraft.getInstance().gui.screen() instanceof com.swaphat.spoofified.gui.PackConsentScreen)) {
                        Minecraft.getInstance().setScreenAndShow(new com.swaphat.spoofified.gui.PackConsentScreen(Minecraft.getInstance().gui.screen()));
                    }
                });
            }
        }
    }
}