package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.BooleanValue;
import ruby.systems.gui.GUIStyle;

public class SettingToggleWindow extends SettingWindow {

    private static final int INDENT = 14;
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

        float target = (boolean) this.value.value() ? 1f : 0f;
        this.knobProgress += (target - this.knobProgress) * 0.2f;
        if (Math.abs(this.knobProgress - target) < 0.01f) this.knobProgress = target;

        int textY = (h - style.bodyFont().fontHeight) / 2;
        this.drawText(style.bodyFont(), context, this.value.name(), INDENT, textY, 0xFF6A6567);

        int swW = 32, swH = 16;
        int swX = w - INDENT - swW;
        int swY = (h - swH) / 2;

        int trackRadius = swH / 2;
        int trackColor = lerpColor(style.trackOff(), style.ruby(), this.knobProgress);
        ModuleTypeWindow.fillSmoothRoundedRect(context, swX, swY, swX + swW, swY + swH, trackRadius, trackColor);

        int knobSize = 12;
        int knobRadius = knobSize / 2;
        int knobTravel = swW - knobSize - 4;
        int knobX = swX + 2 + (int) (knobTravel * this.knobProgress);
        int knobY = swY + 2;
        int knobColor = this.knobProgress > 0.5f ? 0xFFE8E4E5 : 0xFFCCCCCC;
        ModuleTypeWindow.fillSmoothRoundedRect(context, knobX, knobY,
                knobX + knobSize, knobY + knobSize, knobRadius, knobColor);
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
