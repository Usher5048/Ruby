package ruby.helpers.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import ruby.RubyClient;

/** World-to-screen helpers for HUD overlays (framebuffer pixels; GUI scale applied by {@code Modules.render2D}). */
public final class NametagUtils {

    public record ScreenBounds(double x1, double y1, double x2, double y2) {}

    /** Not final: a hotswap does not re-run static init, so this stays null until first use. */
    private static Vector4f projectVec;

    private static double x;
    private static double y;
    public static double scale = 1.0;

    private NametagUtils() {}

    public static boolean to2D(Vec3 worldPos, double tagScale) {
        return to2D(worldPos, tagScale, true, 0.85);
    }

    public static boolean to2D(Vec3 worldPos, double tagScale, boolean distanceScaling, double minScale) {
        if (!project(worldPos)) return false;

        scale = tagScale;
        if (distanceScaling) {
            Vec3 cam = Renderer.cameraPos();
            double dist = cam.distanceTo(worldPos);
            // Full size up close, then shrinks in proportion to distance.
            double distScale = Mth.clamp(5.0 / Math.max(dist, 5.0), minScale, 1.0);
            scale *= distScale;
        }
        return true;
    }

    public static ScreenBounds projectBounds(AABB box) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;

        double x0 = box.minX, y0 = box.minY, z0 = box.minZ;
        double x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        Vec3[] corners = {
                new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x0, y0, z1), new Vec3(x1, y0, z1),
                new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1)
        };

        for (Vec3 corner : corners) {
            if (!project(corner)) continue;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            any = true;
        }

        return any ? new ScreenBounds(minX, minY, maxX, maxY) : null;
    }

    private static boolean project(Vec3 worldPos) {
        Renderer.ensureCamera();
        if (Renderer.projection() == null || Renderer.view() == null) return false;

        Vec3 cam = Renderer.cameraPos();
        Vector4f vec = projectVec;
        if (vec == null) projectVec = vec = new Vector4f();
        vec.set(
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
