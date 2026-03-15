package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Makes the player jump further by boosting horizontal velocity on jump.
 * - Vanilla: Multiplies horizontal velocity on takeoff
 * - Boost: Adds a configurable velocity boost in the look direction on jump
 */
public class LongJump extends Module {

    public enum Mode { Vanilla, Boost }

    private final EnumValue<Mode> mode;
    private final DoubleValue boostFactor;

    private boolean wasOnGround = false;

    public LongJump() {
        super("Long Jump", "Makes you jump further.", ModuleCategory.MOVEMENT);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("The method of boosting jumps.")
                .defaultValue(Mode.Vanilla)
                .build());

        boostFactor = config.create(new DoubleValue.Builder("Boost Factor")
                .description("How much to boost the jump.")
                .defaultValue(1.5).min(1.0).max(5.0).step(0.1)
                .build());
    }

    @Override
    public void onEnable() {
        wasOnGround = false;
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        boolean onGround = player.isOnGround();

        // Detect the jump frame: was on ground, now airborne with positive Y velocity
        if (wasOnGround && !onGround && player.getVelocity().y > 0) {
            Vec3d velocity = player.getVelocity();

            switch (mode.value()) {
                case Vanilla -> {
                    player.setVelocity(
                            velocity.x * boostFactor.value(),
                            velocity.y,
                            velocity.z * boostFactor.value()
                    );
                }
                case Boost -> {
                    float yaw = player.getYaw();
                    double rad = Math.toRadians(yaw);
                    double boost = (boostFactor.value() - 1.0) * 0.2;
                    player.setVelocity(
                            velocity.x + (-Math.sin(rad) * boost),
                            velocity.y,
                            velocity.z + (Math.cos(rad) * boost)
                    );
                }
            }
        }

        wasOnGround = onGround;
    }
}
