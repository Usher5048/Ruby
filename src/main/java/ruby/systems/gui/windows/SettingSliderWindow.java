package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.Value;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.Motion;

public class SettingSliderWindow extends SettingWindow {

    private final Value<?> slider;
    private final double min;
    private final double max;
    private final double step;
    private final boolean integer;
    private boolean dragging;

    public SettingSliderWindow(int x, int y, int width, DoubleValue value) {
        super(x, y, width, ROW, value);
        this.slider = value;
        this.min = value.min();
        this.max = value.max();
        this.step = value.step();
        this.integer = false;
        this.handleChildren = false;
    }

    public SettingSliderWindow(int x, int y, int width, IntegerValue value) {
        super(x, y, width, ROW, value);
        this.slider = value;
        this.min = value.min();
        this.max = value.max();
        this.step = 1;
        this.integer = true;
        this.handleChildren = false;
    }

    @Override
    public int restingHeight() {
        return this.value().visible() ? this.scaled(ROW) + 5 : 0;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        if (this.getHeight() <= 0) return;
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();
        this.clip(context);
        this.paintHover(context, mouseX, mouseY, dt);

        this.beginItem(context);
        w = this.designWidth();
        String display = this.display();
        int valW = style.labelFont().getWidth(display);
        int textY = (this.scaled(ROW) - style.labelFont().fontHeight) / 2;
        String name = this.fit(style.labelFont(), this.slider.name(), w - PAD * 2 - valW - 8);
        int label = Motion.mix(style.textMuted(), style.text(), this.hoverMix());
        style.labelFont().draw(context, name, PAD, textY, label);
        style.labelFont().draw(context, display, w - PAD - valW, textY, style.text());

        int trackL = PAD;
        int trackR = w - PAD;
        int trackY = this.scaled(ROW) - this.scaled(6) + 5;
        int trackH = Math.max(2, this.scaled(2));
        context.fill(trackL, trackY, trackR, trackY + trackH, style.trackOff());

        double normalized = this.normalized();
        int fillX = trackL + (int) (normalized * (trackR - trackL));
        if (fillX > trackL) {
            context.fill(trackL, trackY, fillX, trackY + trackH, style.ruby());
        }
        int thumb = Math.max(7, this.scaled(7));
        int thumbX = Math.clamp(fillX - thumb / 2, trackL, trackR - thumb);
        int thumbY = trackY + trackH / 2 - thumb / 2;
        ModuleTypeWindow.fillSmoothRoundedRect(context, thumbX, thumbY, thumbX + thumb, thumbY + thumb, thumb / 2, style.text());
        this.endItem(context);
        this.unclip(context);
    }

    private String display() {
        if (this.integer) return String.valueOf(((IntegerValue) this.slider).value());
        double current = ((DoubleValue) this.slider).value();
        if (this.step >= 1d) return String.format("%.0f", current);
        if (this.step >= 0.1d) return String.format("%.1f", current);
        return String.format("%.2f", current);
    }

    private double normalized() {
        double current = this.integer
                ? ((IntegerValue) this.slider).value()
                : ((DoubleValue) this.slider).value();
        if (this.max <= this.min) return 0;
        return Math.clamp((current - this.min) / (this.max - this.min), 0, 1);
    }

    private void updateFromMouse(double mouseX) {
        int trackL = PAD;
        int trackR = this.designWidth() - PAD;
        double span = trackR - trackL;
        if (span <= 0) return;
        double normalized = Math.clamp((mouseX - trackL) / span, 0, 1);
        double newVal = this.min + normalized * (this.max - this.min);
        if (this.step > 0) newVal = Math.round(newVal / this.step) * this.step;
        newVal = Math.clamp(newVal, this.min, this.max);
        if (this.integer) ((IntegerValue) this.slider).setValue((int) Math.round(newVal));
        else ((DoubleValue) this.slider).setValue(newVal);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (click.y() < 0 || click.y() >= this.getHeight()) return false;
        this.dragging = true;
        this.updateFromMouse(this.design(click.x()));
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (!this.dragging) return false;
        this.updateFromMouse(this.design(click.x()));
        return true;
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        if (!this.dragging) return false;
        this.dragging = false;
        return true;
    }

    @Override
    public boolean onFocusRemoved() {
        this.dragging = false;
        return true;
    }
}
