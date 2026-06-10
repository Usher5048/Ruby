package ruby.systems.gui.windows;

import net.minecraft.client.gui.DrawContext;
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
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {

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
    public void drawHeader(DrawContext context) {
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
    public static void drawRoundedPanel(DrawContext ctx, int x1, int y1, int x2, int y2, int radius, int bg, int border) {
        ModuleTypeWindow.drawRoundedPanel(ctx, x1, y1, x2, y2, radius, bg, border, true);
    }

    public static void drawRoundedPanel(
            DrawContext ctx, int x1, int y1, int x2, int y2, int radius, int bg, int border, boolean roundBottom
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
    public static void fillTopRoundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        ctx.fill(x1, y1 + r, x2, y2, color);

        for (int y = 0; y < r; y++) {
            double dy = r - y - 0.5;
            double dx = Math.sqrt((double) r * r - dy * dy);
            double edgeX = r - dx;
            int fullInset = (int) Math.ceil(edgeX);
            double coverage = fullInset - edgeX;
            int edgeAlpha = (int) (baseAlpha * coverage);
            int edgeColor = rgb | (edgeAlpha << 24);

            ctx.fill(x1 + fullInset, y1 + y, x2 - fullInset, y1 + y + 1, color);

            if (edgeAlpha > 0 && fullInset > 0) {
                ctx.fill(x1 + fullInset - 1, y1 + y, x1 + fullInset, y1 + y + 1, edgeColor);
                ctx.fill(x2 - fullInset, y1 + y, x2 - fullInset + 1, y1 + y + 1, edgeColor);
            }
        }
    }

    /** Small chevron arrow drawn with pixels so it works without unicode font glyphs. */
    public static void drawChevron(DrawContext ctx, int x, int y, int color, boolean pointRight) {
        for (int i = 0; i < 5; i++) {
            int inset = Math.min(i, 4 - i);
            int px = pointRight ? x + inset : x + (4 - inset);
            ctx.fill(px, y + i, px + 1, y + i + 1, color);
        }
    }

    /** Same › glyph rotated — open = 90° (down), collapsed = -90° (right). */
    public static void drawCollapseArrow(DrawContext ctx, int cx, int cy, int color, boolean collapsed) {
        drawCollapseArrow(ctx, cx, cy, color, collapsed ? -90f : 90f);
    }

    public static void drawCollapseArrow(DrawContext ctx, int cx, int cy, int color, float rotationDeg) {
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(cx, cy);
        matrices.rotate((float) Math.toRadians(rotationDeg));
        drawChevron(ctx, -2, -2, color, true);
        matrices.popMatrix();
    }

    public static void fillRowBackground(
            DrawContext ctx, int x1, int y1, int x2, int y2, int color, boolean roundBottom
    ) {
        if (roundBottom) {
            ModuleTypeWindow.fillBottomRoundedRect(ctx, x1, y1, x2, y2, GUIStyle.RADIUS_PANEL, color);
        } else {
            ctx.fill(x1, y1, x2, y2, color);
        }
    }

    public static void drawRoundedBadge(DrawContext ctx, int x1, int y1, int x2, int y2, int bg, int border) {
        int r = GUIStyle.RADIUS_BADGE;
        fillSmoothRoundedRect(ctx, x1, y1, x2, y2, r, border);
        fillSmoothRoundedRect(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), bg);
    }

    /** Horizontal ⋮ using three stacked dots at right side of row. */
    public static void drawMenuDots(DrawContext ctx, int rightX, int centerY, int color) {
        int dot = 2;
        int gap = 3;
        int cx = rightX - 3;
        for (int i = -1; i <= 1; i++) {
            int dy = centerY + i * gap;
            ctx.fill(cx - dot / 2, dy - dot / 2, cx - dot / 2 + dot, dy - dot / 2 + dot, color);
        }
    }

    /**
     * Draws a filled rounded rectangle with anti-aliased edges.
     */
    public static void fillSmoothRoundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        // Center body
        ctx.fill(x1, y1 + r, x2, y2 - r, color);

        // Corner scanlines with AA
        for (int y = 0; y < r; y++) {
            double dy = r - y - 0.5;
            double dx = Math.sqrt((double) r * r - dy * dy);
            double edgeX = r - dx;
            int fullInset = (int) Math.ceil(edgeX);
            double coverage = fullInset - edgeX;
            int edgeAlpha = (int) (baseAlpha * coverage);
            int edgeColor = rgb | (edgeAlpha << 24);

            // Interior scanlines (full color)
            ctx.fill(x1 + fullInset, y1 + y, x2 - fullInset, y1 + y + 1, color);
            ctx.fill(x1 + fullInset, y2 - y - 1, x2 - fullInset, y2 - y, color);

            // AA edge pixels (partial alpha)
            if (edgeAlpha > 0 && fullInset > 0) {
                ctx.fill(x1 + fullInset - 1, y1 + y, x1 + fullInset, y1 + y + 1, edgeColor);
                ctx.fill(x2 - fullInset, y1 + y, x2 - fullInset + 1, y1 + y + 1, edgeColor);
                ctx.fill(x1 + fullInset - 1, y2 - y - 1, x1 + fullInset, y2 - y, edgeColor);
                ctx.fill(x2 - fullInset, y2 - y - 1, x2 - fullInset + 1, y2 - y, edgeColor);
            }
        }
    }

    /**
     * Draws a filled rectangle with flat top corners and rounded bottom corners.
     */
    public static void fillBottomRoundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int r = Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }

        // Top flat + center body (y1 to y2-r, full width)
        ctx.fill(x1, y1, x2, y2 - r, color);

        // Bottom rounded corners
        for (int y = 0; y < r; y++) {
            double dy = r - y - 0.5;
            double dx = Math.sqrt((double) r * r - dy * dy);
            double edgeX = r - dx;
            int fullInset = (int) Math.ceil(edgeX);
            double coverage = fullInset - edgeX;
            int edgeAlpha = (int) (baseAlpha * coverage);
            int edgeColor = rgb | (edgeAlpha << 24);

            ctx.fill(x1 + fullInset, y2 - y - 1, x2 - fullInset, y2 - y, color);

            if (edgeAlpha > 0 && fullInset > 0) {
                ctx.fill(x1 + fullInset - 1, y2 - y - 1, x1 + fullInset, y2 - y, edgeColor);
                ctx.fill(x2 - fullInset, y2 - y - 1, x2 - fullInset + 1, y2 - y, edgeColor);
            }
        }
    }
}
