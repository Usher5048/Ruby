package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.EnumValue;
import ruby.systems.gui.GUIStyle;

public class SettingEnumWindow extends Window {

    private final EnumValue<?> value;

    public SettingEnumWindow(int x, int y, int width, EnumValue<?> value) {
        super(x, y, width, 26);
        this.value = value;
        this.handleChildren = false;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        GUIStyle style = GUIStyle.get();
        int h = this.getHeight();
        int w = this.getWidth();

        // Label (left-aligned, indented 34px)
        int textY = (h - (int) this.getTextHeight(style.bodyFont())) / 2;
        this.drawText(style.bodyFont(), context, this.value.name(), 34, textY, 0xFF8B8B8B);

        // Value badge (right-aligned)
        String display = this.value.value().toString();
        int valW = (int) this.getTextWidth(style.monospaceFont(), display);
        int badgeW = valW + 12;
        int badgeH = (int) this.getTextHeight(style.monospaceFont()) + 4;
        int badgeX = w - 14 - badgeW;
        int badgeY = (h - badgeH) / 2;

        ModuleTypeWindow.fillSmoothRoundedRect(context, badgeX, badgeY,
                badgeX + badgeW, badgeY + badgeH, 4, 0x1AFFFFFF);

        this.drawText(style.monospaceFont(), context, display,
                badgeX + 6, badgeY + 2, 0xFFCC3344);
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        this.value.cycle();
        return true;
    }
}
