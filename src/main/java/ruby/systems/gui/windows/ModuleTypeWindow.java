package ruby.systems.gui.windows;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.List;

public class ModuleTypeWindow extends CollapsibleWindow {

    private final List<Module> modules;
    private final String title;

    public ModuleTypeWindow(int x, int y, List<Module> modules) {
        this(x, y, null, modules);
    }

    public ModuleTypeWindow(int x, int y, ModuleType category, List<Module> modules) {
        super(x, y, 240, 42);
        this.modules = modules;
        this.title = category == null ? "Keybinds" : category.toString();

        int yOffset = this.getHeaderHeight();
        for (Module module : this.modules) {
            this.addWindow(new ModuleWindow(0, yOffset, module));
            yOffset += 28;
        }
    }

    public String getTitle() {
        return this.title;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {

        // fix order
        int yOffset = this.getHeaderHeight();
        for(Module module : this.modules) {
            for(Window child : this.windows()) {
                if(!(child instanceof ModuleWindow mWin)) continue;
                if(mWin.module() != module) continue;

                child.setPosition(0, yOffset);
                yOffset += child.getHeight();
                break;
            }
        }

        super.onRender(context, mouseX, mouseY, dt);
    }

    private static final int BOTTOM_PADDING = 8;

    @Override
    protected int getExpandedHeight() {
        return super.getExpandedHeight() + (
                this.handleChildren ?
                        BOTTOM_PADDING : 0
        );
    }

    @Override
    public void drawHeader(GuiGraphicsExtractor context) {
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();
        int h = this.getHeight();
        int hdr = this.getHeaderHeight();

        // Drop shadow
        fillSmoothRoundedRect(context, 2, 6, w + 2, h + 6, 12, 0x44000000);

        // Inset border (fully rounded)
        fillSmoothRoundedRect(context, 0, 0, w, h, 10, 0x0AFFFFFF);
        fillSmoothRoundedRect(context, 1, 1, w - 1, h - 1, 9, 0xD90A0A0A);

        // Header title centered
        this.drawCenteredText(
                style.subHeaderFont(),
                context,
                this.title,
                w / 2,
                hdr / 2,
                0xFFFFFFFF
        );

        // Separator line below header
        context.fill(1, hdr - 1, w - 1, hdr, 0x0DFFFFFF);
    }



    /**
     * Draws a rounded panel with a 1px inset border matching test.html.
     */
    public static void drawRoundedPanel(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int radius, int bg, int border) {
        ModuleTypeWindow.drawRoundedPanel(ctx, x1, y1, x2, y2, radius, bg, border, true);
    }

    public static void drawRoundedPanel(
            GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int radius, int bg, int border, boolean roundBottom
    ) {
        if (roundBottom) {
            fillSmoothRoundedRect(ctx, x1, y1, x2, y2, radius, border);
            fillSmoothRoundedRect(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, radius - 1), bg);
        } else {
            fillTopRoundedRect(ctx, x1, y1, x2, y2, radius, border);
            fillTopRoundedRect(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, radius - 1), bg);
        }
    }

    /** Flat bottom edge, rounded top corners — used when the module panel is collapsed. */
    public static void fillTopRoundedRect(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        ctx.fill(x1, y1 + r, x2, y2, color);
        for (int y = 0; y < r; y++) {
            fillCappedSpan(ctx, x1, x2, y1 + y, r, y, color);
        }
    }

    public static void fillRowBackground(
            GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color, boolean roundBottom
    ) {
        ModuleTypeWindow.fillRowBackground(ctx, x1, y1, x2, y2, color, roundBottom, GUIStyle.RADIUS_PANEL);
    }

    /**
     * Last-row hover sits on the container's bottom edge. The container stroke owns the
     * outer pixel, so the highlight is clipped to the inner corner (radius - 1).
     */
    public static void fillRowBackground(
            GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color,
            boolean roundBottom, int containerRadius
    ) {
        if (!roundBottom || containerRadius <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }
        int clipBottom = y2 - 1;
        if (clipBottom <= y1) return;
        ModuleTypeWindow.fillBottomRoundedRect(ctx, x1, y1, x2, clipBottom, containerRadius - 1, color);
    }

    public static void drawRoundedBadge(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int bg, int border) {
        int r = GUIStyle.RADIUS_BADGE;
        fillSmoothRoundedRect(ctx, x1, y1, x2, y2, r, border);
        fillSmoothRoundedRect(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), bg);
    }

    private static final int MAX_CORNER_RADIUS = 32;
    /** Not final: a hotswap can add these fields without re-running a static initializer, leaving them null. */
    private static volatile int[][] CORNER_INSET;
    private static volatile float[][] CORNER_COVERAGE;

    /**
     * Fills the corner lookup if a hotswap skipped class initialization.
     * The tables are built locally and published together so a partial init cannot be observed.
     */
    private static synchronized void ensureCornerTables() {
        if (CORNER_INSET != null && CORNER_COVERAGE != null) return;
        int[][] inset = new int[MAX_CORNER_RADIUS + 1][];
        float[][] coverage = new float[MAX_CORNER_RADIUS + 1][];
        for (int r = 1; r <= MAX_CORNER_RADIUS; r++) {
            inset[r] = new int[r];
            coverage[r] = new float[r];
            double r2 = (double) r * r;
            for (int y = 0; y < r; y++) {
                double dy = r - y - 0.5;
                double dx = Math.sqrt(r2 - dy * dy);
                double edgeX = r - dx;
                int fullInset = (int) Math.ceil(edgeX);
                inset[r][y] = fullInset;
                coverage[r][y] = (float) (fullInset - edgeX);
            }
        }
        CORNER_INSET = inset;
        CORNER_COVERAGE = coverage;
    }

    /** Horizontal inset of a rounded corner scanline. Same curve as {@link #fillSmoothRoundedRect}. */
    static int cornerInset(int radius, int y) {
        if (radius <= 0 || y >= radius) return 0;
        if (y < 0) return radius;
        int x = 0;
        while (x < radius && spanCoverage(radius, y, x) < 0.99f) x++;
        return x;
    }

    private static float cornerCoverage(int radius, int y) {
        if (radius <= MAX_CORNER_RADIUS) {
            if (CORNER_COVERAGE == null) ensureCornerTables();
            return CORNER_COVERAGE[radius][y];
        }
        double dy = radius - y - 0.5;
        double dx = Math.sqrt(Math.max(0.0, radius * (double) radius - dy * dy));
        double edgeX = radius - dx;
        return (float) (Math.ceil(edgeX) - edgeX);
    }

    /**
     * Draws a filled rounded rectangle with anti-aliased edges.
     */
    public static void fillSmoothRoundedRect(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int width = x2 - x1;
        int height = y2 - y1;
        int r = Math.min(radius, Math.min(width / 2, height / 2));
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        // A 1px radius on a thin rect only shaves a fraction of the corner pixel, so it reads as a square.
        // Cut that pixel back to half alpha once the rect is wide enough for the curve to show.
        if (radius == 1 && r == 1 && width >= 3 && height >= 3) {
            ctx.fill(x1, y1 + 1, x2, y2 - 1, color);
            ctx.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
            ctx.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
            int cornerAlpha = baseAlpha >> 1;
            if (cornerAlpha > 0) {
                int edge = rgb | (cornerAlpha << 24);
                ctx.fill(x1, y1, x1 + 1, y1 + 1, edge);
                ctx.fill(x2 - 1, y1, x2, y1 + 1, edge);
                ctx.fill(x1, y2 - 1, x1 + 1, y2, edge);
                ctx.fill(x2 - 1, y2 - 1, x2, y2, edge);
            }
            return;
        }

        ctx.fill(x1, y1 + r, x2, y2 - r, color);
        for (int y = 0; y < r; y++) {
            fillCappedSpan(ctx, x1, x2, y1 + y, r, y, color);
            fillCappedSpan(ctx, x1, x2, y2 - y - 1, r, y, color);
        }
    }

    /**
     * Left accent strip flush with the panel's rounded left edge.
     */
    public static void fillLeftAccentStrip(
            GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int radius, int accentW, int color
    ) {
        if (accentW <= 0 || x2 <= x1 || y2 <= y1) return;

        int accentRight = Math.min(x1 + accentW, x2);
        int h = y2 - y1;
        int r = Math.min(radius, h / 2);
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        if (r <= 0) {
            ctx.fill(x1, y1, accentRight, y2, color);
            return;
        }

        ctx.fill(x1, y1 + r, accentRight, y2 - r, color);

        for (int y = 0; y < r; y++) {
            int fullInset = cornerInset(r, y);
            int edgeAlpha = (int) (baseAlpha * cornerCoverage(r, y));
            int edgeColor = rgb | (edgeAlpha << 24);

            int top = y1 + y;
            int bottom = y2 - y - 1;

            if (accentRight > x1 + fullInset) {
                ctx.fill(x1 + fullInset, top, accentRight, top + 1, color);
                ctx.fill(x1 + fullInset, bottom, accentRight, bottom + 1, color);
            }

            if (edgeAlpha > 0 && fullInset > 0 && x1 + fullInset - 1 < accentRight) {
                ctx.fill(x1 + fullInset - 1, top, x1 + fullInset, top + 1, edgeColor);
                ctx.fill(x1 + fullInset - 1, bottom, x1 + fullInset, bottom + 1, edgeColor);
            }
        }
    }

    /** Soft 1px inset outline with anti-aliased edges. */
    public static void strokeSmoothRoundedRect(
            GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int radius, int color
    ) {
        if (x2 <= x1 || y2 <= y1) return;

        float cx = (x1 + x2) * 0.5f;
        float cy = (y1 + y2) * 0.5f;
        float halfW = (x2 - x1) * 0.5f;
        float halfH = (y2 - y1) * 0.5f;
        float r = Math.min(radius, Math.min(halfW, halfH));
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        for (int y = y1 - 1; y < y2 + 1; y++) {
            for (int x = x1 - 1; x < x2 + 1; x++) {
                float d = sdRoundedBox(x + 0.5f, y + 0.5f, cx, cy, halfW, halfH, r);
                if (d > 0f || d < -1f) continue;

                int alpha = (int) (baseAlpha * (1f + d));
                if (alpha <= 0) continue;
                ctx.fill(x, y, x + 1, y + 1, rgb | (alpha << 24));
            }
        }
    }

    /** Nametag panel: smooth fill, left accent, soft outline (no inset double-fill). */
    public static void drawNametagPanel(
            GuiGraphicsExtractor ctx,
            int x1,
            int y1,
            int x2,
            int y2,
            int radius,
            int bg,
            int border,
            int accentColor,
            int accentW
    ) {
        fillSmoothRoundedRect(ctx, x1, y1, x2, y2, radius, bg);
        strokeSmoothRoundedRect(ctx, x1, y1, x2, y2, radius, border);
        fillLeftAccentStrip(ctx, x1, y1, x2, y2, radius, accentW, accentColor);
    }

    private static float sdRoundedBox(
            float px, float py, float cx, float cy, float halfW, float halfH, float radius
    ) {
        float r = Math.min(radius, Math.min(halfW, halfH));
        float qx = Math.abs(px - cx) - halfW + r;
        float qy = Math.abs(py - cy) - halfH + r;
        float ox = Math.max(qx, 0f);
        float oy = Math.max(qy, 0f);
        return (float) (Math.hypot(ox, oy) + Math.min(Math.max(qx, qy), 0f) - r);
    }

    /**
     * Draws a filled rectangle with flat top corners and rounded bottom corners.
     */
    public static void fillBottomRoundedRect(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        // Top flat + center body (y1 to y2-r, full width)
        ctx.fill(x1, y1, x2, y2 - r, color);
        for (int y = 0; y < r; y++) {
            fillCappedSpan(ctx, x1, x2, y2 - y - 1, r, y, color);
        }
    }

    /**
     * One scanline of a rounded cap. Coverage is a short ramp around the true circle
     * so the corner stays smooth when the GUI is scaled up.
     */
    private static void fillCappedSpan(
            GuiGraphicsExtractor ctx, int x1, int x2, int pixelY, int radius, int row, int color
    ) {
        int baseAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int limit = Math.min(radius + 2, (x2 - x1) / 2);
        int solid = 0;
        while (solid < limit && spanCoverage(radius, row, solid) < 0.99f) solid++;
        if (x1 + solid < x2 - solid) {
            ctx.fill(x1 + solid, pixelY, x2 - solid, pixelY + 1, color);
        }
        for (int i = 0; i < solid; i++) {
            float coverage = spanCoverage(radius, row, i);
            if (coverage <= 0.004f) continue;
            int alpha = Math.min(255, Math.round(baseAlpha * coverage));
            if (alpha <= 0) continue;
            int edge = rgb | (alpha << 24);
            ctx.fill(x1 + i, pixelY, x1 + i + 1, pixelY + 1, edge);
            ctx.fill(x2 - i - 1, pixelY, x2 - i, pixelY + 1, edge);
        }
    }

    /** How much of the pixel at {@code x} on corner row {@code row} sits inside the circle. */
    private static float spanCoverage(int radius, int row, int x) {
        double dx = x + 0.5 - radius;
        double dy = row + 0.5 - radius;
        double signed = Math.hypot(dx, dy) - radius;
        return Math.clamp((float) ((0.85 - signed) / 1.7), 0f, 1f);
    }
}
