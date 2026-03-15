package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Detect the frame right after a jump: was on ground, now airborne with positive Y velocity
        if (wasOnGround && !player.isOnGround() && player.getVelocity().y > 0) {
            applyJumpMultiplier(player, 0.0);
        }

        wasOnGround = player.isOnGround();
    }

    public static void applyJumpMultiplier(ClientPlayerEntity player, double preJumpY) {
        HighJump highJump = Modules.getByClass(HighJump.class);
        if (highJump == null || !highJump.enabled()) return;

        double postJumpY = player.getVelocity().y;
        double jumpDelta = postJumpY - preJumpY;
        if (jumpDelta <= 0) return;

        player.setVelocity(
                player.getVelocity().x,
                preJumpY + jumpDelta * highJump.multiplier.value(),
                player.getVelocity().z
        );
    }
}
