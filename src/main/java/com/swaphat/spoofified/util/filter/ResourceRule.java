package com.swaphat.spoofified.util.filter;

import net.minecraft.resources.Identifier;

/**
 * @param namespace  e.g., "minecraft" or "*"
 * @param pathPrefix e.g., "font/", "textures/gui/", or "*"
 */
public record ResourceRule(String namespace, String pathPrefix, FilterAction action) {

    // Fast evaluation for the render thread
    public boolean matches(Identifier id) {
        boolean namespaceMatches = namespace.equals("*") || namespace.equals(id.getNamespace());
        boolean pathMatches = pathPrefix.equals("*") || id.getPath().startsWith(pathPrefix);
        return namespaceMatches && pathMatches;
    }
}