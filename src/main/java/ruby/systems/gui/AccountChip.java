package ruby.systems.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import ruby.RubyClient;
import ruby.systems.gui.icons.Lucide;
import ruby.systems.gui.windows.ModuleTypeWindow;

/**
 * Accounts entry on a vanilla screen. Drawn in framebuffer pixels, same density as the click GUI,
 * with a GUI-space hit box that matches that size.
 */
public class AccountChip extends AbstractWidget {
    private static final int PHYS_H = 31;
    private static final int PAD = 14;

    private final Screen parent;
    private float hover;

    public AccountChip(int x, int y, Screen parent) {
        super(x, y, 1, 1, Component.literal("Accounts"));
        this.parent = parent;
        this.place();
    }

    public static AccountChip atTopRight(Screen screen) {
        return new AccountChip(0, 0, screen);
    }

    private int scale() {
        return Math.max(1, RubyClient.client.getWindow().getGuiScale());
    }

    private int physicalWidth(GUIStyle style) {
        return PAD + Lucide.UI + 8 + style.bodyFont().getWidth("Accounts") + PAD;
    }

    /** Keep the hit box on the visual after a hotswap, and when the GUI scale changes. */
    private void place() {
        GUIStyle style = GUIStyle.get();
        int scale = this.scale();
        int guiW = Math.max(1, (this.physicalWidth(style) + scale - 1) / scale);
        int guiH = Math.max(1, (PHYS_H + scale - 1) / scale);
        this.width = guiW;
        this.height = guiH;
        this.setX(this.parent.width - guiW - 6);
        this.setY(6);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        this.place();
        boolean inside = mouseX >= this.getX() && mouseY >= this.getY()
                && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
        this.hover = Motion.follow(this.hover, inside ? 1f : 0f, GuiTime.toSeconds(tickDelta), Motion.HOVER);

        int scale = this.scale();
        int x = this.getX() * scale;
        int y = this.getY() * scale;
        int x2 = (this.getX() + this.width) * scale;
        int y2 = (this.getY() + this.height) * scale;

        GUIStyle style = GUIStyle.get();
        int bg = Motion.mix(style.bgPanel(), style.bgHover(), this.hover);
        int color = Motion.mix(style.text(), style.textBright(), this.hover);
        var pose = context.pose();
        pose.pushMatrix();
        pose.scale(1f / scale, 1f / scale);
        ModuleTypeWindow.drawRoundedPanel(context, x, y, x2, y2, GUIStyle.RADIUS_ROW, bg, style.borderPanel());

        int textW = style.bodyFont().getWidth("Accounts");
        int contentW = Lucide.UI + 8 + textW;
        int contentX = x + (x2 - x - contentW) / 2;
        int iconY = y + (y2 - y - Lucide.UI) / 2;
        int textY = y + (y2 - y - style.bodyFont().fontHeight) / 2;
        Press.icon(context, "users", contentX, iconY, color, "accounts-open");
        Press.label(style.bodyFont(), context, "Accounts", contentX + Lucide.UI + 8, textY, color, "accounts-open");
        pose.popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @Override
    public void playDownSound(SoundManager manager) {}

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        Press.down("accounts-open");
        RubyClient.client.gui.setScreen(new AccountsScreen(this.parent));
    }
}
