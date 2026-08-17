package dev.cactusskins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Глобальное состояние мода. */
public final class CactusSkins {

    public static final Map<UUID, PlayerSkinData> OVERRIDES = new ConcurrentHashMap<>();
    public static final Set<String> FETCHED = ConcurrentHashMap.newKeySet();

    private static int tickCounter;

    private CactusSkins() {}

    public static UUID profileId(GameProfile profile) {
        //? if >=1.21.0 {
        /*return profile.id();
        *///?} else {
        return profile.getId();
        //?}
    }

    public static String profileName(GameProfile profile) {
        //? if >=1.21.0 {
        /*return profile.name();
        *///?} else {
        return profile.getName();
        //?}
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (++tickCounter % 40 != 0) return; // свой счётчик: раз в ~2 секунды

        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) return;

        for (PlayerListEntry entry : handler.getPlayerList()) {
            GameProfile profile = entry.getProfile();
            if (profile == null || profileName(profile) == null) continue;
            if (FETCHED.add(profileName(profile).toLowerCase())) {
                SkinService.fetch(profile);
            }
        }
    }

    public static void resetSession() {
        OVERRIDES.clear();
        FETCHED.clear();
    }
}
