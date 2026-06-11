package ruby.systems.commands;

import ruby.RubyClient;

public class Test extends Command {
    protected Test() {
        super("Test", "For testing various ideas");
    }

    @Override
    public void execute(String[] args) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getNetworkHandler() == null) return;

        RubyClient.client.player.setYaw(0);
//        RubyClient.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
//                RubyClient.client.player.getYaw(),
//                100,
//                RubyClient.client.player.isOnGround(),
//                RubyClient.client.player.horizontalCollision
//        ));
    }
}
