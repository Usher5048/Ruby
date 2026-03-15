package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Allows you to climb walls like a spider by setting Y velocity upward
 * when the player has a horizontal collision (touching a wall).
 */
public class Spider extends Module {

    private final DoubleValue climbSpeed;

    public Spider() {
        super("Spider", "Allows you to climb walls like a spider.", ModuleCategory.MOVEMENT);

        climbSpeed = config.create(new DoubleValue.Builder("Climb Speed")
                .description("The speed you go up blocks.")
                .defaultValue(0.2).min(0.0).max(1.0).step(0.01)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!player.horizontalCollision) return;

        Vec3d velocity = player.getVelocity();
        if (velocity.y >= 0.2) return;

        player.setVelocity(velocity.x, climbSpeed.value(), velocity.z);
    }
}
