package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import ruby.systems.config.EnumValue;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.Motion;

public class SettingEnumWindow extends SettingWindow {

    public SettingEnumWindow(int x, int y, int width, EnumValue<?> value) {
        super(x, y, width, ROW, value);
        this.handleChildren = false;
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
        String display = this.value.value().toString();
        int textW = style.labelFont().getWidth(display);
        int valueX = w - PAD - textW;
        int textY = (this.scaled(ROW) - style.labelFont().fontHeight) / 2;
        String name = this.fit(style.labelFont(), this.value.name(), valueX - 8 - PAD);
        int label = Motion.mix(style.textMuted(), style.text(), this.hoverMix());
        style.labelFont().draw(context, name, PAD, textY, label);
        style.labelFont().draw(context, display, valueX, textY, style.text());
        this.endItem(context);
        this.unclip(context);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.y() < 0 || click.y() >= this.getHeight()) return false;
        EnumValue<?> value = (EnumValue<?>) this.value;
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            value.cycle();
            return true;
        }
        if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            value.cycleBack();
            return true;
        }
        return false;
    }
}
