package ruby.systems.modules.render;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import ruby.RubyClient;
import ruby.helpers.render.RenderShapes;
import ruby.systems.config.BlockListValue;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Block ESP with Meteor-style merged group outlines.
 */
public class BlockESP extends Module {

    private static final BlockPos[] NEIGHBORS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    private final BlockListValue blocks;
    private final BooleanValue tracers;
    private final EnumValue<RenderShapes.ShapeMode> shapeMode;
    private final DoubleValue fillOpacity;
    private final ColorValue sideColor;
    private final ColorValue lineColor;

    private final Set<BlockPos> found = new HashSet<>();
    private int scanTimer;

    public BlockESP() {
        super("Block ESP", "Renders specified blocks through walls.", ModuleType.RENDER);

        blocks = config.create(new BlockListValue.Builder("Blocks")
                .defaultValue(Xray.DEFAULT_ORES)
                .build());
        tracers = config.create(new BooleanValue.Builder("Tracers").defaultValue(false).build());
        shapeMode = config.create(new EnumValue.Builder<RenderShapes.ShapeMode>("Shape Mode")
                .defaultValue(RenderShapes.ShapeMode.Both).build());
        fillOpacity = config.create(new DoubleValue.Builder("Fill Opacity")
                .range(0, 1, 0.05).defaultValue(0.25).build());
        sideColor = config.create(new ColorValue.Builder("Side Color").defaultValue(0x5900FFC8).build());
        lineColor = config.create(new ColorValue.Builder("Line Color").defaultValue(0xFF00FFC8).build());
    }

    @Override
    public void onEnable() {
        found.clear();
        scanTimer = 0;
    }

    @Override
    public void onDisable() {
        found.clear();
    }

    @Override
    public String getInfoString() {
        return Integer.toString(found.size());
    }

    @Override
    public void tick() {
        if (RubyClient.client.world == null || RubyClient.client.player == null) return;

        found.removeIf(pos -> !matches(pos));

        if (--scanTimer > 0) return;
        scanTimer = 8;

        found.clear();
        int radius = Math.min(4, RubyClient.client.options.getViewDistance().getValue());
        ChunkPos center = RubyClient.client.player.getChunkPos();
        int minY = Math.max(RubyClient.client.world.getBottomY(), (int) RubyClient.client.player.getY() - 48);
        int maxY = Math.min(RubyClient.client.world.getTopYInclusive(), (int) RubyClient.client.player.getY() + 48);

        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                WorldChunk chunk = RubyClient.client.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                ChunkPos cp = chunk.getPos();
                int startX = cp.getStartX();
                int startZ = cp.getStartZ();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                            if (matches(pos)) found.add(pos.toImmutable());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render3D() {
        if (found.isEmpty()) return;

        found.removeIf(pos -> !matches(pos));
        if (found.isEmpty()) return;

        int side = multiplyAlpha(sideColor.value(), fillOpacity.value().floatValue());
        int line = lineColor.value();

        Set<BlockPos> remaining = new HashSet<>(found);
        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            Block block = RubyClient.client.world.getBlockState(start).getBlock();
            Set<BlockPos> group = floodFill(start, block, remaining);

            RenderShapes.mergedGroup(group, side, line, shapeMode.value());

            if (tracers.value()) {
                double cx = 0, cy = 0, cz = 0;
                for (BlockPos pos : group) {
                    cx += pos.getX() + 0.5;
                    cy += pos.getY() + 0.5;
                    cz += pos.getZ() + 0.5;
                }
                cx /= group.size();
                cy /= group.size();
                cz /= group.size();
                RenderShapes.lineToCenter(cx, cy, cz, line);
            }
        }
    }

    private boolean matches(BlockPos pos) {
        return blocks.value().contains(RubyClient.client.world.getBlockState(pos).getBlock());
    }

    private Set<BlockPos> floodFill(BlockPos start, Block block, Set<BlockPos> remaining) {
        Set<BlockPos> group = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        remaining.remove(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            group.add(pos);

            for (BlockPos offset : NEIGHBORS) {
                BlockPos neighbor = pos.add(offset);
                if (!remaining.contains(neighbor)) continue;
                if (RubyClient.client.world.getBlockState(neighbor).getBlock() != block) continue;

                remaining.remove(neighbor);
                queue.add(neighbor);
            }
        }

        return group;
    }

    private static int multiplyAlpha(int color, float alpha) {
        int a = (color >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0f, Math.min(1f, alpha)));
        return (scaled << 24) | (color & 0x00FFFFFF);
    }
}
