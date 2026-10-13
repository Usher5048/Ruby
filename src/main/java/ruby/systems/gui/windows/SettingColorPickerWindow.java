package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ruby.RubyClient;
import ruby.systems.config.ColorValue;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.Motion;
import ruby.systems.gui.icons.Lucide;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class SettingColorPickerWindow extends SettingWindow {
    private static final int HUE_GAP = 6;
    private static final int HUE_H = 8;
    private static final int FIELD_GAP = 8;
    private static final int FIELD_H = 44;
    private static final int FIELD_RADIUS = 6;
    private static final int BOTTOM = 8;

    private final ColorValue color;
    private boolean expanded;
    private float expandProgress;
    private boolean draggingHue;
    private boolean draggingSv;
    private float hue;
    private float saturation;
    private float brightness;

    private static DynamicTexture hueTexture;
    private static Identifier hueId;
    private static int hueTexW;
    private static int hueTexH;

    private DynamicTexture fieldTexture;
    private Identifier fieldId;
    private int fieldHue = -1;
    private int shown = -1;
    private int fieldTexW;
    private int fieldTexH;

    public SettingColorPickerWindow(int x, int y, int width, ColorValue value) {
        super(x, y, width, ROW, value);
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

    private int hueTop() {
        return this.scaled(ROW + HUE_GAP);
    }

    private int hueH() {
        return this.scaled(HUE_H);
    }

    private int fieldH() {
        return this.scaled(FIELD_H);
    }

    private int fieldTop() {
        return this.hueTop() + this.hueH() + this.scaled(FIELD_GAP);
    }

    private int expandedHeight() {
        return this.fieldTop() + this.fieldH() + this.scaled(BOTTOM);
    }

    @Override
    public int layoutStamp() {
        int stamp = super.layoutStamp();
        stamp = stamp * 31 + (this.expanded ? 1 : 0);
        stamp = stamp * 31 + Float.floatToIntBits(this.expandProgress);
        stamp = stamp * 31 + (this.color.visible() ? 1 : 0);
        return stamp;
    }

    @Override
    public int restingHeight() {
        if (!this.color.visible()) return 0;
        float eased = Motion.standard(this.expandProgress);
        int header = this.scaled(ROW);
        int full = this.expandedHeight();
        return header + Math.round((full - header) * eased);
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        int clip = this.getHeight();
        if (clip <= 0 || !this.color.visible()) return;
        this.clip(context);
        this.paintHover(context, mouseX, mouseY, dt);
        float dir = this.expanded ? 1f : -1f;
        this.expandProgress = Math.clamp(this.expandProgress + dir * ruby.systems.gui.ClientSettings.expandStep(dt), 0f, 1f);

        int now = this.color.opaque();
        if (now != this.shown && !this.draggingHue && !this.draggingSv) {
            this.shown = now;
            this.syncFromColor();
        }

        GUIStyle style = GUIStyle.get();
        this.beginItem(context);
        int w = this.designWidth();
        this.drawHeader(context, style, w);

        if (this.expandProgress > 0.01f && clip > this.scaled(ROW)) {
            int trackL = PAD;
            int trackR = w - PAD;
            int trackW = trackR - trackL;
            this.blit(context, this.hueTexture(trackW, this.hueH()), trackL, this.hueTop(), trackW, this.hueH());
            int hueX = trackL + Math.round(this.hue * trackW);
            this.drawThumb(context, hueX, this.hueTop() + this.hueH() / 2, this.color.opaque());

            int fieldY = this.fieldTop();
            this.blit(context, this.fieldTexture(trackW, this.fieldH()), trackL, fieldY, trackW, this.fieldH());
            int thumbX = trackL + Math.round(this.saturation * trackW);
            int thumbY = fieldY + Math.round((1f - this.brightness) * this.fieldH());
            this.drawThumb(context, thumbX, thumbY, this.color.opaque());
        }
        this.endItem(context);
        this.unclip(context);
    }

    private void drawHeader(GuiGraphicsExtractor context, GUIStyle style, int w) {
        int chevron = Lucide.UI;
        int drop = 12;
        int chevronX = w - PAD - chevron;
        int dropX = chevronX - 8 - drop;

        int row = this.scaled(ROW);
        int textY = (row - style.labelFont().fontHeight) / 2;
        String name = this.fit(style.labelFont(), this.color.name(), dropX - 8 - PAD);
        int label = Motion.mix(style.textMuted(), style.text(), this.hoverMix());
        style.labelFont().draw(context, name, PAD, textY, label);

        Lucide.draw(context, "droplet", dropX, (row - drop) / 2, drop, this.color.opaque());

        Lucide.drawCentered(context, "chevron-right", chevronX + chevron / 2, row / 2, chevron,
                style.textMuted(), this.expandProgress * 90f);
    }

    private void drawThumb(GuiGraphicsExtractor context, int cx, int cy, int color) {
        int outer = 12;
        int inner = 6;
        ModuleTypeWindow.fillSmoothRoundedRect(context, cx - outer / 2, cy - outer / 2,
                cx + outer / 2, cy + outer / 2, outer / 2, 0xFFE8E4E5);
        ModuleTypeWindow.fillSmoothRoundedRect(context, cx - inner / 2, cy - inner / 2,
                cx + inner / 2, cy + inner / 2, inner / 2, color);
    }

    private void blit(GuiGraphicsExtractor context, Identifier texture, int x, int y, int w, int h) {
        if (texture == null || w <= 0 || h <= 0) return;
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, w, h, w, h, 0xFFFFFFFF);
    }

    private Identifier hueTexture(int w, int h) {
        if (w <= 0 || h <= 0) return null;
        if (hueTexture != null && hueId != null && hueTexW == w && hueTexH == h) return hueId;
        if (hueTexture != null) hueTexture.close();
        hueTexture = new DynamicTexture(() -> "ruby-hue", w, h, true);
        hueTexW = w;
        hueTexH = h;
        NativeImage pixels = hueTexture.getPixels();
        int radius = h / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int inset = SettingColorPickerWindow.pillInset(x, w, radius);
                int rgb = Color.HSBtoRGB(w <= 1 ? 0f : x / (float) (w - 1), 1f, 1f);
                int alpha = y >= inset && y < h - inset ? 0xFF : 0;
                pixels.setPixelABGR(x, y, SettingColorPickerWindow.abgr(rgb, alpha));
            }
        }
        hueTexture.upload();
        hueId = Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, "color/hue");
        RubyClient.client.getTextureManager().register(hueId, hueTexture);
        return hueId;
    }

    private Identifier fieldTexture(int w, int h) {
        if (w <= 0 || h <= 0) return null;
        int hueKey = Math.round(this.hue * 1024f);
        if (this.fieldTexture != null && this.fieldId != null
                && this.fieldHue == hueKey && this.fieldTexW == w && this.fieldTexH == h) {
            return this.fieldId;
        }
        if (this.fieldTexture == null || this.fieldTexW != w || this.fieldTexH != h) {
            if (this.fieldTexture != null) this.fieldTexture.close();
            this.fieldTexture = new DynamicTexture(() -> "ruby-color-field", w, h, true);
            this.fieldTexW = w;
            this.fieldTexH = h;
            this.fieldId = Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, "color/field-" + System.identityHashCode(this));
            RubyClient.client.getTextureManager().register(this.fieldId, this.fieldTexture);
        }
        NativeImage pixels = this.fieldTexture.getPixels();
        for (int y = 0; y < h; y++) {
            float v = 1f - (y + 0.5f) / h;
            for (int x = 0; x < w; x++) {
                int alpha = SettingColorPickerWindow.insideRounded(x, y, w, h, FIELD_RADIUS) ? 0xFF : 0;
                float s = (x + 0.5f) / w;
                int rgb = Color.HSBtoRGB(this.hue, s, v);
                pixels.setPixelABGR(x, y, SettingColorPickerWindow.abgr(rgb, alpha));
            }
        }
        this.fieldTexture.upload();
        this.fieldHue = hueKey;
        return this.fieldId;
    }

    private static int abgr(int argb, int alpha) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (alpha << 24) | (b << 16) | (g << 8) | r;
    }

    private static int pillInset(int x, int width, int radius) {
        int fromEdge = Math.min(x, width - 1 - x);
        if (fromEdge >= radius) return 0;
        double dx = radius - fromEdge - 0.5;
        double inside = radius * (double) radius - dx * dx;
        if (inside <= 0d) return radius;
        return Math.max(0, (int) Math.floor(radius - Math.sqrt(inside)));
    }

    private static boolean insideRounded(int x, int y, int width, int height, int radius) {
        int cx = x < radius ? radius : (x >= width - radius ? width - radius : x);
        int cy = y < radius ? radius : (y >= height - radius ? height - radius : y);
        if (cx == x && cy == y) return true;
        double dx = x + 0.5 - cx;
        double dy = y + 0.5 - cy;
        return dx * dx + dy * dy <= radius * (double) radius;
    }

    private void applyHsb() {
        this.color.setHsb(this.hue, this.saturation, this.brightness);
    }

    private void updateHueFromMouse(double mouseX) {
        int trackL = PAD;
        int trackR = this.designWidth() - PAD;
        float span = trackR - trackL;
        if (span <= 0f) return;
        this.hue = Math.clamp((float) ((mouseX - trackL) / span), 0f, 1f);
        this.applyHsb();
    }

    private void updateSvFromMouse(double mouseX, double mouseY) {
        int trackL = PAD;
        int trackR = this.designWidth() - PAD;
        float span = trackR - trackL;
        if (span <= 0f || this.fieldH() <= 0) return;
        this.saturation = Math.clamp((float) ((mouseX - trackL) / span), 0f, 1f);
        this.brightness = Math.clamp(1f - (float) ((mouseY - this.fieldTop()) / this.fieldH()), 0f, 1f);
        this.applyHsb();
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (click.y() < 0 || click.y() >= this.getHeight()) return false;
        double x = this.design(click.x());
        double y = this.design(click.y());

        if (y < this.scaled(ROW)) {
            this.expanded = !this.expanded;
            if (this.expanded) this.syncFromColor();
            return true;
        }

        if (this.expandProgress < 0.5f) return false;

        int hueY = this.hueTop();
        if (y >= hueY - 4 && y <= hueY + this.hueH() + 4) {
            this.draggingHue = true;
            this.updateHueFromMouse(x);
            return true;
        }

        int fieldY = this.fieldTop();
        if (y >= fieldY && y <= fieldY + this.fieldH()
                && x >= PAD && x <= this.designWidth() - PAD) {
            this.draggingSv = true;
            this.updateSvFromMouse(x, y);
            return true;
        }

        return false;
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (this.draggingHue) {
            this.updateHueFromMouse(this.design(click.x()));
            return true;
        }
        if (this.draggingSv) {
            this.updateSvFromMouse(this.design(click.x()), this.design(click.y()));
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        if (!this.draggingHue && !this.draggingSv) return false;
        this.draggingHue = false;
        this.draggingSv = false;
        return true;
    }

    @Override
    public boolean onFocusRemoved() {
        this.draggingHue = false;
        this.draggingSv = false;
        return true;
    }
}
