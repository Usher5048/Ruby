package ruby.systems.modules.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ruby.systems.config.BlockListValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's Nuker: Iterates blocks around the player in a configurable shape/range
 * and breaks them. Modes:
 * - Normal: Breaks all breakable blocks
 * - Flatten: Only breaks blocks at or above the player's feet level
 * - Smash: Only breaks blocks that can be broken instantly (hardness 0)
 */
public class Nuker extends Module {
    public enum Mode { Normal, Flatten, Smash }
    public enum Shape { Sphere, Cube }
    public enum ListMode { Blacklist, Whitelist }

    private final DoubleValue range;
    private final EnumValue<Mode> mode;
    private final EnumValue<Shape> shape;
    private final IntegerValue maxBlocksPerTick;
    private final EnumValue<ListMode> listMode;
    private final BlockListValue blockList;

    public Nuker() {
        super("Nuker", "Breaks blocks around you.", ModuleCategory.WORLD);

        range = config.create(new DoubleValue.Builder("Range")
                .description("The radius of the area to break.")
                .defaultValue(4.0).min(1).max(6).step(0.1)
                .build());

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Which blocks to break.")
                .defaultValue(Mode.Normal)
                .build());

        shape = config.create(new EnumValue.Builder<Shape>("Shape")
                .description("The shape of the area to break.")
                .defaultValue(Shape.Sphere)
                .build());

        maxBlocksPerTick = config.create(new IntegerValue.Builder("Max Blocks/Tick")
                .description("Maximum number of blocks to break per tick.")
                .defaultValue(1).min(1).max(20)
                .build());

        listMode = config.create(new EnumValue.Builder<ListMode>("List Mode")
                .description("Whether the block list acts as a blacklist or whitelist.")
                .defaultValue(ListMode.Blacklist)
                .build());

        blockList = config.create(new BlockListValue.Builder("Blocks")
                .description("Blocks to include/exclude based on list mode.")
                .defaultValue()
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) return;

        BlockPos center = player.getBlockPos();
        int r = (int) Math.ceil(range.value());
        double rSq = range.value() * range.value();
        int broken = 0;

        for (int y = r; y >= -r; y--) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (broken >= maxBlocksPerTick.value()) return;

                    BlockPos pos = center.add(x, y, z);

                    // Shape check
                    if (shape.value() == Shape.Sphere) {
                        double dx = pos.getX() - center.getX();
                        double dy = pos.getY() - center.getY();
                        double dz = pos.getZ() - center.getZ();
                        if (dx * dx + dy * dy + dz * dz > rSq) continue;
                    }

                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isAir()) continue;

                    Block block = state.getBlock();
                    float hardness = state.getHardness(mc.world, pos);

                    // Block list filter
                    if (!blockList.value().isEmpty()) {
                        boolean inList = blockList.value().contains(block);
                        if (listMode.value() == ListMode.Whitelist && !inList) continue;
                        if (listMode.value() == ListMode.Blacklist && inList) continue;
                    }

                    // Mode check
                    switch (mode.value()) {
                        case Flatten -> {
                            if (pos.getY() < center.getY()) continue;
                        }
                        case Smash -> {
                            if (hardness != 0) continue;
                        }
                        case Normal -> {
                            if (hardness < 0) continue; // Bedrock / unbreakable
                        }
                    }

                    // Also skip unbreakable in Flatten mode
                    if (mode.value() == Mode.Flatten && hardness < 0) continue;

                    mc.interactionManager.attackBlock(pos, Direction.UP);
                    player.swingHand(Hand.MAIN_HAND);
                    broken++;
                }
            }
        }
    }
}
