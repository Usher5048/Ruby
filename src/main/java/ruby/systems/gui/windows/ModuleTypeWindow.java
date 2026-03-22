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
