package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import ruby.systems.config.EnumValue;
import ruby.systems.gui.GUIStyle;

public class SettingEnumWindow extends SettingWindow {

    private static final int INDENT = 14;

    public SettingEnumWindow(int x, int y, int width, EnumValue<?> value) {
        super(x, y, width, 26, value);
        this.handleChildren = false;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        GUIStyle style = GUIStyle.get();
        int h = this.getHeight();
        int w = this.getWidth();

        int textY = (h - style.bodyFont().fontHeight) / 2;
        this.drawText(style.bodyFont(), context, this.value.name(), INDENT, textY, 0xFF6A6567);

        String display = this.value.value().toString();
        int valW = style.monospaceFont().getWidth(display);
        int badgeW = valW + 16;
        int badgeH = style.monospaceFont().fontHeight + 4;
        int badgeX = w - INDENT - badgeW;
        int badgeY = (h - badgeH) / 2;

        ModuleTypeWindow.fillSmoothRoundedRect(context, badgeX, badgeY,
                badgeX + badgeW, badgeY + badgeH, GUIStyle.RADIUS_BADGE, style.bgHover());
        this.drawBorder(context, badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, style.border(), 1);

        this.drawText(style.monospaceFont(), context, display,
                badgeX + 8, badgeY + 2, style.ruby());
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        ((EnumValue<?>) this.value).cycle();
        return true;
    }
}
