package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Allows the player to walk on water and lava (simplified tick-based version).
 * When the player is in a liquid, sets Y velocity to push them up to the surface.
 * Once at the surface, maintains position. Sneak to go underwater.
 */
public class Jesus extends Module {

    private final BooleanValue water;
    private final BooleanValue lava;
    private final BooleanValue dipOnSneak;

    public Jesus() {
        super("Jesus", "Walk on liquids like Jesus.", ModuleType.MOVEMENT);

        water = config.create(new BooleanValue.Builder("Water")
                .description("Walk on water.")
                .defaultValue(true)
                .build());

        lava = config.create(new BooleanValue.Builder("Lava")
                .description("Walk on lava.")
                .defaultValue(true)
                .build());

        dipOnSneak = config.create(new BooleanValue.Builder("Dip On Sneak")
                .description("Lets you go into the liquid when sneaking.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        // Don't activate while flying, swimming, in vehicles, or sneaking (if dip-on-sneak)
        if (player.getAbilities().flying || player.isSwimming() || player.getVehicle() != null) return;
        if (dipOnSneak.value() && mc.options.sneakKey.isPressed()) return;

        boolean inWater = player.isTouchingWater();
        boolean inLava = player.isInLava();

        if (inWater && water.value()) {
            player.setVelocity(player.getVelocity().x, 0.11, player.getVelocity().z);
            return;
        }

        if (inLava && lava.value()) {
            player.setVelocity(player.getVelocity().x, 0.11, player.getVelocity().z);
            return;
        }

        // When just above the liquid surface, stabilize
        BlockPos below = player.getBlockPos().down();
        BlockState blockBelow = mc.world.getBlockState(below);

        boolean waterBelow = blockBelow.getFluidState().getFluid() == Fluids.WATER ||
                blockBelow.getFluidState().getFluid() == Fluids.FLOWING_WATER;
        boolean lavaBelow = blockBelow.isOf(Blocks.LAVA);

        if ((waterBelow && water.value()) || (lavaBelow && lava.value())) {
            if (player.getVelocity().y < 0) {
                player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
            }
        }
    }
}
