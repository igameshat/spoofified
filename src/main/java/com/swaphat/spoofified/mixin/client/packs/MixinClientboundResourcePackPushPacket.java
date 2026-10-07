package com.swaphat.spoofified.mixin.client.packs;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.NetworkUtils;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Mixin(ClientboundResourcePackPushPacket.class)
public class MixinClientboundResourcePackPushPacket {

    @Shadow @Final private String url;

    @Inject(method = "url", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$proxyUrl(CallbackInfoReturnable<String> cir) {
        if (ClientSpooferOptions.PROXY_RESOURCE_PACKS && this.url != null) {
            if (ClientSpooferOptions.BLOCK_LOCAL_PACKS && !NetworkUtils.isUrlSafe(this.url)) {
                return;
            }

            if (!this.url.startsWith(ClientSpooferOptions.PROXY_URL)) {
                try {
                    String encodedUrl = URLEncoder.encode(this.url, StandardCharsets.UTF_8);
                    cir.setReturnValue(ClientSpooferOptions.PROXY_URL + "?url=" + encodedUrl);
                } catch (Exception ignored) {
                }
            }
        }
    }
}