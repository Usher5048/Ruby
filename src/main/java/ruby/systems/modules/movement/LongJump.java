package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

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
        super("Long Jump", "Makes you jump further.", ModuleType.MOVEMENT);

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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean onGround = player.onGround();

        // Detect the jump frame: was on ground, now airborne with positive Y velocity
        if (wasOnGround && !onGround && player.getDeltaMovement().y > 0) {
            Vec3 velocity = player.getDeltaMovement();

            switch (mode.value()) {
                case Vanilla -> {
                    player.setDeltaMovement(
                            velocity.x * boostFactor.value(),
                            velocity.y,
                            velocity.z * boostFactor.value()
                    );
                }
                case Boost -> {
                    float yaw = player.getYRot();
                    double rad = Math.toRadians(yaw);
                    double boost = (boostFactor.value() - 1.0) * 0.2;
                    player.setDeltaMovement(
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
