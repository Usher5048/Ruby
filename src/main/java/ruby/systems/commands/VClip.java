package ruby.systems.commands;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import ruby.RubyClient;

public class VClip extends Command {
    protected VClip() {
        super("VClip", "Allows vertical clipping");
    }

    public static void clip(double height, boolean hasPacketLimit) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getConnection() == null) return;

        int grounds = (int) Math.ceil(Math.abs(height) / 10);
        if(grounds > 20 && !hasPacketLimit) grounds = 1;

        if(RubyClient.client.player.isPassenger()) {
            if(RubyClient.client.player.getVehicle() == null) return;
            for(int i = 0; i < grounds - 1; i++) {
                RubyClient.client.getConnection().send(
                        ServerboundMoveVehiclePacket.fromEntity(RubyClient.client.player.getVehicle())
                );
            }

            RubyClient.client.player.getVehicle().setPos(
                    RubyClient.client.player.getVehicle().getX(),
                    RubyClient.client.player.getVehicle().getY() + height,
                    RubyClient.client.player.getVehicle().getZ()
            );

            RubyClient.client.getConnection().send(
                    ServerboundMoveVehiclePacket.fromEntity(RubyClient.client.player.getVehicle())
            );

            return;
        }

        for(int i = 0; i < grounds - 1; i++) {
            RubyClient.client.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(
                    true,
                    RubyClient.client.player.horizontalCollision
            ));
        }

        RubyClient.client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                RubyClient.client.player.getX(),
                RubyClient.client.player.getY() + height,
                RubyClient.client.player.getZ(),
                true,
                RubyClient.client.player.horizontalCollision
        ));

        RubyClient.client.player.setPos(
                RubyClient.client.player.getX(),
                RubyClient.client.player.getY() + height,
                RubyClient.client.player.getZ()
        );
    }
    
    @Override
    public void execute(String[] args) {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.getConnection() == null) return;

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
