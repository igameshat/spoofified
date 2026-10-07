package com.swaphat.spoofified.mixin.client.packs;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.PackStripHandler;
import com.swaphat.spoofified.util.ShaderStripTracker;
import com.swaphat.spoofified.util.filter.FilterAction;
import com.swaphat.spoofified.util.filter.PackFilterProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Mixin(PathPackResources.class)
public abstract class MixinPathPackResources {

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void clientspoofer$interceptMaliciousAssets(PackType type, Identifier location, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        if (type == PackType.CLIENT_RESOURCES) {

            PackResources self = (PackResources) this;
            PackLocationInfo info = self.location();

            Optional<UUID> packUuid = PackStripHandler.packIdToUuid(info.id());

            if (packUuid.isPresent() && PackStripHandler.isWrapped(packUuid.get())) {
                UUID uuid = packUuid.get();

                if (ClientSpooferOptions.PACK_STRIP_MODE == ClientSpooferOptions.PackStripMode.FULL) {
                    cir.setReturnValue(null);
                    return;
                }

                PackFilterProfile profile = PackStripHandler.getProfileForPack(uuid);
                if (profile != null) {
                    FilterAction action = profile.evaluate(location);

                    if (action == FilterAction.BLOCK) {
                        cir.setReturnValue(null);
                        return;
                    } else if (action == FilterAction.ALLOW) {
                        return;
                    }
                }

                if (ClientSpooferOptions.PACK_STRIP_MODE == ClientSpooferOptions.PackStripMode.VISUALS_ONLY) {
                    String path = location.getPath();
                    boolean isLangFile = path.contains("/lang/") && path.endsWith(".json");

                    if (!isLangFile) {
                        cir.setReturnValue(null);
                        return;
                    }
                }

                if (ClientSpooferOptions.PACK_STRIP_MODE == ClientSpooferOptions.PackStripMode.SHADERS_ONLY) {
                    if (!ShaderStripTracker.isShaderAllowed(location, uuid)) {
                        cir.setReturnValue(null);
                    }
                }
            }
        }
    }
}