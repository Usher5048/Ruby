package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.ColorValue;
import ruby.systems.gui.GUIStyle;

import java.awt.Color;

public class SettingColorPickerWindow extends SettingWindow {
    private static final int INDENT = 14;
    private static final int COLLAPSED_H = 26;
    private static final int EXPANDED_H = 94;

    private final ColorValue color;
    private boolean expanded;
    private boolean draggingHue;
    private boolean draggingSv;
    private float hue;
    private float saturation;
    private float brightness;

    public SettingColorPickerWindow(int x, int y, int width, ColorValue value) {
        super(x, y, width, COLLAPSED_H, value);
        this.color = value;
        this.handleChildren = false;
        this.syncFromColor();
    }

    private void syncFromColor() {
        float[] hsb = this.color.hsb();
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    public int getHeight() {
        if (!this.color.visible()) {
            this.height = 0;
            return 0;
        }
        this.height = this.expanded ? EXPANDED_H : COLLAPSED_H;
        return this.height;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        if (!this.color.visible()) return;
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();
        int textY = (COLLAPSED_H - style.bodyFont().fontHeight) / 2;
        this.drawText(style.bodyFont(), context, this.color.name(), INDENT, textY, 0xFF6A6567);

        int swW = 28;
        int swH = 14;
        int swX = w - INDENT - swW;
        int swY = (COLLAPSED_H - swH) / 2;
        ModuleTypeWindow.fillSmoothRoundedRect(context, swX, swY, swX + swW, swY + swH, GUIStyle.RADIUS_BADGE,
                this.color.opaque());
        this.drawBorder(context, swX, swY, swX + swW, swY + swH, style.border(), 1);

        if (!this.expanded) return;

        int hueY = COLLAPSED_H + 8;
        int hueH = 10;
        int trackL = INDENT;
        int trackR = w - INDENT;
        this.drawHueBar(context, trackL, hueY, trackR, hueY + hueH);

        float hueNorm = this.hue;
        int hueThumbX = trackL + Math.round(hueNorm * (trackR - trackL));
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
        context.fill(thumbX - 1, thumbY - 1, thumbX + 1, thumbY + 1, this.color.opaque());
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

    private void applyHsb() {
        this.color.setHsb(this.hue, this.saturation, this.brightness);
    }

    private void updateHueFromMouse(double mouseX) {
        int trackL = INDENT;
        int trackR = this.getWidth() - INDENT;
        float norm = (float) ((mouseX - trackL) / (trackR - trackL));
        this.hue = Math.max(0f, Math.min(1f, norm));
        this.applyHsb();
    }

    private void updateSvFromMouse(double mouseX, double mouseY) {
        int trackL = INDENT;
        int trackR = this.getWidth() - INDENT;
        int svY = COLLAPSED_H + 8 + 10 + 8;
        int svH = 36;
        float s = (float) ((mouseX - trackL) / (trackR - trackL));
        float v = 1f - (float) ((mouseY - svY) / svH);
        this.saturation = Math.max(0f, Math.min(1f, s));
        this.brightness = Math.max(0f, Math.min(1f, v));
        this.applyHsb();
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        int w = this.getWidth();
        int swX = w - INDENT - 28;
        if (click.y() < COLLAPSED_H && click.x() >= INDENT) {
            if (click.x() >= swX) {
                this.expanded = !this.expanded;
                if (this.expanded) this.syncFromColor();
                return true;
            }
            this.expanded = !this.expanded;
            if (this.expanded) this.syncFromColor();
            return true;
        }

        if (!this.expanded) return false;

        int hueY = COLLAPSED_H + 8;
        if (click.y() >= hueY - 2 && click.y() <= hueY + 12) {
            this.draggingHue = true;
            this.updateHueFromMouse(click.x());
            return true;
        }

        int svY = hueY + 10 + 8;
        if (click.y() >= svY && click.y() <= svY + 36 && click.x() >= INDENT && click.x() <= w - INDENT) {
            this.draggingSv = true;
            this.updateSvFromMouse(click.x(), click.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean onMouseDragged(Click click, double deltaX, double deltaY) {
        if (this.draggingHue) {
            this.updateHueFromMouse(click.x());
            return true;
        }
        if (this.draggingSv) {
            this.updateSvFromMouse(click.x(), click.y());
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseUp(Click click) {
        if (this.draggingHue || this.draggingSv) {
            this.draggingHue = false;
            this.draggingSv = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean onFocusRemoved() {
        this.draggingHue = false;
        this.draggingSv = false;
        return true;
    }
}
