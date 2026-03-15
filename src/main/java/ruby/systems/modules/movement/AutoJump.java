package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Automatically jumps when on the ground.
 * - Jump: Normal jump
 * - LowHop: Sets Y velocity directly for a lower hop
 */
public class AutoJump extends Module {

    public enum Mode { Jump, LowHop }
    public enum JumpWhen { Sprinting, Walking, Always }

    private final EnumValue<Mode> mode;
    private final EnumValue<JumpWhen> jumpWhen;
    private final DoubleValue velocityHeight;

    public AutoJump() {
        super("AutoJump", "Automatically jumps.", ModuleCategory.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("The method of jumping.")
                .defaultValue(Mode.Jump)
                .build());

        jumpWhen = config.create(new EnumValue.Builder<JumpWhen>("Jump When")
                .description("When to automatically jump.")
                .defaultValue(JumpWhen.Always)
                .build());

        velocityHeight = config.create(new DoubleValue.Builder("Velocity Height")
                .description("The Y velocity for LowHop mode.")
                .defaultValue(0.25).min(0.0).max(2.0).step(0.05)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!player.isOnGround() || player.isSneaking()) return;
        if (!shouldJump(player)) return;

        if (mode.value() == Mode.Jump) {
            player.jump();
        } else {
            player.setVelocity(player.getVelocity().x, velocityHeight.value(), player.getVelocity().z);
        }
    }

    private boolean shouldJump(ClientPlayerEntity player) {
        return switch (jumpWhen.value()) {
            case Sprinting -> player.isSprinting() && (player.forwardSpeed != 0 || player.sidewaysSpeed != 0);
            case Walking -> player.forwardSpeed != 0 || player.sidewaysSpeed != 0;
            case Always -> true;
        };
    }
}
