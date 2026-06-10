package ruby.systems.social;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;
import ruby.RubyClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerHeadCache {
    private static final Map<String, SkinTextures> SKINS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();

    private PlayerHeadCache() {}

    public static SkinTextures getSkin(String username) {
        if (username == null || username.isBlank()) return null;
        String key = username.toLowerCase().trim();
        SkinTextures cached = PlayerHeadCache.SKINS.get(key);
        if (cached != null) return cached;

        if (PlayerHeadCache.LOADING.putIfAbsent(key, true) == null) {
            String trimmed = username.trim();
            Optional<GameProfile> resolved = RubyClient.client.getApiServices()
                    .profileResolver()
                    .getProfileByName(trimmed);
            GameProfile profile = resolved.orElseGet(() -> new GameProfile(
                    UUID.nameUUIDFromBytes(("OfflinePlayer:" + trimmed).getBytes(StandardCharsets.UTF_8)),
                    trimmed
            ));

            RubyClient.client.getSkinProvider().fetchSkinTextures(profile).thenAccept(opt -> {
                opt.ifPresent(skin -> PlayerHeadCache.SKINS.put(key, skin));
                PlayerHeadCache.LOADING.remove(key);
            });
        }

        return PlayerHeadCache.SKINS.get(key);
    }

    public static void drawHead(DrawContext context, String username, int x, int y, int size) {
        SkinTextures skin = PlayerHeadCache.getSkin(username);
        if (skin != null) {
            PlayerSkinDrawer.draw(context, skin, x, y, size);
        }
    }
}
