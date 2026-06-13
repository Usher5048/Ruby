package ruby.helpers.render;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;
import ruby.RubyClient;

/** World-to-screen helpers for HUD overlays (framebuffer pixels; GUI scale applied by {@code Modules.render2D}). */
public final class NametagUtils {

    public record ScreenBounds(double x1, double y1, double x2, double y2) {}

    private static double x;
    private static double y;
    public static double scale = 1.0;

    private NametagUtils() {}

    public static boolean to2D(Vec3d worldPos, double tagScale) {
        return to2D(worldPos, tagScale, true, 0.85);
    }

    public static boolean to2D(Vec3d worldPos, double tagScale, boolean distanceScaling, double minScale) {
        if (!project(worldPos)) return false;

        scale = tagScale;
        if (distanceScaling) {
            Vec3d cam = RubyClient.client.gameRenderer.getCamera().getCameraPos();
            double distScale = MathHelper.clamp(1.0 - cam.distanceTo(worldPos) * 0.004, minScale, 1.0);
            scale *= distScale;
        }
        return true;
    }

    public static ScreenBounds projectBounds(Box box) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;

        double x0 = box.minX, y0 = box.minY, z0 = box.minZ;
        double x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        Vec3d[] corners = {
                new Vec3d(x0, y0, z0), new Vec3d(x1, y0, z0), new Vec3d(x0, y0, z1), new Vec3d(x1, y0, z1),
                new Vec3d(x0, y1, z0), new Vec3d(x1, y1, z0), new Vec3d(x0, y1, z1), new Vec3d(x1, y1, z1)
        };

        for (Vec3d corner : corners) {
            if (!project(corner)) continue;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            any = true;
        }

        return any ? new ScreenBounds(minX, minY, maxX, maxY) : null;
    }

    private static boolean project(Vec3d worldPos) {
        if (Renderer.projection() == null || Renderer.view() == null) return false;

        Vec3d cam = RubyClient.client.gameRenderer.getCamera().getCameraPos();
        Vector4f vec = new Vector4f(
                (float) (worldPos.x - cam.x),
                (float) (worldPos.y - cam.y),
                (float) (worldPos.z - cam.z),
                1f
        );

        Renderer.view().transform(vec);
        Renderer.projection().transform(vec);
        if (vec.w <= 0f) return false;

        float invW = 1f / vec.w;
        double ndcX = vec.x * invW;
        double ndcY = vec.y * invW;

        int width = RubyClient.client.getWindow().getWidth();
        int height = RubyClient.client.getWindow().getHeight();
        x = (ndcX * 0.5 + 0.5) * width;
        y = (1.0 - (ndcY * 0.5 + 0.5)) * height;
        return true;
    }

    public static double getX() {
        return x;
    }

    public static double getY() {
        return y;
    }
}
