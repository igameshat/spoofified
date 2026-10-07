package com.swaphat.spoofified.util;

import com.swaphat.spoofified.ClientSpooferOptions;
import com.swaphat.spoofified.util.filter.PackFilterProfile;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PackStripHandler {
    private PackStripHandler() {}

    private static final Set<UUID> wrappedPacks = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> requiredPacks = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, PackFilterProfile> packProfiles = new ConcurrentHashMap<>();
    private static volatile PackFilterProfile globalProfile = null;

    public static void onPackPush(UUID id, boolean required) {
        // Updated to use the new PackStripMode enum
        if (ClientSpooferOptions.PACK_STRIP_MODE != ClientSpooferOptions.PackStripMode.NONE || hasActiveRules()) {
            wrappedPacks.add(id);
            if (required) {
                requiredPacks.add(id);
            } else {
                requiredPacks.remove(id);
            }
        }
    }

    public static boolean isRequired(UUID id) {
        return id != null && requiredPacks.contains(id);
    }

    public static void onPop(Optional<UUID> maybeId) {
        if (maybeId.isEmpty()) {
            clearAll();
        } else {
            UUID id = maybeId.get();
            wrappedPacks.remove(id);
            requiredPacks.remove(id);
            packProfiles.remove(id);
            ShaderStripTracker.clear();
        }
    }

    public static void clearAll() {
        wrappedPacks.clear();
        requiredPacks.clear();
        packProfiles.clear();
        ShaderStripTracker.clear();
    }

    public static boolean isWrapped(UUID id) {
        return id != null && wrappedPacks.contains(id);
    }

    public static PackFilterProfile getProfileForPack(UUID id) {
        if (id == null) return globalProfile;
        PackFilterProfile profile = packProfiles.get(id);
        return (profile != null) ? profile : globalProfile;
    }

    public static void setProfileForPack(UUID id, PackFilterProfile profile) {
        if (id != null && profile != null) {
            packProfiles.put(id, profile);
            wrappedPacks.add(id);
        }
    }

    public static void setGlobalProfile(PackFilterProfile profile) {
        globalProfile = profile;
    }

    public static PackFilterProfile getGlobalProfile() {
        return globalProfile;
    }

    private static boolean hasActiveRules() {
        return globalProfile != null || !packProfiles.isEmpty();
    }

    public static final String SERVER_PACK_PREFIX = "server/";
    public static final String LEGACY_SERVER_PACK_ID = "server";
    public static final UUID LEGACY_SERVER_PACK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static Optional<UUID> packIdToUuid(String packId) {
        if (packId == null) return Optional.empty();
        if (LEGACY_SERVER_PACK_ID.equals(packId)) return Optional.of(LEGACY_SERVER_PACK_UUID);
        if (!packId.startsWith(SERVER_PACK_PREFIX)) return Optional.empty();
        int lastSlash = packId.lastIndexOf('/');
        if (lastSlash < SERVER_PACK_PREFIX.length() - 1) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(packId.substring(lastSlash + 1)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}