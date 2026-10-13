package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.sdl.SDLMouse;
import ruby.systems.config.Value;
import ruby.systems.gui.ClientSettings;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.text.FontRenderer;

public class SettingWindow extends Window {
    protected static final int ROW = 22;
    protected static final int PAD = 14;

    protected final Value<?> value;
    private int visibleLimit = -1;
    private float hover;
    private static CursorType pointer;

    public SettingWindow(int x, int y, int width, int height, Value<?> value) {
        super(x, y, width, height);
        this.value = value;
    }

    public Value<?> value() {
        return this.value;
    }

    /** Height once this setting's own open animation has settled. */
    public int restingHeight() {
        return this.value.visible() ? this.scaled(ROW) : 0;
    }

    protected float itemScale() {
        return ClientSettings.get().dropdownScale();
    }

    protected int scaled(int design) {
        return Math.max(1, Math.round(design * this.itemScale()));
    }

    /** Panel width. Dropdown contents are sized in whole pixels, never stretched. */
    protected int designWidth() {
        return this.getWidth();
    }

    protected double design(double layout) {
        return layout;
    }

    protected void beginItem(GuiGraphicsExtractor context) {
    }

    protected void endItem(GuiGraphicsExtractor context) {
    }

    /** How much of this setting the module frame is currently revealing. */
    public void setVisibleLimit(int visibleLimit) {
        this.visibleLimit = visibleLimit;
    }

    protected float hoverMix() {
        return this.hover;
    }

    /** Soft row highlight and a pointer, drawn in layout pixels behind the scaled content. */
    protected void paintHover(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        int width = this.getWidth();
        int height = this.getHeight();
        int band = Math.min(height, this.scaled(ROW));
        boolean over = mouseX >= 0 && mouseY >= 0 && mouseX < width && mouseY < height;
        boolean hot = over && mouseY < band;
        float target = hot ? 1f : 0f;
        if (!ClientSettings.get().animations()) this.hover = target;
        else this.hover = Motion.follow(this.hover, target, GuiTime.toSeconds(dt), Motion.HOVER);
        if (this.hover > 0.01f) {
            context.fill(1, 0, width - 1, band, GUIStyle.withAlpha(GUIStyle.get().bgHover(), this.hover));
        }
        if (over) context.requestCursor(SettingWindow.pointer());
    }

    private static CursorType pointer() {
        CursorType cursor = SettingWindow.pointer;
        if (cursor != null) return cursor;
        cursor = CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_POINTER, "setting_pointer", CursorType.DEFAULT);
        SettingWindow.pointer = cursor;
        return cursor;
    }

    @Override
    public int getHeight() {
        if (!this.value.visible()) return 0;
        int rest = this.restingHeight();
        if (this.visibleLimit < 0) return rest;
        return Math.min(rest, this.visibleLimit);
    }

    protected void clip(GuiGraphicsExtractor context) {
        context.enableScissor(0, 0, this.getWidth(), Math.max(0, this.getHeight()));
    }

    protected void unclip(GuiGraphicsExtractor context) {
        context.disableScissor();
    }

    protected String fit(FontRenderer font, String text, int maxW) {
        if (text == null || text.isEmpty()) return "";
        if (font.getWidth(text) <= maxW) return text;
        String ellipsis = "...";
        while (text.length() > 1 && font.getWidth(text + ellipsis) > maxW) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }
}
