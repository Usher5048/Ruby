package ruby.systems.modules.misc;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import ruby.RubyClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class GamemodeNotifier extends Module {
    public final BooleanValue excludeSelf = this.config.create(new BooleanValue.Builder("Exclude Self")
            .description("Whether or not to skip notifying if you changed gamemodes")
            .defaultValue(true)
            .build());

    public GamemodeNotifier() {
        super("Gamemode Notifier", "Notifies you when player change their gamemode.", ModuleType.MISC);

        Events.PACKET.register(PacketEvents.RECEIVE, event -> {
            if(!this.enabled()) return;
            if(RubyClient.client.player == null) return;
            if(RubyClient.client.getNetworkHandler() == null) return;

            if(!(event.packet() instanceof PlayerListS2CPacket packet)) return;
            if(!packet.getActions().contains(PlayerListS2CPacket.Action.UPDATE_GAME_MODE)) return;

            for(PlayerListS2CPacket.Entry newEntry : packet.getEntries()) {
                PlayerListEntry oldEntry = RubyClient.client.getNetworkHandler().getPlayerListEntry(newEntry.profileId());
                if(oldEntry == null) continue;

                boolean isSelf = RubyClient.client.player.getUuid() == oldEntry.getProfile().id();
                if(this.excludeSelf.value() && isSelf) continue;

                if(oldEntry.getGameMode() == newEntry.gameMode()) continue;
                this.notifyUser(String.format("%s switched to %s", oldEntry.getProfile().name(), newEntry.gameMode().asString()));
            }
        });
    }
}
