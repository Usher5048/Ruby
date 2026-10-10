package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // Don't activate while flying, swimming, in vehicles, or sneaking (if dip-on-sneak)
        if (player.getAbilities().flying || player.isSwimming() || player.getVehicle() != null) return;
        if (dipOnSneak.value() && mc.options.keyShift.isDown()) return;

        boolean inWater = player.isInWater();
        boolean inLava = player.isInLava();

        if (inWater && water.value()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.11, player.getDeltaMovement().z);
            return;
        }

        if (inLava && lava.value()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.11, player.getDeltaMovement().z);
            return;
        }

        // When just above the liquid surface, stabilize
        BlockPos below = player.blockPosition().below();
        BlockState blockBelow = mc.level.getBlockState(below);

        boolean waterBelow = blockBelow.getFluidState().getType() == Fluids.WATER ||
                blockBelow.getFluidState().getType() == Fluids.FLOWING_WATER;
        boolean lavaBelow = blockBelow.is(Blocks.LAVA);

        if ((waterBelow && water.value()) || (lavaBelow && lava.value())) {
            if (player.getDeltaMovement().y < 0) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
            }
        }
    }
}
