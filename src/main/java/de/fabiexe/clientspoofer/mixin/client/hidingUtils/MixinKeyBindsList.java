package de.fabiexe.clientspoofer.mixin.client.hidingUtils;

import de.fabiexe.clientspoofer.ClientSpooferOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;

@Mixin(KeyBindsList.class)
public class MixinKeyBindsList {

    @Redirect(
            method = "<init>",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;", opcode = Opcodes.GETFIELD)
    )
    private KeyMapping[] clientspoofer$filterHiddenKeybinds(Options options) {
        KeyMapping[] original = options.keyMappings;

        if (!ClientSpooferOptions.ENABLED) {
            return original;
        }

        return Arrays.stream(original).filter(mapping -> {
            String category = mapping.getCategory().toString().toLowerCase();
            String name = mapping.getName().toLowerCase();

            for (String hiddenMod : ClientSpooferOptions.HIDDEN_MODS) {
                String sanitizedId = hiddenMod.toLowerCase().replace("-", "").replace("_", "").trim();
                if (!sanitizedId.isEmpty() && (category.contains(sanitizedId) || name.contains(sanitizedId))) {
                    return false;
                }
            }

            for (String customKey : ClientSpooferOptions.CUSTOM_HIDDEN_KEYS) {
                // The .trim() ensures accidental spaces don't break the hiding logic!
                String sanitizedKey = customKey.toLowerCase().trim();
                if (!sanitizedKey.isEmpty() && (category.contains(sanitizedKey) || name.contains(sanitizedKey))) {
                    return false;
                }
            }

            return true;
        }).toArray(KeyMapping[]::new);
    }
}