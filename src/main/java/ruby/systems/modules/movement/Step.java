package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's Step: Modifies the player's STEP_HEIGHT attribute to allow stepping
 * up taller blocks. Tracks the active height to restore on disable.
 */
public class Step extends Module {
    private final DoubleValue height;

    private double prevStepHeight = 0.6;

    public Step() {
        super("Step", "Allows you to walk up full blocks instantly.", ModuleType.MOVEMENT);

        height = config.create(new DoubleValue.Builder("Height")
                .description("Step height in blocks.")
                .defaultValue(1.0).min(0.6).max(10.0).step(0.1)
                .build());
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AttributeInstance stepAttr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            prevStepHeight = stepAttr.getBaseValue();
            stepAttr.setBaseValue(height.value());
        }
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AttributeInstance stepAttr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(height.value());
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AttributeInstance stepAttr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(prevStepHeight);
        }
    }
}
