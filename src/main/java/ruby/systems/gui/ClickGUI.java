package ruby.systems.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.windows.ModuleTypeWindow;
import ruby.systems.gui.windows.Window;
import ruby.systems.gui.windows.WindowedScreen;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClickGUI extends WindowedScreen {

    private static final ModuleCategory[] CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.PLAYER,
            ModuleCategory.RENDER, ModuleCategory.WORLD, ModuleCategory.MISC
    };

    /* ---- animation state ---- */
    private float openProgress = 0f;
    private boolean closing = false;

    /* ---- search state ---- */
    private String searchText = "";
    private ModuleTypeWindow searchPanel = null;
    private float cursorBlink = 0f;

    public ClickGUI() {
        super("Click GUI");
    }

    /* ---- init ---- */

    @Override
    protected void init() {
        super.init();
        this.windows().clear();
        this.closing = false;
        // openProgress stays at whatever it was (0 on first open → will animate in)

        Map<String, int[]> positions = ConfigManager.getPanelPositions();

        int spacing = 200;
        int totalCategories = 0;
        for (ModuleCategory cat : CATEGORIES) {
            if (!Modules.getByCategory(cat).isEmpty()) totalCategories++;
        }

        int totalWidth = totalCategories * spacing;
        int startX = Math.max(20, (this.width - totalWidth) / 2);

        int col = 0;
        for (ModuleCategory cat : CATEGORIES) {
            var modules = Modules.getByCategory(cat);
            if (modules.isEmpty()) continue;

            String key = cat.toString();
            int[] saved = positions.get(key);
            int px = saved != null ? saved[0] : startX + col * spacing;
            int py = saved != null ? saved[1] : 60;

            this.addWindow(new ModuleTypeWindow(px, py, cat, modules));
            col++;
        }

        this.searchText = "";
        this.searchPanel = null;
    }

    /* ---- render with animation ---- */

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        // Drive open/close animation (fast lerp)
        float target = this.closing ? 0f : 1f;
        this.openProgress += (target - this.openProgress) * 0.35f;
        if (Math.abs(this.openProgress - target) < 0.005f) this.openProgress = target;

        // Finish closing when animation completes
        if (this.closing && this.openProgress <= 0.01f) {
            this.client.setScreen(null);
            return;
        }

        // Animated background overlay
        int bgAlpha = (int) (0x88 * this.openProgress);
        context.fill(0, 0, this.width, this.height, bgAlpha << 24);

        // Scale-based animation: panels grow in / shrink out from their own center
        double sf = this.client.getWindow().getScaleFactor();
        float scale = this.openProgress;

        for (Window window : this.windows()) {
            context.getMatrices().pushMatrix();

            // Scale around each panel's center
            float cx = window.getX() / (float) sf + (window.getWidth() / (float) sf) / 2f;
            float cy = window.getY() / (float) sf + (window.getHeight() / (float) sf) / 2f;
            context.getMatrices().translate(cx, cy);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-cx, -cy);

            window.render(context, mouseX, mouseY, tickDelta);
            context.getMatrices().popMatrix();
        }

        // Draw search bar at top right
        drawSearchBar(context);

        this.cursorBlink += 0.07f;
        if (this.cursorBlink > 2f) this.cursorBlink = 0f;
    }

    private void drawSearchBar(DrawContext context) {
        GUIStyle style = GUIStyle.get();
        double sf = this.client.getWindow().getScaleFactor();

        int barW = 220;
        int barH = 24;
        int barX = (int) (this.width * sf) - barW - 16;
        int barY = 16;

        float alpha = Math.min(1f, this.openProgress * 2f);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(1f / (float) sf, 1f / (float) sf);

        int bgAlpha = (int) (0xCC * alpha);
        ModuleTypeWindow.fillSmoothRoundedRect(context, barX, barY, barX + barW, barY + barH, 6,
                (bgAlpha << 24) | 0x0A0A0A);

        // Border glow when active
        if (!this.searchText.isEmpty()) {
            int glowAlpha = (int) (0x33 * alpha);
            ModuleTypeWindow.fillSmoothRoundedRect(context, barX - 1, barY - 1,
                    barX + barW + 1, barY + barH + 1, 7, (glowAlpha << 24) | 0xCC3344);
        }

        // Text or placeholder
        String display = this.searchText.isEmpty() ? "Search..." : this.searchText;
        int col = this.searchText.isEmpty() ? 0xFF555555 : 0xFFCCCCCC;
        int textY = barY + (barH - (int) (style.monospaceFont().fontHeight)) / 2;
        style.monospaceFont().draw(context, display, barX + 10, textY, col);

        // Cursor
        if (!this.searchText.isEmpty() && this.cursorBlink < 1f) {
            int cx = barX + 10 + (int) style.monospaceFont().getWidth(this.searchText);
            int ch = (int) style.monospaceFont().fontHeight;
            context.fill(cx, textY, cx + 1, textY + ch, 0xFFCCCCCC);
        }

        context.getMatrices().popMatrix();
    }

    /* ---- search ---- */

    private void updateSearch() {
        // Remove existing search panel
        if (this.searchPanel != null) {
            this.windows().remove(this.searchPanel);
            this.searchPanel = null;
        }

        if (this.searchText.isEmpty()) return;

        // Find matching modules
        String lower = this.searchText.toLowerCase(Locale.ROOT);
        List<Module> matches = new ArrayList<>();
        for (Module m : Modules.getModules()) {
            if (m.name().toLowerCase(Locale.ROOT).contains(lower)) {
                matches.add(m);
            }
        }
        if (matches.isEmpty()) return;

        // Create search panel at a reasonable position
        Map<String, int[]> positions = ConfigManager.getPanelPositions();
        int[] saved = positions.get("Search");
        double sf = this.client.getWindow().getScaleFactor();
        int px = saved != null ? saved[0] : (int)(this.width * sf) - 260;
        int py = saved != null ? saved[1] : 60;

        this.searchPanel = new SearchPanel(px, py, matches);
        this.addWindow(this.searchPanel);
    }

    /* ---- input handling ---- */

    @Override
    public boolean keyPressed(KeyInput input) {
        // Shift closes the GUI
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT || input.key() == GLFW.GLFW_KEY_LEFT_SHIFT) {
            this.close();
            return true;
        }

        // ESC closes the GUI
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }

        // Backspace in search
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !this.searchText.isEmpty()) {
            this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
            this.updateSearch();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        // Let focused child handle chars first (e.g., keybind setting, list search)
        if (super.charTyped(input)) return true;

        // Append to search
        char c = (char) input.codepoint();
        if (c >= 32 && c < 127) {
            this.searchText += c;
            this.updateSearch();
            return true;
        }
        return false;
    }

    /* ---- close with animation + save ---- */

    @Override
    public void close() {
        if (this.closing) return;

        // Save panel positions
        Map<String, int[]> positions = ConfigManager.getPanelPositions();
        for (Window window : this.windows()) {
            if (window instanceof ModuleTypeWindow mtw) {
                positions.put(mtw.getTitle(), new int[]{ mtw.getX(), mtw.getY() });
            }
        }

        this.closing = true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /* ---- search panel inner class ---- */

    private static class SearchPanel extends ModuleTypeWindow {
        SearchPanel(int x, int y, List<Module> modules) {
            super(x, y, null, modules);
        }

        @Override
        public String getTitle() {
            return "Search";
        }

        @Override
        public void drawHeader(DrawContext context) {
            GUIStyle style = GUIStyle.get();
            int w = this.getWidth();
            int h = this.getHeight();
            int hdr = this.getHeaderHeight();

            fillSmoothRoundedRect(context, 2, 6, w + 2, h + 6, 12, 0x44000000);
            fillSmoothRoundedRect(context, 0, 0, w, h, 10, 0x0AFFFFFF);
            fillSmoothRoundedRect(context, 1, 1, w - 1, h - 1, 9, 0xD90A0A0A);

            this.drawCenteredText(style.subHeaderFont(), context, "Search", w / 2, hdr / 2, 0xFFCC3344);
            context.fill(1, hdr - 1, w - 1, hdr, 0x0DFFFFFF);
        }
    }
}
