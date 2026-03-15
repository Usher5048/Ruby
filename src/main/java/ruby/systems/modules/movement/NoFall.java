package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's NoFall modes:
 * - Packet: Sets onGround=true in player move packets to prevent fall damage.
 *   Meteor does this via a mixin on PlayerMoveC2SPacket. Here we send an explicit
 *   OnGroundOnly packet when falling.
 * - AirPlace: Places a block below the player before landing.
 * <p>
 * Tick-based adaptation: sends onGround=true packet when falling > 2 blocks.
 */
public class NoFall extends Module {
    public enum Mode { Packet }

    private final EnumValue<Mode> mode;

    public NoFall() {
        super("No Fall", "Prevents you from taking fall damage.", ModuleType.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("The method used to prevent fall damage.")
                .defaultValue(Mode.Packet)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.getNetworkHandler() == null) return;

        if (mode.value() == Mode.Packet) {
            // Meteor's packet mode: spoof onGround when falling far enough to take damage
            if (player.fallDistance > 2.0f) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            }
        }
    }
}
