package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.ThemeManager;

import java.awt.Color;
import java.util.List;

public class ThemePanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 26;
    private static final int LAST_ROW_EXTRA = 4;
    private static final int EXPANDED_EXTRA = 66;
    private static final int BTN_SECTION_H = 40;

    private int expandedIndex = -1;
    private int draggingHue = -1;
    private int draggingSv = -1;
    private float hue;
    private float saturation;
    private float brightness;
    private float contentAlpha = 1f;

    public ThemePanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    private int rowHeight(int index, int total) {
        return index == total - 1 ? ROW_H + ThemePanelContent.LAST_ROW_EXTRA : ROW_H;
    }

    private int colorRowTop(int colorIndex, List<ThemeManager.ThemeColor> colors) {
        int y = BTN_SECTION_H;
        for (int i = 0; i < colorIndex; i++) {
            y += this.rowHeight(i, colors.size());
            if (this.expandedIndex == i) y += EXPANDED_EXTRA;
        }
        return y;
    }

    @Override
    public int getHeight() {
        List<ThemeManager.ThemeColor> colors = ThemeManager.get().colors();
        int h = BTN_SECTION_H;
        for (int i = 0; i < colors.size(); i++) {
            h += this.rowHeight(i, colors.size());
            if (this.expandedIndex == i) h += EXPANDED_EXTRA;
        }
        this.height = h;
        return h;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        this.drawContent(context, mouseX, mouseY);
    }

    private void drawContent(DrawContext context, int mouseX, int mouseY) {
        GUIStyle style = GUIStyle.get();
        int innerR = this.getWidth() - INNER_L;
        int y = 0;
        List<ThemeManager.ThemeColor> colors = ThemeManager.get().colors();

        int btnY = y + 8;
        int btnH = 24;
        boolean resetHovered = mouseX >= PAD && mouseX < innerR - PAD
                && mouseY >= btnY && mouseY < btnY + btnH;
        ModuleTypeWindow.drawRoundedBadge(context, PAD, btnY, innerR - PAD, btnY + btnH,
                resetHovered ? style.rubyBg() : 0x00000000, style.border());
        this.drawTextInRect(style.bodyFont(), context, "Reset to Default", PAD, btnY,
                innerR - PAD, btnY + btnH,
                GUIStyle.withAlpha(resetHovered ? style.textBright() : style.ruby(), this.contentAlpha));
        context.fill(INNER_L, y + BTN_SECTION_H - 1, innerR, y + BTN_SECTION_H, style.borderSubtle());
        y += BTN_SECTION_H;

        for (int i = 0; i < colors.size(); i++) {
            boolean isLast = i == colors.size() - 1;
            y = this.drawColorRow(context, style, innerR, y, mouseX, mouseY, colors.get(i), i, isLast);
            if (this.expandedIndex == i) {
                y = this.drawExpandedPicker(context, style, innerR, y, mouseX, mouseY, colors.get(i), isLast);
            }
        }
    }

    private int drawColorRow(DrawContext context, GUIStyle style, int innerR, int y,
                             int mouseX, int mouseY, ThemeManager.ThemeColor color, int index, boolean isLast) {
        int rowH = this.rowHeight(index, ThemeManager.get().colors().size());
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        boolean expandedBelow = this.expandedIndex == index;

        if (isLast && !expandedBelow) {
            ModuleTypeWindow.fillBottomRoundedRect(context, INNER_L, y, innerR, y + rowH, GUIStyle.RADIUS_PANEL,
                    GUIStyle.withAlpha(style.bgPanel(), this.contentAlpha));
        }

        if (hovered) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha), isLast && !expandedBelow);
        }

        int textY = y + (rowH - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, color.name(), PAD, textY,
                GUIStyle.withAlpha(style.text(), this.contentAlpha));

        int swW = 28;
        int swH = 14;
        int swX = innerR - PAD - swW;
        int swY = y + (rowH - swH) / 2;
        ModuleTypeWindow.fillSmoothRoundedRect(context, swX, swY, swX + swW, swY + swH,
                GUIStyle.RADIUS_BADGE, GUIStyle.withAlpha(color.get(), this.contentAlpha));
        this.drawBorder(context, swX, swY, swX + swW, swY + swH, style.border(), 1);

        if (!isLast || expandedBelow) {
            context.fill(INNER_L, y + rowH - 1, innerR, y + rowH, style.borderSubtle());
        }

        return y + rowH;
    }

    private int drawExpandedPicker(DrawContext context, GUIStyle style, int innerR, int y,
                                   int mouseX, int mouseY, ThemeManager.ThemeColor color, boolean isLast) {
        if (isLast) {
            ModuleTypeWindow.fillBottomRoundedRect(context, INNER_L, y, innerR, y + EXPANDED_EXTRA,
                    GUIStyle.RADIUS_PANEL, GUIStyle.withAlpha(style.bgPanel(), this.contentAlpha));
        }

        int hueY = y + 6;
        int hueH = 10;
        int trackL = PAD;
        int trackR = innerR - PAD;
        this.drawHueBar(context, trackL, hueY, trackR, hueY + hueH);

        int hueThumbX = trackL + Math.round(this.hue * (trackR - trackL));
        ModuleTypeWindow.fillSmoothRoundedRect(context, hueThumbX - 3, hueY - 2,
                hueThumbX + 3, hueY + hueH + 2, GUIStyle.RADIUS_PILL, 0xFFE8E4E5);

        int svY = hueY + hueH + 8;
        int svH = 36;
        this.drawSvField(context, trackL, svY, trackR, svY + svH);

        int svW = trackR - trackL;
        int thumbX = trackL + Math.round(this.saturation * svW);
        int thumbY = svY + Math.round((1f - this.brightness) * svH);
        ModuleTypeWindow.fillSmoothRoundedRect(context, thumbX - 3, thumbY - 3,
                thumbX + 3, thumbY + 3, GUIStyle.RADIUS_PILL, 0xFFE8E4E5);
        context.fill(thumbX - 1, thumbY - 1, thumbX + 1, thumbY + 1, color.get());

        return y + EXPANDED_EXTRA;
    }

    private void drawHueBar(DrawContext context, int left, int top, int right, int bottom) {
        int width = right - left;
        if (width <= 0) return;
        for (int i = 0; i < width; i++) {
            int rgb = Color.HSBtoRGB(i / (float) width, 1f, 1f);
            context.fill(left + i, top, left + i + 1, bottom, 0xFF000000 | rgb);
        }
    }

    private void drawSvField(DrawContext context, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        if (width <= 0 || height <= 0) return;

        for (int py = 0; py < height; py += 2) {
            float v = 1f - py / (float) height;
            for (int px = 0; px < width; px += 2) {
                float s = px / (float) width;
                int rgb = Color.HSBtoRGB(this.hue, s, v);
                context.fill(left + px, top + py, left + Math.min(px + 2, width), top + Math.min(py + 2, height),
                        0xFF000000 | rgb);
            }
        }
        this.drawBorder(context, left, top, right, bottom, GUIStyle.get().border(), 1);
    }

    private void syncHsb(ThemeManager.ThemeColor color) {
        float[] hsb = Color.RGBtoHSB((color.get() >> 16) & 0xFF, (color.get() >> 8) & 0xFF, color.get() & 0xFF, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    private void applyHsb(ThemeManager.ThemeColor color) {
        int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        int alpha = color.get() & 0xFF000000;
        ThemeManager.get().setColor(color, alpha | (rgb & 0xFFFFFF));
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        int innerR = this.getWidth() - INNER_L;
        int y = 0;
        List<ThemeManager.ThemeColor> colors = ThemeManager.get().colors();

        if (click.y() >= y + 8 && click.y() < y + 32 && click.x() >= PAD && click.x() < innerR - PAD) {
            ThemeManager.get().resetDefaults();
            this.expandedIndex = -1;
            return true;
        }
        y += BTN_SECTION_H;

        for (int i = 0; i < colors.size(); i++) {
            ThemeManager.ThemeColor color = colors.get(i);
            int rowH = this.rowHeight(i, colors.size());
            if (click.y() >= y && click.y() < y + rowH) {
                if (this.expandedIndex == i) {
                    this.expandedIndex = -1;
                } else {
                    this.expandedIndex = i;
                    this.syncHsb(color);
                }
                return true;
            }
            y += rowH;
            if (this.expandedIndex == i) {
                int hueY = y + 6;
                if (click.y() >= hueY - 2 && click.y() <= hueY + 12) {
                    this.draggingHue = i;
                    this.updateHueFromMouse(click.x(), color);
                    return true;
                }
                int svY = hueY + 10 + 8;
                if (click.y() >= svY && click.y() <= svY + 36 && click.x() >= PAD && click.x() <= innerR - PAD) {
                    this.draggingSv = i;
                    this.updateSvFromMouse(click.x(), click.y(), color);
                    return true;
                }
                y += EXPANDED_EXTRA;
            }
        }

        return false;
    }

    private void updateHueFromMouse(double mouseX, ThemeManager.ThemeColor color) {
        int trackL = PAD;
        int trackR = this.getWidth() - INNER_L - PAD;
        float norm = (float) ((mouseX - trackL) / (trackR - trackL));
        this.hue = Math.max(0f, Math.min(1f, norm));
        this.applyHsb(color);
    }

    private void updateSvFromMouse(double mouseX, double mouseY, ThemeManager.ThemeColor color) {
        int trackL = PAD;
        int trackR = this.getWidth() - INNER_L - PAD;
        int baseY = this.colorRowTop(this.expandedIndex, ThemeManager.get().colors())
                + this.rowHeight(this.expandedIndex, ThemeManager.get().colors().size()) + 6 + 10 + 8;
        int svH = 36;
        float s = (float) ((mouseX - trackL) / (trackR - trackL));
        float v = 1f - (float) ((mouseY - baseY) / svH);
        this.saturation = Math.max(0f, Math.min(1f, s));
        this.brightness = Math.max(0f, Math.min(1f, v));
        this.applyHsb(color);
    }

    @Override
    public boolean onMouseDragged(Click click, double deltaX, double deltaY) {
        if (this.draggingHue >= 0) {
            this.updateHueFromMouse(click.x(), ThemeManager.get().colors().get(this.draggingHue));
            return true;
        }
        if (this.draggingSv >= 0) {
            this.updateSvFromMouse(click.x(), click.y(), ThemeManager.get().colors().get(this.draggingSv));
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseUp(Click click) {
        if (this.draggingHue >= 0 || this.draggingSv >= 0) {
            this.draggingHue = -1;
            this.draggingSv = -1;
            return true;
        }
        return false;
    }
}
