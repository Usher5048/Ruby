package ruby.systems.modules.misc;

import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import ruby.RubyClient;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.StringValue;
import ruby.systems.events.Events;
import ruby.systems.events.chat.ChatEvents;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.*;
import java.util.stream.Collectors;

public class AntiVanish extends Module {
    public final EnumValue<Mode> mode = this.config.create(new EnumValue.Builder<Mode>("mode")
            .defaultValue(Mode.LeaveMessage)
            .build());

    public final IntegerValue checkInterval = this.config.create(new IntegerValue.Builder("check-interval")
            .description("How often to check for vanished players in ticks")
            .defaultValue(60)
            .range(0, 1200)
            .build());

    public final StringValue command = this.config.create(new StringValue.Builder("command")
            .description("The command to use to check if players are still in the server")
            .defaultValue("minecraft:msg")
            .visible(() -> this.mode.value() == Mode.CommandCompletion)
            .build());

    private final ArrayList<Integer> completionIDs = new ArrayList<>();
    private List<String> usernameCache = new ArrayList<>();

    private final ArrayList<String> messageList = new ArrayList<>();
    private final Map<UUID, String> vanishMap = new HashMap<>();
    private Map<UUID, String> playerMap = new HashMap<>();

    private int timer = 0;

    private enum Mode {
        LeaveMessage,
        CommandCompletion
    }

    public AntiVanish() {
        super("anti-vanish", "Notifies you when players enter or exit vanish.", ModuleType.MISC);

        Events.CHAT.register(ChatEvents.RECEIVE, event -> this.messageList.add(event.message().getString()));
        Events.PACKET.register(PacketEvents.RECEIVE, event -> {
            if(RubyClient.client.player == null) return;
            if(!this.enabled()) return;

            if(!(event.packet() instanceof CommandSuggestionsS2CPacket packet)) return;
            if(this.mode.value() != Mode.CommandCompletion) return;

            if(!this.completionIDs.contains(packet.id())) return;

            List<String> prevUsernames = this.usernameCache.stream().toList();
            this.usernameCache = packet.getSuggestions().getList().stream()
                    .map(Suggestion::getText)
                    .toList();

            if(prevUsernames.isEmpty()) return;

            for(String username : prevUsernames) {
                if(RubyClient.client.player.getName().getString().equals(username)) continue;

                if(username.contains(" ")) continue;
                if(username.length() < 3 || username.length() > 16) continue;

                if(!this.usernameCache.contains(username)) continue;
                this.notifyUser(String.format("%s left the game", username));
            }

            for(String username : this.usernameCache) {
                if(RubyClient.client.player.getName().getString().equals(username)) continue;

                if(username.contains(" ")) continue;
                if(username.length() < 3 || username.length() > 16) continue;

                if(!prevUsernames.contains(username)) continue;
                this.notifyUser(String.format("%s joined the game", username));
            }

            this.completionIDs.remove(packet.id());
            event.setCancelled(true);
        });
    }

    private void checkLeaveMessage() {
        if(RubyClient.client.getNetworkHandler() == null) return;
        if(RubyClient.client.player == null) return;

        Map<UUID, String> prevPlayerMap = Map.copyOf(this.playerMap);
        this.playerMap = RubyClient.client.getNetworkHandler()
                .getPlayerList()
                .stream()
                .collect(Collectors.toMap(
                        playerEntry -> playerEntry.getProfile().id(),
                        playerEntry -> playerEntry.getProfile().name()
                ));

        for(UUID uuid : prevPlayerMap.keySet()) {
            if(RubyClient.client.player.getUuid().equals(uuid)) continue;
            if(this.playerMap.containsKey(uuid)) continue;

            String username = prevPlayerMap.get(uuid);

            if(username.contains(" ")) continue;
            if(username.length() < 3 || username.length() > 16) continue;

            if(this.messageList.stream().anyMatch(str -> str.contains(username)))
                continue;

            this.vanishMap.put(uuid, username);
            this.notifyUser(String.format("%s entered vanish", username));
        }

        // Prevent ConcurrentModificationException
        for(UUID uuid : this.vanishMap.keySet().toArray(new UUID[0])) {
            if(RubyClient.client.player.getUuid().equals(uuid)) continue;
            if(prevPlayerMap.containsKey(uuid)) continue;
            if(!this.playerMap.containsKey(uuid)) continue;

            String username = this.vanishMap.get(uuid);

            if(username.contains(" ")) continue;
            if(username.length() < 3 || username.length() > 16) continue;

            if(this.messageList.stream().anyMatch(str -> str.contains(username)))
                continue;

            this.vanishMap.remove(uuid);
            this.notifyUser(String.format("%s exited vanish", username));
        }

        this.messageList.clear();
    }

    @Override
    public void tick() {
        if(RubyClient.client.getNetworkHandler() == null) return;
        if(RubyClient.client.world == null) {
            this.vanishMap.clear();
            return;
        }

        this.timer++;
        if(this.timer < this.checkInterval.value())
            return;

        switch(this.mode.value()) {
            case LeaveMessage -> checkLeaveMessage();
            case CommandCompletion -> {
                int id = RubyClient.client.world.random.nextInt();
                this.completionIDs.add(id);

                RubyClient.client.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(
                        id,
                        String.format("%s ", this.command.value())
                ));
            }
        }

        this.timer = 0;
    }

    @Override
    public void onEnable() {
        this.playerMap.clear();
        this.vanishMap.clear();
    }
}
