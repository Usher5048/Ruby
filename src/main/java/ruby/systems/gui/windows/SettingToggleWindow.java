package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ruby.RubyClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SettingToggleWindow extends SettingWindow {

    private static final int TRACK_W = 26;
    private static final int TRACK_H = 14;

    static int trackWidth() {
        return Math.max(1, Math.round(TRACK_W * ruby.systems.gui.ClientSettings.get().dropdownScale()));
    }

    static int trackHeight() {
        return Math.max(1, Math.round(TRACK_H * ruby.systems.gui.ClientSettings.get().dropdownScale()));
    }

    private static int knobSize() {
        return Math.max(1, Math.round(KNOB * ruby.systems.gui.ClientSettings.get().dropdownScale()));
    }
    private static final int INSET = 2;
    private static final int KNOB = 10;
    private static final float SLIDE = 0.2f;

    /** Hotswap skips static init, so this is created on first use. */
    private static Map<Long, Identifier> pills;

    private float shown;
    private float from;
    private float to;
    private float clock = 1f;
    private float stretch;
    private int motionVersion;

    public SettingToggleWindow(int x, int y, int width, BooleanValue value) {
        super(x, y, width, ROW, value);
        float on = value.value() ? 1f : 0f;
        this.shown = on;
        this.from = on;
        this.to = on;
        this.motionVersion = 1;
        this.handleChildren = false;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        if (this.getHeight() <= 0) return;
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();
        this.clip(context);
        this.paintHover(context, mouseX, mouseY, dt);

        boolean enabled = (boolean) this.value.value();
        this.advance(enabled, GuiTime.toSeconds(dt));

        this.beginItem(context);
        w = this.designWidth();
        int row = this.scaled(ROW);
        int trackW = trackWidth();
        int textY = (row - style.labelFont().fontHeight) / 2;
        int labelMax = w - PAD - trackW - 10 - PAD;
        String name = this.fit(style.labelFont(), this.value.name(), labelMax);
        int label = Motion.mix(style.textMuted(), style.text(), this.hoverMix());
        style.labelFont().draw(context, name, PAD, textY, label);

        int swX = w - PAD - trackW;
        int swY = (row - trackHeight()) / 2;
        String id = "set:" + System.identityHashCode(this);
        paintSwitch(context, style, swX, swY, this.shown, this.stretch, Press.sample(id), this.to < this.from);
        this.endItem(context);
        this.unclip(context);
    }

    private void advance(boolean enabled, float seconds) {
        if (this.motionVersion != 1) {
            this.motionVersion = 1;
            float on = enabled ? 1f : 0f;
            this.shown = on;
            this.from = on;
            this.to = on;
            this.clock = 1f;
            this.stretch = 0f;
        }
        float target = enabled ? 1f : 0f;
        if (!ruby.systems.gui.ClientSettings.get().animations()) {
            this.shown = target;
            this.from = target;
            this.to = target;
            this.clock = 1f;
            this.stretch = 0f;
            return;
        }
        if (target != this.to) {
            this.from = this.shown;
            this.to = target;
            this.clock = 0f;
        }
        float before = this.shown;
        this.clock = Math.min(1f, this.clock + seconds / SLIDE);
        this.shown = this.from + (this.to - this.from) * Motion.out(this.clock);
        float speed = seconds <= 0f ? 0f : Math.abs(this.shown - before) / seconds;
        float stretchTarget = Math.clamp(speed * 0.09f, 0f, 1f);
        this.stretch = Motion.follow(this.stretch, stretchTarget, seconds, 0.06f);
    }

    static void paintSwitch(
            GuiGraphicsExtractor context, GUIStyle style,
            int x, int y, float shown, float stretch, float press, boolean shrinkLeft
    ) {
        var pose = context.pose();
        pose.pushMatrix();
        int trackW = trackWidth();
        int trackH = trackHeight();
        int knob = knobSize();
        if (press >= 0.02f) {
            float cx = x + trackW / 2f;
            float cy = y + trackH / 2f;
            pose.translate(cx, cy);
            pose.scale(1f - 0.06f * press, 1f - 0.06f * press);
            pose.translate(-cx, -cy);
        }

        int edge = Motion.mix(style.borderPanel(), style.ruby(), shown);
        int fill = Motion.mix(style.trackOff(), style.ruby(), shown);
        blit(context, pill(trackW, trackH), x, y, trackW, trackH, edge);
        blit(context, pill(trackW - 2, trackH - 2), x + 1, y + 1, trackW - 2, trackH - 2, fill);

        float extra = stretch * 4f;
        int knobW = knob + Math.round(extra);
        float travel = trackW - INSET * 2 - knob;
        float knobX = x + INSET + travel * shown;
        if (shrinkLeft) knobX -= extra;
        float knobY = y + (trackH - knob) / 2f;
        pose.pushMatrix();
        pose.translate(knobX, knobY);
        blit(context, pill(knobW, knob), 0, 0, knobW, knob, 0xFFE8E4E5);
        pose.popMatrix();
        pose.popMatrix();
    }

    private static void blit(GuiGraphicsExtractor context, Identifier texture, int x, int y, int w, int h, int color) {
        if (texture == null || w <= 0 || h <= 0) return;
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, w, h, w, h, color);
    }

    private static Identifier pill(int w, int h) {
        if (w < 2 || h < 2) return null;
        if (SettingToggleWindow.pills == null) SettingToggleWindow.pills = new ConcurrentHashMap<>();
        long key = ((long) w << 32) | (h & 0xFFFFFFFFL);
        Identifier cached = SettingToggleWindow.pills.get(key);
        if (cached != null) return cached;

        int ss = 4;
        NativeImage image = new NativeImage(w, h, true);
        float radius = (h * ss) * 0.5f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float covered = 0f;
                for (int sy = 0; sy < ss; sy++) {
                    for (int sx = 0; sx < ss; sx++) {
                        float px = x * ss + sx + 0.5f;
                        float py = y * ss + sy + 0.5f;
                        float d = SettingToggleWindow.pillDistance(px, py, w * ss, h * ss, radius);
                        covered += Math.clamp(0.5f - d, 0f, 1f);
                    }
                }
                int alpha = Math.round(covered / (ss * ss) * 255f);
                image.setPixelABGR(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        String path = "toggle/pill-" + w + "x" + h;
        Identifier id = Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, path);
        RubyClient.client.getTextureManager().register(id, new DynamicTexture(() -> path, image));
        SettingToggleWindow.pills.put(key, id);
        return id;
    }

    private static float pillDistance(float px, float py, float w, float h, float radius) {
        float r = Math.min(radius, Math.min(w, h) * 0.5f);
        float qx = Math.abs(px - w * 0.5f) - w * 0.5f + r;
        float qy = Math.abs(py - h * 0.5f) - h * 0.5f + r;
        float ox = Math.max(qx, 0f);
        float oy = Math.max(qy, 0f);
        return (float) (Math.hypot(ox, oy) + Math.min(Math.max(qx, qy), 0f) - r);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (click.y() < 0 || click.y() >= this.getHeight()) return false;
        Press.down("set:" + System.identityHashCode(this));
        ((BooleanValue) this.value).setValue(!(boolean) this.value.value());
        return true;
    }
}
