package ruby.systems.social;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import ruby.RubyClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.PlayerSkin;

public final class PlayerHeadCache {
    private static final Map<String, PlayerSkin> SKINS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();

    private PlayerHeadCache() {}

    public static PlayerSkin getSkin(String username) {
        if (username == null || username.isBlank()) return null;
        String key = username.toLowerCase().trim();
        PlayerSkin cached = PlayerHeadCache.SKINS.get(key);
        if (cached != null) return cached;

        if (PlayerHeadCache.LOADING.putIfAbsent(key, true) == null) {
            String trimmed = username.trim();
            Optional<GameProfile> resolved = RubyClient.client.services()
                    .profileResolver()
                    .fetchByName(trimmed);
            GameProfile profile = resolved.orElseGet(() -> new GameProfile(
                    UUID.nameUUIDFromBytes(("OfflinePlayer:" + trimmed).getBytes(StandardCharsets.UTF_8)),
                    trimmed
            ));

            RubyClient.client.getSkinManager().get(profile).thenAccept(opt -> {
                opt.ifPresent(skin -> PlayerHeadCache.SKINS.put(key, skin));
                PlayerHeadCache.LOADING.remove(key);
            });
        }

        return PlayerHeadCache.SKINS.get(key);
    }

    public static void drawHead(GuiGraphicsExtractor context, String username, int x, int y, int size) {
        PlayerSkin skin = PlayerHeadCache.getSkin(username);
        if (skin != null) {
            PlayerFaceExtractor.extractRenderState(context, skin, x, y, size);
        }
    }
}
