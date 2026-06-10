package ruby.systems.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.windows.ClickGuiLayoutWindow;
import ruby.systems.gui.windows.Window;
import ruby.systems.gui.windows.WindowedScreen;

public class ClickGUI extends WindowedScreen {

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
        if (ClickGUI.sharedLayout == null) {
            ClickGUI.sharedLayout = new ClickGuiLayoutWindow();
        }
        ClickGUI.sharedLayout.prepareForOpen();
        this.addWindow(ClickGUI.sharedLayout);
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        int width = RubyClient.client.getWindow().getWidth();
        int height = RubyClient.client.getWindow().getHeight();

        float target = this.closing ? 0f : 1f;
        this.openProgress += (target - this.openProgress) * 0.35f;
        if (Math.abs(this.openProgress - target) < 0.005f) this.openProgress = target;

        if (this.closing && this.openProgress <= 0.01f) {
            RubyClient.client.setScreen(null);
            return;
        }

        int bgAlpha = (int) ((GUIStyle.get().overlayDim() >>> 24) * this.openProgress);
        context.fill(0, 0, width, height, bgAlpha << 24);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        float sf = RubyClient.client.getWindow().getScaleFactor();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(1f / sf, 1f / sf);
        this.onRender(context, mouseX, mouseY);
        context.getMatrices().popMatrix();

        float scale = this.openProgress;
        for (Window window : this.windows()) {
            context.getMatrices().pushMatrix();

            float cx = window.getX() / sf + (window.getWidth() / sf) / 2f;
            float cy = window.getY() / sf + (window.getHeight() / sf) / 2f;

            context.getMatrices().translate(cx, cy);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-cx, -cy);

            window.render(context, mouseX, mouseY, tickDelta);
            context.getMatrices().popMatrix();
        }
    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        if (this.closing) return;
        if (ClickGUI.sharedLayout != null) {
            ClickGUI.sharedLayout.persistPosition();
        }
        ConfigManager.saveState();
        this.closing = true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
