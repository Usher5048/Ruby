package ruby.systems.commands;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import ruby.RubyClient;

public class Test extends Command {
    protected Test() {
        super("Test", "For testing various ideas");
    }

    @Override
    public void execute(String[] args) {
        if(RubyClient.client.world == null) return;
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getNetworkHandler() == null) return;

        Entity c = null;
        double d = Double.POSITIVE_INFINITY;
        for(Entity e : RubyClient.client.world.getEntities()) {
            if(!e.isAttackable()) continue;
            if(RubyClient.client.player.equals(e)) continue;
            if(RubyClient.client.player.distanceTo(e) < d) {
                d = RubyClient.client.player.distanceTo(e);
                c = e;
            }
        }

        if(c == null) return;

        RubyClient.client.player.swingHand(Hand.MAIN_HAND);
        RubyClient.client.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(c, false));
    }
}
