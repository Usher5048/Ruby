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
 * Boosts elytra flight speed by adding velocity in the player's look direction.
 */
public class ElytraBoost extends Module {

    private final DoubleValue speed;

    public ElytraBoost() {
        super("ElytraBoost", "Boosts your elytra flight speed.", ModuleCategory.MOVEMENT);

        speed = config.create(new DoubleValue.Builder("Speed")
                .description("How fast to boost elytra flight.")
                .defaultValue(1.5).min(0.1).max(5.0).step(0.1)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!player.isGliding()) return;

        Vec3d look = player.getRotationVector();
        Vec3d velocity = player.getVelocity();

        double boost = speed.value() * 0.05;
        player.setVelocity(
                velocity.x + look.x * boost,
                velocity.y + look.y * boost,
                velocity.z + look.z * boost
        );
    }
}
