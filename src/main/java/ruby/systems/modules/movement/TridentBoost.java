package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from Meteor Client (https://github.com/MeteorDevelopment/meteor-client)
 * Licensed under GPL-3.0
 *
 * Boosts Trident riptide speed by multiplying velocity when riptiding.
 */
public class TridentBoost extends Module {

    private final DoubleValue multiplier;

    private boolean wasRiptiding = false;

    public TridentBoost() {
        super("TridentBoost", "Boosts your trident riptide speed.", ModuleCategory.MOVEMENT);

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
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        boolean riptiding = player.isUsingRiptide();

        // Boost on the first tick of riptide activation
        if (riptiding && !wasRiptiding) {
            Vec3d velocity = player.getVelocity();
            player.setVelocity(
                    velocity.x * multiplier.value(),
                    velocity.y * multiplier.value(),
                    velocity.z * multiplier.value()
            );
        }

        wasRiptiding = riptiding;
    }
}
