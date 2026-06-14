package ruby.systems.commands;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import ruby.RubyClient;

public class VClip extends Command {
    protected VClip() {
        super("VClip", "Allows vertical clipping");
    }

    public static void clip(double height, boolean hasPacketLimit) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getNetworkHandler() == null) return;

        int grounds = (int) Math.ceil(Math.abs(height) / 10);
        if(grounds > 20 && !hasPacketLimit) grounds = 1;

        if(RubyClient.client.player.hasVehicle()) {
            if(RubyClient.client.player.getVehicle() == null) return;
            for(int i = 0; i < grounds - 1; i++) {
                RubyClient.client.getNetworkHandler().sendPacket(
                        VehicleMoveC2SPacket.fromVehicle(RubyClient.client.player.getVehicle())
                );
            }

            RubyClient.client.player.getVehicle().setPosition(
                    RubyClient.client.player.getVehicle().getX(),
                    RubyClient.client.player.getVehicle().getY() + height,
                    RubyClient.client.player.getVehicle().getZ()
            );

            RubyClient.client.getNetworkHandler().sendPacket(
                    VehicleMoveC2SPacket.fromVehicle(RubyClient.client.player.getVehicle())
            );

            return;
        }

        for(int i = 0; i < grounds - 1; i++) {
            RubyClient.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(
                    true,
                    RubyClient.client.player.horizontalCollision
            ));
        }

        RubyClient.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                RubyClient.client.player.getX(),
                RubyClient.client.player.getY() + height,
                RubyClient.client.player.getZ(),
                true,
                RubyClient.client.player.horizontalCollision
        ));

        RubyClient.client.player.setPosition(
                RubyClient.client.player.getX(),
                RubyClient.client.player.getY() + height,
                RubyClient.client.player.getZ()
        );
    }
    
    @Override
    public void execute(String[] args) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getNetworkHandler() == null) return;

        if(args.length == 0) {
            RubyClient.notifyUser("Syntax: vclip <height> [removePacketLimit]");
            return;
        }

        double height;
        boolean hasPacketLimit = true;

        try {
            height = Double.parseDouble(args[0]);
            if(args.length > 1)
                hasPacketLimit = !Boolean.parseBoolean(args[1]);
        } catch(NumberFormatException | NullPointerException e) {
            e.printStackTrace();

            // parseBoolean never throws an exception
            RubyClient.notifyUser("Invalid height!");

            return;
        }

        VClip.clip(height, hasPacketLimit);
    }
}
