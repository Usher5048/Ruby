package ruby.systems.modules.render;

import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import ruby.RubyClient;
import ruby.helpers.render.RenderShapes;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage ESP ported from Meteor Client (box mode).
 */
public class StorageESP extends Module {

    private final BooleanValue tracers;
    private final EnumValue<RenderShapes.ShapeMode> shapeMode;
    private final DoubleValue fillOpacity;
    private final DoubleValue fadeDistance;
    private final ColorValue chestColor;
    private final ColorValue trappedChestColor;
    private final ColorValue barrelColor;
    private final ColorValue shulkerColor;
    private final ColorValue enderChestColor;
    private final ColorValue otherColor;

    private int count;

    public StorageESP() {
        super("Storage ESP", "Renders storage blocks through walls.", ModuleType.RENDER);

        tracers = config.create(new BooleanValue.Builder("Tracers").defaultValue(false).build());
        shapeMode = config.create(new EnumValue.Builder<RenderShapes.ShapeMode>("Shape Mode")
                .defaultValue(RenderShapes.ShapeMode.Both).build());
        fillOpacity = config.create(new DoubleValue.Builder("Fill Opacity")
                .range(0, 1, 0.05).defaultValue(0.2).build());
        fadeDistance = config.create(new DoubleValue.Builder("Fade Distance")
                .range(0, 32, 1).defaultValue(6.0).build());
        chestColor = config.create(new ColorValue.Builder("Chest").defaultValue(0xFFFFA000).build());
        trappedChestColor = config.create(new ColorValue.Builder("Trapped Chest").defaultValue(0xFFFF0000).build());
        barrelColor = config.create(new ColorValue.Builder("Barrel").defaultValue(0xFFFFA000).build());
        shulkerColor = config.create(new ColorValue.Builder("Shulker").defaultValue(0xFFFFA000).build());
        enderChestColor = config.create(new ColorValue.Builder("Ender Chest").defaultValue(0xFF7800FF).build());
        otherColor = config.create(new ColorValue.Builder("Other").defaultValue(0xFF8C8C8C).build());
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    @Override
    public void render3D() {
        if (RubyClient.client.world == null || RubyClient.client.player == null) return;

        count = 0;
        for (BlockEntity blockEntity : collectBlockEntities()) {
            int line = colorFor(blockEntity);
            if (line == 0) continue;

            BlockPos pos = blockEntity.getPos();
            double dist = RubyClient.client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            double fade = fadeDistance.value();
            double alpha = fade <= 0 ? 1 : Math.max(0.075, dist <= fade * fade ? dist / (fade * fade) : 1);

            int side = multiplyAlpha(line, fillOpacity.value().floatValue() * (float) alpha);
            int lineCol = RenderShapes.scaleAlpha(line, alpha);

            double x1 = pos.getX(), y1 = pos.getY(), z1 = pos.getZ();
            double x2 = x1 + 1, y2 = y1 + 1, z2 = z1 + 1;

            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity) {
                double inset = 1.0 / 16.0;
                x1 += inset; z1 += inset; x2 -= inset; z2 -= inset; y2 -= inset * 2;
            }

            RenderShapes.box(x1, y1, z1, x2, y2, z2, side, lineCol, shapeMode.value());

            if (tracers.value()) {
                RenderShapes.lineToCenter(x1 + 0.5, y1 + 0.5, z1 + 0.5, lineCol);
            }

            count++;
        }
    }

    private List<BlockEntity> collectBlockEntities() {
        List<BlockEntity> entities = new ArrayList<>();
        int radius = Math.min(RubyClient.client.options.getViewDistance().getValue(), 8);
        ChunkPos center = RubyClient.client.player.getChunkPos();

        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                WorldChunk chunk = RubyClient.client.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                entities.addAll(chunk.getBlockEntities().values());
            }
        }

        return entities;
    }

    private int colorFor(BlockEntity blockEntity) {
        if (blockEntity instanceof TrappedChestBlockEntity) return trappedChestColor.value();
        if (blockEntity instanceof ChestBlockEntity) return chestColor.value();
        if (blockEntity instanceof BarrelBlockEntity) return barrelColor.value();
        if (blockEntity instanceof ShulkerBoxBlockEntity) return shulkerColor.value();
        if (blockEntity instanceof EnderChestBlockEntity) return enderChestColor.value();
        if (blockEntity instanceof AbstractFurnaceBlockEntity
                || blockEntity instanceof DispenserBlockEntity
                || blockEntity instanceof HopperBlockEntity
                || blockEntity instanceof BrewingStandBlockEntity) {
            return otherColor.value();
        }
        return 0;
    }

    private static int multiplyAlpha(int color, float alpha) {
        int a = (color >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0f, Math.min(1f, alpha)));
        return (scaled << 24) | (color & 0x00FFFFFF);
    }
}
