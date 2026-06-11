package ruby.systems.hud;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import ruby.systems.gui.text.FontRenderer;

/**
 * HUD draw helpers modeled on Meteor Client's {@code HudRenderer} text/quad API.
 * <p>
 * Meteor batches colored quads before text so outlines and backgrounds sit behind labels.
 * This class draws quads before text per call site to match that visual result on {@link DrawContext}.
 *
 * @see <a href="https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/hud/HudRenderer.java">HudRenderer.java</a>
 */
public final class HudRenderer {
    public static final HudRenderer INSTANCE = new HudRenderer();

    private static final int SHADOW_COLOR = 0x80000000;

    private DrawContext context;
    private FontRenderer font;
    public double delta;

    private HudRenderer() {}

    public void begin(DrawContext context, FontRenderer font, double delta) {
        this.context = context;
        this.font = font;
        this.delta = delta;
    }

    public void quad(double x, double y, double width, double height, int color) {
        this.quad(x, y, width, height, color, color, color, color);
    }

    /**
     * Colored quad with independent corner colors, like {@code Renderer2D.COLOR.quad}.
     */
    public void quad(double x, double y, double width, double height,
                     int topLeft, int topRight, int bottomRight, int bottomLeft) {
        int x1 = (int) Math.floor(x);
        int y1 = (int) Math.floor(y);
        int x2 = (int) Math.ceil(x + width);
        int y2 = (int) Math.ceil(y + height);

        if (topLeft == topRight && bottomLeft == bottomRight && topLeft == bottomLeft) {
            this.context.fill(x1, y1, x2, y2, topLeft);
        } else if (topLeft == topRight && bottomLeft == bottomRight) {
            this.context.fillGradient(x1, y1, x2, y2, topLeft, bottomLeft);
        } else if (topLeft == bottomLeft && topRight == bottomRight) {
            this.drawHorizontalGradient(x1, y1, x2, y2, topLeft, topRight);
        } else {
            this.context.fill(x1, y1, x2, y2, bottomRight);
        }
    }

    /**
     * Text draw with per-call scale, matching Meteor's {@code VanillaTextRenderer} when {@code scaleIndividually} is on.
     */
    public double text(String text, double x, double y, int color, boolean shadow, double scale) {
        if (text.isEmpty()) return 0;

        x += 0.5 * scale;
        y += 0.5 * scale;

        Matrix3x2fStack matrices = this.context.getMatrices();
        matrices.pushMatrix();
        matrices.scale((float) scale, (float) scale);

        float drawX = (float) (x / scale);
        float drawY = (float) (y / scale);

        if (shadow) {
            this.font.draw(this.context, text, (int) drawX + 1, (int) drawY + 1, HudRenderer.SHADOW_COLOR);
        }
        this.font.draw(this.context, text, (int) drawX, (int) drawY, color);

        matrices.popMatrix();
        return this.textWidth(text, shadow, scale);
    }

    public double textWidth(String text, boolean shadow, double scale) {
        if (text.isEmpty()) return 0;
        return (this.font.getWidth(text) + (shadow ? 1 : 0)) * scale + (shadow ? 1 : 0);
    }

    public double textHeight(boolean shadow, double scale) {
        return (this.font.fontHeight + (shadow ? 1 : 0)) * scale;
    }

    private void drawHorizontalGradient(int x1, int y1, int x2, int y2, int leftColor, int rightColor) {
        int width = x2 - x1;
        if (width <= 0) return;

        for (int px = 0; px < width; px++) {
            float t = width == 1 ? 1f : px / (float) (width - 1);
            int color = this.lerpArgb(leftColor, rightColor, t);
            this.context.fill(x1 + px, y1, x1 + px + 1, y2, color);
        }
    }

    private int lerpArgb(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = (int) (((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = (int) (((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
