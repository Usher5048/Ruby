package ruby.systems.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.windows.CategorySidebarWindow;
import ruby.systems.gui.windows.ClickGuiLayoutWindow;
import ruby.systems.gui.windows.Window;
import ruby.systems.gui.windows.WindowedScreen;

public class ClickGUI extends WindowedScreen {
    private static ClickGuiLayoutWindow sharedLayout;

    /** Eased visibility. 0 is hidden, 1 is fully open. */
    private float shown = 0f;
    private float animFrom = 0f;
    private float animTo = 1f;
    private long animStart;
    private boolean opening = false;
    private boolean closing = false;
    private boolean saved = false;
    /**
     * Multiplies fill and blit alpha while this screen draws. Stays at 1 for the HUD
     * and every other screen.
     */
    public static float opacity = 1f;
    /** Ignore one grab-warp look delta after the GUI returns to the game. */
    private static long dropWarpUntil;

    public static int applyOpacity(int color) {
        float opacity = ClickGUI.opacity;
        if (opacity >= 0.999f) return color;
        int alpha = Math.round((color >>> 24) * opacity);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public ClickGUI() {
        super("Click GUI");
    }

    @Override
    protected void init() {
        super.init();
        if (!this.opening) {
            this.opening = true;
            this.closing = false;
            this.play(1f);
            if (ClientSettings.get().perspective()) PerspectiveBoard.reseat();
        }

        if(ClickGUI.sharedLayout == null) ClickGUI.sharedLayout = new ClickGuiLayoutWindow();
        ClickGUI.sharedLayout.prepareForOpen();

        this.addWindow(ClickGUI.sharedLayout);
    }

    /** Skip the dirt menu background. It covers the world at full strength on the first frame. */
    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        RubyClient.client.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        this.advance();
        boolean perspective = ClientSettings.get().perspective();
        int dim = GUIStyle.get().overlayDim();
        int dimAlpha = perspective ? 0 : Math.round(((dim >>> 24) & 0xFF) * this.shown);
        // The fill is already faded. Leave opacity at 1 so the blit mixin does not fade it again.
        ClickGUI.opacity = 1f;
        if (dimAlpha > 0) context.fill(0, 0, this.width, this.height, (dimAlpha << 24) | (dim & 0x00FFFFFF));
        ClickGUI.opacity = this.shown;
        boolean zoomed = perspective && PerspectiveBoard.supersample() > 1;
        if (zoomed) {
            context.pose().pushMatrix();
            float zoom = PerspectiveBoard.supersample();
            context.pose().scale(zoom, zoom);
        }
        try {
            float rise = ClientSettings.get().animations() ? Motion.RISE * (1f - this.shown) : 0f;
            var windows = this.windows();
            for (int i = 0, n = windows.size(); i < n; i++) {
                Window window = windows.get(i);
                context.pose().pushMatrix();
                if (rise != 0f) context.pose().translate(0f, rise);
                window.extractRenderState(context, mouseX, mouseY, tickDelta);
                context.pose().popMatrix();
            }
        } finally {
            if (zoomed) context.pose().popMatrix();
            ClickGUI.opacity = 1f;
        }
    }

    private MouseButtonEvent unmap(MouseButtonEvent click) {
        double[] point = PerspectiveBoard.toGui(click.x(), click.y());
        return new MouseButtonEvent(point[0], point[1], click.buttonInfo());
    }

    private double[] unmap(double x, double y) {
        return PerspectiveBoard.toGui(x, y);
    }

    /** Framebuffer rectangle of the click GUI, or false when it is not open. */
    static boolean panelFrame(int[] out) {
        ClickGuiLayoutWindow layout = ClickGUI.sharedLayout;
        if (layout == null) return false;
        out[0] = layout.getX();
        out[1] = layout.getY();
        out[2] = Math.max(1, layout.getWidth());
        out[3] = Math.max(1, layout.getHeight());
        return true;
    }

    /** True when a GUI-pixel point lands on a drawn row, not the empty tail under a collapsed list. */
    static boolean covers(double guiX, double guiY) {
        ClickGuiLayoutWindow layout = ClickGUI.sharedLayout;
        if (layout == null) return false;
        double scale = Math.max(1, RubyClient.client.getWindow().getGuiScale());
        double x = layout.getX() / scale;
        double y = layout.getY() / scale;
        double sideW = CategorySidebarWindow.WIDTH / scale;
        double sideH = layout.sidebar().getHeight() / scale;
        if (guiX >= x && guiY >= y && guiX < x + sideW && guiY < y + sideH) return true;

        double panelX = x + (ClickGuiLayoutWindow.SIDEBAR_WIDTH + ClickGuiLayoutWindow.PANEL_GAP) / scale;
        double panelW = ClickGuiLayoutWindow.MODULE_PANEL_WIDTH / scale;
        double solidH = layout.modulePanel().solidHeight() / scale;
        return guiX >= panelX && guiY >= y && guiX < panelX + panelW && guiY < y + solidH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        MouseButtonEvent mapped = this.unmap(click);
        return super.mouseClicked(mapped, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        PerspectiveBoard.endDrag();
        return super.mouseReleased(this.unmap(click));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (PerspectiveBoard.isDragging()) {
            PerspectiveBoard.dragToAim();
            return true;
        }
        double[] start = this.unmap(click.x(), click.y());
        double[] end = this.unmap(click.x() + deltaX, click.y() + deltaY);
        return super.mouseDragged(this.unmap(click), end[0] - start[0], end[1] - start[1]);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ClientSettings.get().perspective() && PerspectiveBoard.scroll(verticalAmount)) return true;
        double[] point = this.unmap(mouseX, mouseY);
        return super.mouseScrolled(point[0], point[1], horizontalAmount, verticalAmount);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double[] point = this.unmap(mouseX, mouseY);
        super.mouseMoved(point[0], point[1]);
    }

    private void play(float to) {
        this.animFrom = this.shown;
        this.animTo = to;
        this.animStart = System.nanoTime();
        if (!ClientSettings.get().animations()) this.shown = to;
    }

    private void advance() {
        if (!ClientSettings.get().animations()) {
            this.shown = this.closing ? 0f : 1f;
            return;
        }
        float dur = this.closing ? Motion.EXIT : Motion.ENTER;
        float t = (System.nanoTime() - this.animStart) / 1_000_000_000f / dur;
        float eased = Motion.out(Math.clamp(t, 0f, 1f));
        this.shown = this.animFrom + (this.animTo - this.animFrom) * eased;
    }

    @Override
    public void onTick() {
        PerspectiveBoard.tick();
        if (ClientSettings.get().perspective() && ClickGUI.sharedLayout != null) {
            ClickGUI.sharedLayout.closeSearch();
        }
        if (this.closing && this.shown <= 0.001f) this.dismissToGame();
    }

    /**
     * {@code setScreenAndShow} renders a frame inside the tick, and that frame
     * applies the cursor warp from grabbing the mouse as a look delta. Closing
     * here, then dropping that delta, leaves the player's pitch where it was.
     */
    private void dismissToGame() {
        var mc = RubyClient.client;
        var player = mc.player;
        float yaw = player != null ? player.getYRot() : 0f;
        float pitch = player != null ? player.getXRot() : 0f;

        PerspectiveBoard.reseat();
        mc.gui.setScreen(null);
        mc.mouseHandler.setIgnoreFirstMove();
        ClickGUI.dropWarpUntil = System.nanoTime() + 250_000_000L;

        if (player != null) {
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yRotO = yaw;
            player.xRotO = pitch;
        }
    }

    /** True when a mouse-grab warp should not be applied as camera movement. */
    public static boolean dropWarpDelta(double dx, double dy) {
        if (System.nanoTime() > ClickGUI.dropWarpUntil) return false;
        return Math.abs(dx) > 200d || Math.abs(dy) > 200d;
    }

    public void requestClose() {
        this.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (ClientSettings.get().perspective()) return super.keyPressed(input);
        if(super.keyPressed(input)) return true;
        return ClickGUI.sharedLayout != null && ClickGUI.sharedLayout.handleSearchKey(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (ClientSettings.get().perspective()) return false;
        if (super.charTyped(input)) return true;
        return ClickGUI.sharedLayout != null && ClickGUI.sharedLayout.handleSearchChar(input);
    }

    @Override
    public void onClose() {
        if(this.closing) return;

        if(ClickGUI.sharedLayout != null)
            ClickGUI.sharedLayout.modulePanel().releaseHeightDrag();

        this.closing = true;
        this.play(0f);
        if (this.shown <= 0.001f) this.dismissToGame();
    }

    @Override
    public void removed() {
        if(ClickGUI.sharedLayout != null) ClickGUI.sharedLayout.modulePanel().releaseHeightDrag();
        if (!this.saved) {
            this.saved = true;
            ConfigManager.saveState();
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
