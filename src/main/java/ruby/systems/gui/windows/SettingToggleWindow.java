package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.BooleanValue;
import ruby.systems.gui.GUIStyle;

public class SettingToggleWindow extends SettingWindow {

    private float knobProgress;

    public SettingToggleWindow(int x, int y, int width, BooleanValue value) {
        super(x, y, width, 26, value);
        this.knobProgress = value.value() ? 1f : 0f;
        this.handleChildren = false;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        GUIStyle style = GUIStyle.get();
        int h = this.getHeight();
        int w = this.getWidth();

        // Animate knob
        float target = (boolean) this.value.value() ? 1f : 0f;
        this.knobProgress += (target - this.knobProgress) * 0.2f;
        if (Math.abs(this.knobProgress - target) < 0.01f) this.knobProgress = target;

        // Label (left-aligned, indented 34px)
        int textY = (h - (int) this.getTextHeight(style.bodyFont())) / 2;
        this.drawText(style.bodyFont(), context, this.value.name(), 34, textY, 0xFF8B8B8B);

        // Toggle switch (26x14, right-aligned with 14px padding)
        int swW = 26, swH = 14;
        int swX = w - 14 - swW;
        int swY = (h - swH) / 2;

        // Track (pill shape) — color interpolated
        int trackColor = lerpColor(0xFF333333, 0xFFCC3344, this.knobProgress);
        ModuleTypeWindow.fillSmoothRoundedRect(context, swX, swY, swX + swW, swY + swH, 7, trackColor);

        // Knob (10x10 circle) — position interpolated
        int knobSize = 10;
        int knobTravel = swW - knobSize - 4;
        int knobX = swX + 2 + (int) (knobTravel * this.knobProgress);
        int knobY = swY + 2;
        ModuleTypeWindow.fillSmoothRoundedRect(context, knobX, knobY,
                knobX + knobSize, knobY + knobSize, 5, 0xFFFFFFFF);
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((int) (a1 + (a2 - a1) * t) << 24) |
               ((int) (r1 + (r2 - r1) * t) << 16) |
               ((int) (g1 + (g2 - g1) * t) << 8) |
               (int) (b1 + (b2 - b1) * t);
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        ((BooleanValue) this.value).setValue(!(boolean) this.value.value());
        return true;
    }
}
