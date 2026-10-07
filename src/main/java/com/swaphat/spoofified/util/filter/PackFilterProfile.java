package com.swaphat.spoofified.util.filter;

import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public class PackFilterProfile {

    private final List<ResourceRule> rules = new ArrayList<>();
    private final FilterAction defaultAction = FilterAction.ALLOW; // What to do if no rules match

    public PackFilterProfile() {
        // Setup default "Safe" rules that a user might start with
        rules.add(new ResourceRule("*", "lang/", FilterAction.ALLOW));           // Always allow translations
        rules.add(new ResourceRule("minecraft", "font/", FilterAction.BLOCK));   // Block custom fonts (often used to obscure text or create fake UIs)
        rules.add(new ResourceRule("minecraft", "shaders/", FilterAction.BLOCK));// Block core shaders
    }

    public void addRule(ResourceRule rule) {
        this.rules.add(rule);
    }

    /**
     * Evaluates an asset Identifier against the user's rules.
     * Evaluates in order: first match wins.
     */
    public FilterAction evaluate(Identifier id) {
        for (ResourceRule rule : rules) {
            if (rule.matches(id)) {
                if (rule.action() != FilterAction.PASS) {
                    return rule.action();
                }
            }
        }
        return defaultAction;
    }
}