package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Boosts Trident riptide speed by multiplying velocity when riptiding.
 */
public class TridentBoost extends Module {

    private final DoubleValue multiplier;

    private boolean wasRiptiding = false;

    public TridentBoost() {
        super("Trident Boost", "Boosts your trident riptide speed.", ModuleType.MOVEMENT);

        multiplier = config.create(new DoubleValue.Builder("Multiplier")
                .description("Speed multiplier for riptide.")
                .defaultValue(2.0).min(1.0).max(10.0).step(0.1)
                .build());
    }

    @Override
    public void onEnable() {
        wasRiptiding = false;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean riptiding = player.isAutoSpinAttack();

        // Boost on the first tick of riptide activation
        if (riptiding && !wasRiptiding) {
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(
                    velocity.x * multiplier.value(),
                    velocity.y * multiplier.value(),
                    velocity.z * multiplier.value()
            );
        }

        wasRiptiding = riptiding;
    }
}
