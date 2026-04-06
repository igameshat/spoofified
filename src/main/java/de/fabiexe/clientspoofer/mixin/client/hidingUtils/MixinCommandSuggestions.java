package de.fabiexe.clientspoofer.mixin.client.hidingUtils;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import de.fabiexe.clientspoofer.ClientSpooferOptions;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public class MixinCommandSuggestions {

    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;

    @Inject(method = "showSuggestions", at = @At("HEAD"))
    private void clientspoofer$filterBeforeShow(boolean immediateNarration, CallbackInfo ci) {
        if (ClientSpooferOptions.ENABLED && this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {

            Suggestions original = this.pendingSuggestions.join();
            List<Suggestion> safeSuggestions = new ArrayList<>();

            for (Suggestion suggestion : original.getList()) {
                String text = suggestion.getText().toLowerCase();
                boolean shouldHide = text.equals("..") || text.startsWith(".. ");


                if (!shouldHide) {
                    for (String hiddenCmd : ClientSpooferOptions.CUSTOM_HIDDEN_COMMANDS) {
                        String cleanCmd = hiddenCmd.toLowerCase().replace("/", "").trim();
                        if (!cleanCmd.isEmpty() && (text.equals(cleanCmd) || text.startsWith(cleanCmd + " "))) {
                            shouldHide = true;
                            break;
                        }
                    }
                }

                if (!shouldHide) {
                    for (String hiddenMod : ClientSpooferOptions.HIDDEN_MODS) {
                        String cleanMod = hiddenMod.toLowerCase().replace("-", "").replace("_", "").trim();
                        if (!cleanMod.isEmpty() && text.contains(cleanMod)) {
                            shouldHide = true;
                            break;
                        }
                    }
                }

                if (!shouldHide) {
                    safeSuggestions.add(suggestion);
                }
            }

            this.pendingSuggestions = CompletableFuture.completedFuture(
                    new Suggestions(original.getRange(), safeSuggestions)
            );
        }
    }
}