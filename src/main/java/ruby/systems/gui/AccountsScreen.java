package ruby.systems.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;
import ruby.systems.gui.windows.AccountsPanelContent;
import ruby.systems.gui.windows.ModuleTypeWindow;
import ruby.systems.gui.windows.WindowedScreen;

public class AccountsScreen extends WindowedScreen {
    private static final int PANEL_W = 320;
    private static final int HEADER_H = 36;

    private final Screen parent;
    private AccountsPanelContent panel;

    public AccountsScreen(Screen parent) {
        super("Accounts");
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.layoutPanel();
    }

    private void layoutPanel() {
        float sf = (float) RubyClient.client.getWindow().getScaleFactor();
        int contentH = this.panel == null ? 120 : this.panel.getHeight();
        int totalH = HEADER_H + contentH;
        int x = Math.round((this.width * sf - PANEL_W) / 2f);
        int y = Math.round((this.height * sf - totalH) / 2f) + HEADER_H;
        if (this.panel == null) {
            this.panel = new AccountsPanelContent(x, y, PANEL_W);
            this.addWindow(this.panel);
        } else {
            this.panel.setPosition(x, y);
        }
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        GUIStyle style = GUIStyle.get();
        int width = RubyClient.client.getWindow().getWidth();
        int height = RubyClient.client.getWindow().getHeight();
        context.fill(0, 0, width, height, style.overlayDim());

        this.layoutPanel();

        float sf = (float) RubyClient.client.getWindow().getScaleFactor();
        int contentH = this.panel.getHeight();
        int totalH = HEADER_H + contentH;
        int px = Math.round((this.width * sf - PANEL_W) / 2f);
        int py = Math.round((this.height * sf - totalH) / 2f);
        int bottom = py + totalH;
        ModuleTypeWindow.drawRoundedPanel(context, px - 1, py, px + PANEL_W + 1, bottom,
                GUIStyle.RADIUS_PANEL, style.bgPanel(), style.borderPanel());
        style.logoFont().draw(context, "Accounts", px + 14, py + 8, style.ruby());
        style.bodyFont().draw(context, "Logged in as " + RubyClient.client.getSession().getUsername(),
                px + 14, py + HEADER_H - 12, style.textMuted());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            RubyClient.client.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(input);
    }
}
