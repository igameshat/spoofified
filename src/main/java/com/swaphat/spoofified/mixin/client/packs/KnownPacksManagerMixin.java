package com.swaphat.spoofified.mixin.client.packs;

import com.swaphat.spoofified.ClientSpoofer;
import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.SpoofMode;
import com.swaphat.spoofified.util.WarnUtils;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(KnownPacksManager.class)
public class KnownPacksManagerMixin {
    @Redirect(
            method = "trySelectingPacks",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private <V> V clientspoofer$redirectSelectPacks(Map<KnownPack, V> instance, Object object) {
        KnownPack pack = (KnownPack) object;

        if (!pack.namespace().equalsIgnoreCase("fabric") || ClientSpooferOptions.SPOOF_MODE == SpoofMode.OFF) {
            return instance.get(pack);
        }

        if (ClientSpooferOptions.shouldPreventFingerprinting()) {
            for (String mod : ClientSpooferOptions.MOD_FILTER_LIST) {
                if (pack.id().toLowerCase().startsWith(mod.toLowerCase())) {
                    return instance.get(pack);
                }
            }
        }

        ClientSpoofer.LOGGER.info(" Hid known pack from server handshake: {}", pack.id());
        WarnUtils.notify("KNOWN_PACK_PROBE",
                Component.literal("Spoofified: Handshake Blocked"),
                Component.literal("Blocked server from profiling your hidden mods.")
        );

        return null;
    }
}