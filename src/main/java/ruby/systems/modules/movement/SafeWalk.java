package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Prevents walking off blocks by sneaking at edges.
 * Uses collision detection to determine if the player is near a ledge,
 * then forces sneak input to prevent falling.
 */
public class SafeWalk extends Module {

    private final BooleanValue sneak;

    public SafeWalk() {
        super("Safe Walk", "Prevents you from walking off blocks.", ModuleType.MOVEMENT);

        config.create(new IntegerValue.Builder("Fall Distance")
                .description("Minimum fall distance before the module activates.")
                .defaultValue(1).min(1).max(20)
                .build());

        sneak = config.create(new BooleanValue.Builder("Sneak At Edge")
                .description("Sneak when approaching the edge of a block.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (!player.onGround() || player.isShiftKeyDown()) return;

        AABB box = player.getBoundingBox();
        AABB belowBox = box.move(0, -player.maxUpStep() - 0.01, 0).inflate(-0.3, 0, -0.3);

        boolean atEdge = !mc.level.getBlockCollisions(player, belowBox).iterator().hasNext();

        if (atEdge && sneak.value()) {
            mc.options.keyShift.setDown(true);
        }
    }
}
