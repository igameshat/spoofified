package com.swaphat.spoofified.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.UUID;

public class ShaderStripTracker {

    // Common vanilla core shaders that have built-in fallbacks in vanilla assets
    private static final Set<String> KNOWN_VANILLA_SHADERS = Set.of(
            "rendertype_solid", "rendertype_cutout", "rendertype_cutout_mipped",
            "rendertype_translucent", "rendertype_translucent_moving_block",
            "rendertype_armor_cutout_no_cull", "rendertype_entity_solid",
            "rendertype_entity_cutout", "rendertype_entity_cutout_no_cull",
            "rendertype_entity_smooth_cutout", "rendertype_entity_translucent",
            "rendertype_entity_translucent_cull", "rendertype_text",
            "rendertype_text_intensity", "rendertype_text_see_through",
            "rendertype_lightning", "rendertype_lines", "position",
            "position_color", "position_tex", "position_tex_color", "blit"
    );

    public static void clear() {}

    public static boolean isShaderAllowed(Identifier location, UUID packUuid) {
        String path = location.getPath();

        if (!path.startsWith("shaders/")) {
            return true;
        }

        String fileName = path.substring(path.lastIndexOf('/') + 1);
        String shaderName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

        // Determine real-time consequence
        boolean isVanillaCore = KNOWN_VANILLA_SHADERS.contains(shaderName);
        boolean isRequired = PackStripHandler.isRequired(packUuid);

        if (isVanillaCore) {
            WarnUtils.notify("SHADER_" + shaderName,
                    Component.literal("Shader Stripped (Safe)"),
                    Component.literal("Blocked '" + shaderName + "'. Vanilla fallback active; game will run normally.")
            );
        } else if (isRequired) {
            WarnUtils.notify("SHADER_" + shaderName,
                    Component.literal("Warning: Kick Hazard!"),
                    Component.literal("Blocked custom shader '" + shaderName + "'. This pack is required; pack load may fail and get you kicked!")
            );
        } else {
            WarnUtils.notify("SHADER_" + shaderName,
                    Component.literal("Warning: Pack Load Hazard"),
                    Component.literal("Blocked custom shader '" + shaderName + "'. No vanilla fallback exists; this optional pack may fail to load.")
            );
        }

        return false;
    }
}