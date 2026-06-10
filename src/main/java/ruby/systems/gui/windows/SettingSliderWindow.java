package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.Value;
import ruby.systems.gui.GUIStyle;

public class SettingSliderWindow extends SettingWindow {

    private static final int INDENT = 14;

    private final Value<?> value;
    private final double min;
    private final double max;
    private final double step;
    private final boolean isInteger;
    private boolean dragging;

    public SettingSliderWindow(int x, int y, int width, DoubleValue value) {
        super(x, y, width, 44, value);
        this.value = value;
        this.min = value.min();
        this.max = value.max();
        this.step = value.step();
        this.isInteger = false;
        this.handleChildren = false;
    }

    public SettingSliderWindow(int x, int y, int width, IntegerValue value) {
        super(x, y, width, 44, value);
        this.value = value;
        this.min = value.min();
        this.max = value.max();
        this.step = 1;
        this.isInteger = true;
        this.handleChildren = false;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();

        this.drawText(style.bodyFont(), context, this.value.name(), INDENT, 6, 0xFF6A6567);

        String displayVal = this.isInteger
                ? String.valueOf(((IntegerValue) this.value).value())
                : String.format("%.1f", ((DoubleValue) this.value).value());
        int valW = style.monospaceFont().getWidth(displayVal);
        this.drawText(style.monospaceFont(), context, displayVal, w - INDENT - valW, 6, style.ruby());

        int trackLeft = INDENT;
        int trackRight = w - INDENT;
        int trackY = 30;
        int trackH = 4;
        ModuleTypeWindow.fillSmoothRoundedRect(context,
                trackLeft, trackY, trackRight, trackY + trackH, 2, style.trackOff());

        double normalized = getNormalized();
        int fillX = (int) (trackLeft + normalized * (trackRight - trackLeft));
        if (fillX > trackLeft) {
            ModuleTypeWindow.fillSmoothRoundedRect(context,
                    trackLeft, trackY, fillX, trackY + trackH, 2, style.ruby());
        }

        int thumbSize = 12;
        int thumbX = fillX - thumbSize / 2;
        int thumbY = trackY + trackH / 2 - thumbSize / 2;
        ModuleTypeWindow.fillSmoothRoundedRect(context,
                thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize, GUIStyle.RADIUS_PILL, style.ruby());
    }

    private double getNormalized() {
        double current = this.isInteger
                ? ((IntegerValue) this.value).value()
                : ((DoubleValue) this.value).value();
        if (this.max <= this.min) return 0;
        return Math.max(0, Math.min(1, (current - this.min) / (this.max - this.min)));
    }

    private void updateFromMouse(double mouseX) {
        int trackLeft = INDENT;
        int trackRight = this.getWidth() - INDENT;
        double normalized = (mouseX - trackLeft) / (trackRight - trackLeft);
        normalized = Math.max(0, Math.min(1, normalized));
        double newVal = this.min + normalized * (this.max - this.min);
        newVal = Math.round(newVal / this.step) * this.step;
        newVal = Math.max(this.min, Math.min(this.max, newVal));
        if (this.isInteger) {
            ((IntegerValue) this.value).setValue((int) Math.round(newVal));
        } else {
            ((DoubleValue) this.value).setValue(newVal);
        }
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (click.y() >= 22 && click.y() <= 44) {
            this.dragging = true;
            updateFromMouse(click.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(Click click, double deltaX, double deltaY) {
        if (this.dragging) {
            updateFromMouse(click.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseUp(Click click) {
        if (this.dragging) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean onFocusRemoved() {
        this.dragging = false;
        return true;
    }
}
