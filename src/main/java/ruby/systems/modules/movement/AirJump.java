package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's AirJump: Lets you jump in the air by pressing the jump key.
 * maintainLevel: auto-jumps at current Y level while holding jump.
 */
public class AirJump extends Module {

    private final BooleanValue maintainLevel;

    private int level;
    private boolean jumpWasPressed = false;
    private boolean sneakWasPressed = false;

    public AirJump() {
        super("Air Jump", "Lets you jump in the air.", ModuleType.MOVEMENT);

        maintainLevel = config.create(new BooleanValue.Builder("Maintain Level")
                .description("Maintains your current Y level when holding the jump key.")
                .defaultValue(false)
                .build());
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            level = mc.player.getBlockPos().getY();
        }
        jumpWasPressed = false;
        sneakWasPressed = false;
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.currentScreen != null) return;
        if (player.isOnGround()) {
            jumpWasPressed = mc.options.jumpKey.isPressed();
            sneakWasPressed = mc.options.sneakKey.isPressed();
            return;
        }

        boolean jumpPressed = mc.options.jumpKey.isPressed();
        boolean sneakPressed = mc.options.sneakKey.isPressed();

        // Detect key press edge (not held)
        if (jumpPressed && !jumpWasPressed) {
            level = player.getBlockPos().getY();
            double preJumpY = player.getVelocity().y;
            player.jump();
            HighJump.applyJumpMultiplier(player, preJumpY);
        }
        if (sneakPressed && !sneakWasPressed) {
            level--;
        }

        jumpWasPressed = jumpPressed;
        sneakWasPressed = sneakPressed;

        // Maintain level: auto-jump when holding jump at current Y
        if (maintainLevel.value() && player.getBlockPos().getY() == level && jumpPressed) {
            double preJumpY = player.getVelocity().y;
            player.jump();
            HighJump.applyJumpMultiplier(player, preJumpY);
        }
    }

    @Override
    public void onDisable() {
        jumpWasPressed = false;
        sneakWasPressed = false;
    }
}
