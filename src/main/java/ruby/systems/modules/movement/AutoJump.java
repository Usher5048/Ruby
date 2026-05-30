package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
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
        super("Auto Jump", "Automatically jumps.", ModuleType.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("The method of jumping.")
                .defaultValue(Mode.Jump)
                .build());

        jumpWhen = config.create(new EnumValue.Builder<JumpWhen>("Jump When")
                .description("When to automatically jump.")
                .defaultValue(JumpWhen.Walking)
                .build());

        velocityHeight = config.create(new DoubleValue.Builder("Velocity Height")
                .description("The Y velocity for LowHop mode.")
                .defaultValue(0.25).min(0.0).max(2.0).step(0.05)
                .build());
    }

    @Override
    public void tick() {
        if (!enabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.currentScreen != null) return;
        if (player.getVehicle() != null || player.isGliding() || player.isClimbing()) return;

        if (!player.isOnGround() || player.isSneaking() || mc.options.sneakKey.isPressed()) return;
        if (mc.options.jumpKey.isPressed()) return;
        if (!shouldJump(player)) return;

        if (mode.value() == Mode.Jump) {
            player.jump();
        } else {
            player.setVelocity(player.getVelocity().x, velocityHeight.value(), player.getVelocity().z);
        }
    }

    private boolean shouldJump(ClientPlayerEntity player) {
        boolean moving = isMoving(player);

        return switch (jumpWhen.value()) {
            case Sprinting -> player.isSprinting() && moving;
            case Walking -> moving;
            case Always -> true;
        };
    }

    private boolean isMoving(ClientPlayerEntity player) {
        if (player.input == null) {
            return player.forwardSpeed != 0 || player.sidewaysSpeed != 0;
        }

        var input = player.input.playerInput;
        return input.forward() || input.backward() || input.left() || input.right();
    }
}
