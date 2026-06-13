package ruby.helpers.render;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

/**
 * Shape-mode box rendering using the client {@link Renderer}.
 */
public final class RenderShapes {

    public enum ShapeMode { Lines, Sides, Both }

    private RenderShapes() {}

    /**
     * Renders only exterior faces of a connected block group (Meteor-style merged outline).
     */
    public static void mergedGroup(Set<BlockPos> group, int sideColor, int lineColor, ShapeMode mode) {
        if (group.isEmpty()) return;

        boolean lines = mode == ShapeMode.Lines || mode == ShapeMode.Both;
        boolean sides = mode == ShapeMode.Sides || mode == ShapeMode.Both;

        if (lines) {
            Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
            Renderer.color(lineColor);
        }

        if (sides) {
            Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
            Renderer.color(sideColor);
        }

        for (BlockPos pos : group) {
            for (Direction dir : Direction.values()) {
                if (group.contains(pos.offset(dir))) continue;

                if (lines && sides) {
                    Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
                    Renderer.color(lineColor);
                    Renderer.face(pos, dir);
                    Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
                    Renderer.color(sideColor);
                    Renderer.face(pos, dir);
                } else if (lines) {
                    Renderer.face(pos, dir);
                } else {
                    Renderer.face(pos, dir);
                }
            }
        }
    }

    public static void box(double x1, double y1, double z1, double x2, double y2, double z2,
                           int sideColor, int lineColor, ShapeMode mode) {
        Vec3d center = new Vec3d((x1 + x2) / 2.0, (y1 + y2) / 2.0, (z1 + z2) / 2.0);
        Vec3d size = new Vec3d(x2 - x1, y2 - y1, z2 - z1);

        if (mode == ShapeMode.Sides || mode == ShapeMode.Both) {
            Renderer.setMode(Renderer.Mode.FILL_ALWAYS_ON_TOP);
            Renderer.color(sideColor);
            Renderer.cuboid(center, size);
        }

        if (mode == ShapeMode.Lines || mode == ShapeMode.Both) {
            Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
            Renderer.color(lineColor);
            Renderer.cuboid(center, size);
        }
    }

    public static void box(Vec3d min, Vec3d max, int sideColor, int lineColor, ShapeMode mode) {
        box(min.x, min.y, min.z, max.x, max.y, max.z, sideColor, lineColor, mode);
    }

    public static void lineToCenter(double x, double y, double z, int color) {
        Vec3d center = Renderer.getScreenCenter();
        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
        Renderer.color(color);
        Renderer.line(center.x, center.y, center.z, x, y, z);
    }

    public static int scaleAlpha(int color, double factor) {
        int a = (color >>> 24) & 0xFF;
        int scaled = (int) Math.round(a * Math.max(0, Math.min(1, factor)));
        return (scaled << 24) | (color & 0x00FFFFFF);
    }
}
