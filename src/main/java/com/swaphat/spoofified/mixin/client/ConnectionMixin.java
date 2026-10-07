package com.swaphat.spoofified.mixin.client;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.SpoofMode;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    public void sendPacket(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload)) {
            if (!(payload instanceof DiscardedPayload) && !(payload instanceof BrandPayload)) {

                if (!ClientSpooferOptions.ENABLED) {
                    return;
                }

                String payloadId = payload.type().id().toString().toLowerCase();

                if (ClientSpooferOptions.SPOOF_MODE == SpoofMode.MODDED) {
                    boolean matchesList = ClientSpooferOptions.MOD_FILTER_LIST.stream()
                            .anyMatch(mod -> payloadId.startsWith(mod.toLowerCase()));

                    if (ClientSpooferOptions.MOD_FILTER_STRATEGY == ClientSpooferOptions.FilterStrategy.ALLOWLIST) {
                        if (matchesList) return;
                    } else {
                        if (!matchesList) return;
                    }
                } else if (ClientSpooferOptions.shouldPreventFingerprinting()) {
                    if (ClientSpooferOptions.PAYLOAD_POLICY == ClientSpooferOptions.PayloadPolicy.ALLOWLIST) {
                        for (String channel : ClientSpooferOptions.PAYLOAD_CHANNELS) {
                            if (payloadId.startsWith(channel.toLowerCase())) {
                                return;
                            }
                        }
                    } else if (ClientSpooferOptions.PAYLOAD_POLICY == ClientSpooferOptions.PayloadPolicy.ALLOW_ALL) {
                        return;
                    }
                }
                ci.cancel();
            }
        }
    }
}