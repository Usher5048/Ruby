package ruby.systems.gui.icons;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ruby.RubyClient;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws Lucide icons shipped in {@code assets/ruby/icons/lucide}.
 * Icons are rasterized once per pixel size and tinted by the blit color.
 */
public final class Lucide {
    /** Size used by the click GUI rows. */
    public static final int UI = 14;

    private static final Map<String, SvgIcon> ICONS = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> MISSING = new ConcurrentHashMap<>();
    /** Fixed-color strokes. Null until first use so a hotswap does not leave it unset. */
    private static Map<String, Identifier> strokeTextures;
    /** Bumped when icon rasters change. A hotswap leaves this at 0 and rebuilds the cache. */
    private static int cacheStamp;

    private Lucide() {}

    public static void draw(GuiGraphicsExtractor context, String name, int x, int y, int size, int color) {
        Lucide.draw(context, name, x, y, size, color, 0f);
    }

    /** {@code press} is 1 at the click and settles to 0. The icon scales down, then back. */
    public static void draw(GuiGraphicsExtractor context, String name, int x, int y, int size, int color, float press) {
        if (press < 0.02f) {
            Lucide.blit(context, name, x, y, size, color);
            return;
        }
        float scale = 1f - 0.12f * Math.clamp(press, 0f, 1f);
        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x + size / 2f, y + size / 2f);
        pose.scale(scale, scale);
        Lucide.blit(context, name, -size / 2, -size / 2, size, color);
        pose.popMatrix();
    }

    private static void blit(GuiGraphicsExtractor context, String name, int x, int y, int size, int color) {
        Identifier texture = Lucide.texture(name, size);
        if (texture == null) return;
        Lucide.blit(context, texture, x, y, size, color);
        Identifier stroke = Lucide.strokeTexture(name, size);
        if (stroke != null) Lucide.blit(context, stroke, x, y, size, 0xFFFFFFFF);
    }

    private static void blit(GuiGraphicsExtractor context, Identifier texture, int x, int y, int size, int color) {
        context.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x, y,
                0f, 0f,
                size, size,
                size, size,
                size, size,
                color
        );
    }

    public static void drawCentered(
            GuiGraphicsExtractor context, String name,
            int centerX, int centerY, int size, int color, float rotationDeg
    ) {
        Lucide.drawCentered(context, name, centerX, centerY, size, color, rotationDeg, 0f);
    }

    public static void drawCentered(
            GuiGraphicsExtractor context, String name,
            int centerX, int centerY, int size, int color, float rotationDeg, float press
    ) {
        float scale = 1f - 0.12f * Math.clamp(press, 0f, 1f);
        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        if (rotationDeg != 0f) pose.rotate((float) Math.toRadians(rotationDeg));
        if (scale < 0.999f) pose.scale(scale, scale);
        Lucide.blit(context, name, -size / 2, -size / 2, size, color);
        pose.popMatrix();
    }

    private static void refresh() {
        if (Lucide.cacheStamp == 2) return;
        Lucide.ICONS.clear();
        Lucide.TEXTURES.clear();
        Lucide.MISSING.clear();
        if (Lucide.strokeTextures != null) Lucide.strokeTextures.clear();
        Lucide.cacheStamp = 2;
    }

    private static Map<String, Identifier> strokeTextures() {
        if (Lucide.strokeTextures == null) Lucide.strokeTextures = new ConcurrentHashMap<>();
        return Lucide.strokeTextures;
    }

    private static Identifier texture(String name, int size) {
        Lucide.refresh();
        if (size <= 0 || !Lucide.validName(name)) return null;
        String key = name + "@" + size;
        Identifier cached = Lucide.TEXTURES.get(key);
        if (cached != null) return cached;

        SvgIcon icon = Lucide.icon(name);
        if (icon == null) return null;

        BufferedImage image = icon.raster(size);
        NativeImage nativeImage = Lucide.nativeImage(image, size);

        String path = "icons/lucide/" + name + "_" + size + "_v" + Lucide.cacheStamp;
        Identifier id = Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, path);
        RubyClient.client.getTextureManager().register(id, new DynamicTexture(() -> path, nativeImage));
        Lucide.TEXTURES.put(key, id);
        return id;
    }

    private static Identifier strokeTexture(String name, int size) {
        Lucide.refresh();
        if (size <= 0 || !Lucide.validName(name)) return null;
        String key = name + "@" + size;
        Map<String, Identifier> strokes = Lucide.strokeTextures();
        Identifier cached = strokes.get(key);
        if (cached != null) return cached;

        SvgIcon icon = Lucide.icon(name);
        if (icon == null || !icon.hasFixedStroke()) return null;

        BufferedImage image = icon.rasterFixedStroke(size);
        NativeImage nativeImage = Lucide.nativeImage(image, size);
        String path = "icons/lucide/" + name + "_stroke_" + size + "_v" + Lucide.cacheStamp;
        Identifier id = Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, path);
        RubyClient.client.getTextureManager().register(id, new DynamicTexture(() -> path, nativeImage));
        strokes.put(key, id);
        return id;
    }

    private static NativeImage nativeImage(BufferedImage image, int size) {
        NativeImage nativeImage = new NativeImage(size, size, true);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                nativeImage.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return nativeImage;
    }

    private static SvgIcon icon(String name) {
        Lucide.refresh();
        SvgIcon cached = Lucide.ICONS.get(name);
        if (cached != null) return cached;
        if (Lucide.MISSING.containsKey(name)) return null;

        String resource = "/assets/ruby/icons/lucide/" + name + ".svg";
        try (InputStream stream = Lucide.class.getResourceAsStream(resource)) {
            if (stream == null) {
                Lucide.MISSING.put(name, Boolean.TRUE);
                System.err.println("[ruby] missing lucide icon " + name);
                return null;
            }
            SvgIcon icon = SvgIcon.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            Lucide.ICONS.put(name, icon);
            return icon;
        } catch (Exception e) {
            Lucide.MISSING.put(name, Boolean.TRUE);
            System.err.println("[ruby] failed to load lucide icon " + name + ": " + e.getMessage());
            return null;
        }
    }

    private static boolean validName(String name) {
        if (name == null || name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!ok) return false;
        }
        return true;
    }
}
