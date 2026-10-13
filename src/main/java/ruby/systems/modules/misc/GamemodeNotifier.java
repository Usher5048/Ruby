package ruby.systems.modules.misc;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
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
            if(RubyClient.client.getConnection() == null) return;

            if(!(event.packet() instanceof ClientboundPlayerInfoUpdatePacket packet)) return;
            if(!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) return;

            for(ClientboundPlayerInfoUpdatePacket.Entry newEntry : packet.entries()) {
                PlayerInfo oldEntry = RubyClient.client.getConnection().getPlayerInfo(newEntry.profileId());
                if(oldEntry == null) continue;

                boolean isSelf = RubyClient.client.player.getUUID() == oldEntry.getProfile().id();
                if(this.excludeSelf.value() && isSelf) continue;

                if(oldEntry.getGameMode() == newEntry.gameMode()) continue;
                this.toast(oldEntry.getProfile().name())
                        .icon("user")
                        .detail("Switched to " + newEntry.gameMode().getSerializedName())
                        .show();
            }
        });
    }
}
