package com.swaphat.spoofified.mixin.client.packs;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.NetworkUtils;
import com.swaphat.spoofified.util.PackStripHandler;
import com.swaphat.spoofified.util.WarnUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonPacketListener {

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$trackServerPackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {

        if (ClientSpooferOptions.BLOCK_LOCAL_PACKS && !NetworkUtils.isUrlSafe(packet.url())) {

            ClientSpoofer.LOGGER.warn("[Spoofified] Blocked malicious local resource pack redirect: {}", packet.url());

            Minecraft.getInstance().execute(() -> {
                WarnUtils.notify("IP_SCAN",
                        Component.literal("Spoofified: Attack Blocked"),
                        Component.literal("Blocked malicious local IP port-scan attempt.")
                );

                if (Minecraft.getInstance().getConnection() != null) {
                    Minecraft.getInstance().getConnection().send(
                            new net.minecraft.network.protocol.common.ServerboundResourcePackPacket(
                                    packet.id(),
                                    net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action.ACCEPTED
                            )
                    );

                    Minecraft.getInstance().getConnection().send(
                            new net.minecraft.network.protocol.common.ServerboundResourcePackPacket(
                                    packet.id(),
                                    net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED
                            )
                    );
                }
            });
            ci.cancel();
            return;
        }
        PackStripHandler.onPackPush(packet.id(), packet.required());
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void clientspoofer$cleanupPacksOnDisconnect(DisconnectionDetails details, CallbackInfo ci) {
        PackStripHandler.clearAll();
        WarnUtils.clearCache();
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$interceptInboundPayloads(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (!ClientSpooferOptions.ENABLED) return;

        CustomPacketPayload payload = packet.payload();
        String fullChannelId = payload.type().id().toString();
        String namespace = payload.type().id().getNamespace();

        if (namespace.equals("minecraft") || namespace.equals("bungeecord") || namespace.equals("velocity")) return;

        if (ClientSpooferOptions.PAYLOAD_POLICY == ClientSpooferOptions.PayloadPolicy.ALLOWLIST) {
            if (ClientSpooferOptions.PAYLOAD_CHANNELS.contains(fullChannelId) ||
                    ClientSpooferOptions.PAYLOAD_CHANNELS.contains(namespace)) {
                return;
            }

            ClientSpoofer.LOGGER.info("[Spoofified] Dropped unlisted payload: {}", fullChannelId);

            Minecraft.getInstance().execute(() -> WarnUtils.notify("INBOUND_PROBE_" + namespace,
                    Component.literal("Spoofified: Payload Blocked"),
                    Component.literal("Blocked unlisted server probe: " + namespace)
            ));

            ci.cancel();

        } else if (ClientSpooferOptions.PAYLOAD_POLICY == ClientSpooferOptions.PayloadPolicy.BLOCKLIST) {
            if (ClientSpooferOptions.PAYLOAD_CHANNELS.contains(fullChannelId) ||
                    ClientSpooferOptions.PAYLOAD_CHANNELS.contains(namespace)) {

                ClientSpoofer.LOGGER.info("[Spoofified] Dropped blocked payload: {}", fullChannelId);

                Minecraft.getInstance().execute(() -> WarnUtils.notify("INBOUND_PROBE_" + namespace,
                        Component.literal("Spoofified: Payload Blocked"),
                        Component.literal("Blocked explicitly listed server probe: " + namespace)
                ));

                ci.cancel();
            }
        }
    }

    @Inject(method = "handleStoreCookie", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$blockCookieStorage(net.minecraft.network.protocol.common.ClientboundStoreCookiePacket packet, CallbackInfo ci) {
        if (ClientSpooferOptions.BLOCK_SERVER_COOKIES) {
            ClientSpoofer.LOGGER.info("[Spoofified] Dropped cross-session tracking cookie request.");

            Minecraft.getInstance().execute(() -> WarnUtils.notify("COOKIE_TRACKING",
                    Component.literal("Spoofified: Tracking Blocked"),
                    Component.literal("Blocked server from saving a tracking cookie.")
            ));

            ci.cancel();
        }
    }
}