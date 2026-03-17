package ruby.systems.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.windows.ModuleTypeWindow;
import ruby.systems.gui.windows.Window;
import ruby.systems.gui.windows.WindowedScreen;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClickGUI extends WindowedScreen {

    private static final ModuleType[] CATEGORIES = {
            ModuleType.COMBAT, ModuleType.MOVEMENT, ModuleType.PLAYER,
            ModuleType.RENDER, ModuleType.WORLD, ModuleType.MISC
    };

    /* ---- animation state ---- */
    private float openProgress = 0f;
    private boolean closing = false;

    /* ---- search state ---- */
    private String searchText = "";
    private ModuleTypeWindow searchPanel = null;
    private int cursorBlink = 0;

    public ClickGUI() {
        super("Click GUI");
    }

    /* ---- init ---- */

    @Override
    protected void init() {
        super.init();
        this.closing = false;
        // openProgress stays at whatever it was (0 on first open → will animate in)

        Map<String, int[]> positions = ConfigManager.getPanelPositions();

        int xOffset = 20;
        for (ModuleType cat : CATEGORIES) {
            var modules = Modules.getByType(cat);
            if (modules.isEmpty()) continue;

            String key = cat.toString();
            int[] saved = positions.get(key);
            int px = saved != null ? saved[0] : xOffset;
            int py = saved != null ? saved[1] : 20;

            ModuleTypeWindow type = new ModuleTypeWindow(px, py, cat, modules);
            xOffset += type.getWidth() + 20;

            this.addWindow(type);
        }

        this.searchText = "";
        this.searchPanel = null;
    }

    /* ---- render with animation ---- */

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        int width = RubyClient.client.getWindow().getWidth();
        int height = RubyClient.client.getWindow().getHeight();

        // Drive open/close animation (fast lerp)
        float target = this.closing ? 0f : 1f;
        this.openProgress += (target - this.openProgress) * 0.35f;
        if (Math.abs(this.openProgress - target) < 0.005f) this.openProgress = target;

        // Finish closing when animation completes
        if (this.closing && this.openProgress <= 0.01f) {
            RubyClient.client.setScreen(null);
            return;
        }

        // Animated background overlay
        int bgAlpha = (int) (0x88 * this.openProgress);
        context.fill(0, 0, width, height, bgAlpha << 24);

        // Draw search bar at top right
        drawSearchBar(context);
    }

    @Override
    public void onTick() {
        // moved cursor blink here for consistency independent of framerate
        if(this.closing && this.openProgress <= 0.01f) return;

        this.cursorBlink++;
        if(this.cursorBlink > 20) this.cursorBlink = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        float sf = RubyClient.client.getWindow().getScaleFactor();

        // first part of super.render
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(1f / sf, 1f / sf);
        this.onRender(context, mouseX, mouseY);
        context.getMatrices().popMatrix();

        // Scale-based animation: panels grow in / shrink out from their own center
        float scale = this.openProgress;
        for(Window window : this.windows()) {
            context.getMatrices().pushMatrix();

            // Scale around each panel's center
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
    public boolean onMouseDown(Click click, boolean doubled) {
        if(click.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return false;

        int xOffset = 20;
        for(ModuleType type : ModuleType.values()) {
            for(Window child : this.windows()) {
                if(!(child instanceof ModuleTypeWindow mWin)) continue;
                if(!mWin.getTitle().equals(type.toString())) continue;

                child.setPosition(xOffset, 20);
                xOffset += child.getWidth() + 20;
                break;
            }
        }

        return true;
    }

    private void drawSearchBar(DrawContext context) {
        int width = RubyClient.client.getWindow().getWidth();
        GUIStyle style = GUIStyle.get();

        int barW = 220;
        int barH = 24;
        int barX = width - barW - 20;
        int barY = 20;

        float alpha = Math.min(1f, this.openProgress * 2f);

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
        int textY = barY + (barH - style.monospaceFont().fontHeight) / 2;
        style.monospaceFont().draw(context, display, barX + 10, textY, col);

        // Cursor
        if (!this.searchText.isEmpty() && this.cursorBlink < 10) {
            int cx = barX + 10 + style.monospaceFont().getWidth(this.searchText);
            int ch = style.monospaceFont().fontHeight;
            context.fill(cx, textY, cx + 1, textY + ch, 0xFFCCCCCC);
        }
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
        float sf = RubyClient.client.getWindow().getScaleFactor();
        Map<String, int[]> positions = ConfigManager.getPanelPositions();
        int[] saved = positions.get("Search_");
        int px = saved != null ? saved[0] : (int) (this.width * sf) - 260;
        int py = saved != null ? saved[1] : 60;

        this.searchPanel = new SearchPanel(px, py, matches);
        this.addWindow(this.searchPanel);
    }

    /* ---- input handling ---- */

    @Override
    public boolean onKeyPress(KeyInput input) {
        // Shift closes the GUI
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.close();
            return true;
        }

        // Backspace in search
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !this.searchText.isEmpty()) {
            this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
            this.updateSearch();
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        // Append to search
        char c = (char) input.codepoint();
        if(c >= 32 && c < 127) {
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
