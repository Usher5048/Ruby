package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            level = mc.player.blockPosition().getY();
        }
        jumpWasPressed = false;
        sneakWasPressed = false;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null) return;
        if (player.onGround()) {
            jumpWasPressed = mc.options.keyJump.isDown();
            sneakWasPressed = mc.options.keyShift.isDown();
            return;
        }

        boolean jumpPressed = mc.options.keyJump.isDown();
        boolean sneakPressed = mc.options.keyShift.isDown();

        // Detect key press edge (not held)
        if (jumpPressed && !jumpWasPressed) {
            level = player.blockPosition().getY();
            double preJumpY = player.getDeltaMovement().y;
            player.jumpFromGround();
            HighJump.applyJumpMultiplier(player, preJumpY);
        }
        if (sneakPressed && !sneakWasPressed) {
            level--;
        }

        jumpWasPressed = jumpPressed;
        sneakWasPressed = sneakPressed;

        // Maintain level: auto-jump when holding jump at current Y
        if (maintainLevel.value() && player.blockPosition().getY() == level && jumpPressed) {
            double preJumpY = player.getDeltaMovement().y;
            player.jumpFromGround();
            HighJump.applyJumpMultiplier(player, preJumpY);
        }
    }

    @Override
    public void onDisable() {
        jumpWasPressed = false;
        sneakWasPressed = false;
    }
}
