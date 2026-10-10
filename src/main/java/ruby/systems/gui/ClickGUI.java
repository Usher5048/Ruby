package ruby.systems.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.windows.ClickGuiLayoutWindow;
import ruby.systems.gui.windows.Window;
import ruby.systems.gui.windows.WindowedScreen;

public class ClickGUI extends WindowedScreen {
    public static final String PANEL_ID = "ClickGUI";
    private static ClickGuiLayoutWindow sharedLayout;

    private float openProgress = 0f;
    private boolean closing = false;

    public ClickGUI() {
        super("Click GUI");
    }

    @Override
    protected void init() {
        super.init();
        this.closing = false;
        this.openProgress = 0f;

        if(ClickGUI.sharedLayout == null) ClickGUI.sharedLayout = new ClickGuiLayoutWindow();
        ClickGUI.sharedLayout.prepareForOpen();

        this.addWindow(ClickGUI.sharedLayout);
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int width = RubyClient.client.getWindow().getWidth();
        int height = RubyClient.client.getWindow().getHeight();

        float target = this.closing ? 0f : 1f;
        this.openProgress += (target - this.openProgress) * 0.35f;
        if (Math.abs(this.openProgress - target) < 0.005f) this.openProgress = target;

        if (this.closing && this.openProgress <= 0.01f) {
            RubyClient.client.setScreenAndShow(null);
            return;
        }

        int bgAlpha = (int) ((GUIStyle.get().overlayDim() >>> 24) * this.openProgress);
        context.fill(0, 0, width, height, bgAlpha << 24);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        float sf = RubyClient.client.getWindow().getGuiScale();

        context.pose().pushMatrix();
        context.pose().scale(1f / sf, 1f / sf);
        this.onRender(context, mouseX, mouseY);
        context.pose().popMatrix();

        float scale = this.openProgress;
        for (Window window : this.windows()) {
            context.pose().pushMatrix();

            float cx = window.getX() / sf + (window.getWidth() / sf) / 2f;
            float cy = window.getY() / sf + (window.getHeight() / sf) / 2f;

            context.pose().translate(cx, cy);
            context.pose().scale(scale, scale);
            context.pose().translate(-cx, -cy);

            window.extractRenderState(context, mouseX, mouseY, tickDelta);
            context.pose().popMatrix();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if(input.key() == InputConstants.KEY_RSHIFT) {
            this.onClose();
            return true;
        }
        if(super.keyPressed(input)) return true;
        return ClickGUI.sharedLayout != null && ClickGUI.sharedLayout.handleSearchKey(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (super.charTyped(input)) return true;
        return ClickGUI.sharedLayout != null && ClickGUI.sharedLayout.handleSearchChar(input);
    }

    @Override
    public void onClose() {
        if(this.closing) return;

        if(ClickGUI.sharedLayout != null) ClickGUI.sharedLayout.persistPosition();
        ConfigManager.saveState();

        this.closing = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
