package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's HighJump uses JumpVelocityMultiplierEvent to multiply jump height.
 * Since Ruby doesn't have that event, we detect jump timing and scale only the
 * jump impulse (delta Y) instead of multiplying total Y velocity. This avoids
 * exponential momentum when used with modules like AirJump.
 */
public class HighJump extends Module {

    private final DoubleValue multiplier;

    private boolean wasOnGround = false;

    public HighJump() {
        super("High Jump", "Makes you jump higher than normal.", ModuleType.MOVEMENT);

        multiplier = config.create(new DoubleValue.Builder("Jump Multiplier")
                .description("Jump height multiplier.")
                .defaultValue(2.0).min(1.0).max(10.0).step(0.1)
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

        // Detect the frame right after a jump: was on ground, now airborne with positive Y velocity
        if (wasOnGround && !player.onGround() && player.getDeltaMovement().y > 0) {
            applyJumpMultiplier(player, 0.0);
        }

        wasOnGround = player.onGround();
    }

    public static void applyJumpMultiplier(LocalPlayer player, double preJumpY) {
        HighJump highJump = Modules.getByClass(HighJump.class);
        if (highJump == null || !highJump.enabled()) return;

        double postJumpY = player.getDeltaMovement().y;
        double jumpDelta = postJumpY - preJumpY;
        if (jumpDelta <= 0) return;

        player.setDeltaMovement(
                player.getDeltaMovement().x,
                preJumpY + jumpDelta * highJump.multiplier.value(),
                player.getDeltaMovement().z
        );
    }
}
