package ruby.helpers.render;

import ruby.RubyClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

/** Filters tab-list NPC bots; resists brief gamemode flicker during list refreshes. */
public final class NametagBotFilter {

    private static final int STABLE_TAB_TICKS = 8;

    private final Map<UUID, Boolean> confirmedBots = new HashMap<>();
    private final Map<UUID, Integer> stableTabTicks = new HashMap<>();

    public void clear() {
        confirmedBots.clear();
        stableTabTicks.clear();
    }

    public void tick() {
        if (RubyClient.client.level == null || RubyClient.client.getConnection() == null) return;

        for (Player player : RubyClient.client.level.players()) {
            UUID uuid = player.getUUID();
            PlayerInfo entry = RubyClient.client.getConnection().getPlayerInfo(uuid);

            if (Boolean.TRUE.equals(confirmedBots.get(uuid))) continue;

            if (entry != null) {
                stableTabTicks.merge(uuid, 1, Integer::sum);
            } else {
                stableTabTicks.remove(uuid);
            }
        }
    }

    public boolean shouldHide(Player player) {
        UUID uuid = player.getUUID();
        if (Boolean.TRUE.equals(confirmedBots.get(uuid))) return true;
        if (RubyClient.client.getConnection() == null) return true;

        PlayerInfo entry = RubyClient.client.getConnection().getPlayerInfo(uuid);
        if (entry == null) return true;
        return stableTabTicks.getOrDefault(uuid, 0) < STABLE_TAB_TICKS;
    }
}
