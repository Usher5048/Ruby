package ruby.helpers.render;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import ruby.RubyClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        if (RubyClient.client.world == null || RubyClient.client.getNetworkHandler() == null) return;

        for (PlayerEntity player : RubyClient.client.world.getPlayers()) {
            UUID uuid = player.getUuid();
            PlayerListEntry entry = RubyClient.client.getNetworkHandler().getPlayerListEntry(uuid);

            if (entry != null && entry.getGameMode() == null) {
                confirmedBots.put(uuid, true);
                stableTabTicks.remove(uuid);
                continue;
            }

            if (Boolean.TRUE.equals(confirmedBots.get(uuid))) continue;

            if (entry != null && entry.getGameMode() != null) {
                stableTabTicks.merge(uuid, 1, Integer::sum);
            } else {
                stableTabTicks.remove(uuid);
            }
        }
    }

    public boolean shouldHide(PlayerEntity player) {
        UUID uuid = player.getUuid();
        if (Boolean.TRUE.equals(confirmedBots.get(uuid))) return true;
        if (RubyClient.client.getNetworkHandler() == null) return true;

        PlayerListEntry entry = RubyClient.client.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry == null || entry.getGameMode() == null) return true;
        return stableTabTicks.getOrDefault(uuid, 0) < STABLE_TAB_TICKS;
    }
}
