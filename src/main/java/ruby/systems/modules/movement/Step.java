package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        EntityAttributeInstance stepAttr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (stepAttr != null) {
            prevStepHeight = stepAttr.getBaseValue();
            stepAttr.setBaseValue(height.value());
        }
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        EntityAttributeInstance stepAttr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(height.value());
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        EntityAttributeInstance stepAttr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(prevStepHeight);
        }
    }
}
