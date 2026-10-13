package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.Value;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;
import ruby.systems.gui.icons.Lucide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One expandable group of the same setting rows the modules use. */
public class SettingsSectionWindow extends CollapsibleWindow {
    private static final int ROW = 31;
    private static final int PAD = 14;
    private static final int INNER_L = 1;

    private final String name;
    private final int panelWidth;
    private final List<SettingWindow> settings = new ArrayList<>();
    private float hover;
    private float contentAlpha = 1f;
    private Consumer<SettingsSectionWindow> accordionHost;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public SettingsSectionWindow(String name, List<Value<?>> values, int width) {
        super(0, 0, width, ROW);
        this.name = name;
        this.panelWidth = width;
        this.expanded = false;
        this.draggableBounds = new int[] {0, 0, 0, 0};
        for (Value<?> value : values) {
            SettingWindow setting = this.setting(value);
            if (setting == null) continue;
            this.settings.add(setting);
            this.addWindow(setting);
        }
    }

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    public void setAccordionHost(Consumer<SettingsSectionWindow> host) {
        this.accordionHost = host;
    }

    private SettingWindow setting(Value<?> value) {
        if (value instanceof BooleanValue bool) return new SettingToggleWindow(0, 0, this.panelWidth, bool);
        if (value instanceof DoubleValue number) return new SettingSliderWindow(0, 0, this.panelWidth, number);
        if (value instanceof IntegerValue number) return new SettingSliderWindow(0, 0, this.panelWidth, number);
        if (value instanceof EnumValue<?> choice) return new SettingEnumWindow(0, 0, this.panelWidth, choice);
        if (value instanceof ColorValue color) return new SettingColorPickerWindow(0, 0, this.panelWidth, color);
        return null;
    }

    @Override
    public int getWidth() {
        return this.panelWidth;
    }

    @Override
    public int getHeaderHeight() {
        return ROW;
    }

    private int restingBody() {
        int h = 0;
        for (SettingWindow setting : this.settings) h += setting.restingHeight();
        return h;
    }

    @Override
    public int getHeight() {
        int header = ROW;
        if (this.expandProgress() <= 0f) return header;
        int full = ROW + this.restingBody();
        if (this.expandProgress() >= 1f) return full;
        return header + (int) ((full - header) * Motion.standard(this.expandProgress()));
    }

    @Override
    protected int getExpandedHeight() {
        return ROW + this.restingBody();
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        boolean hot = mouseX >= INNER_L && mouseX < this.panelWidth - INNER_L && mouseY >= 0 && mouseY < ROW;
        this.hover = Motion.follow(this.hover, hot ? 1f : 0f, GuiTime.toSeconds(dt), Motion.HOVER);

        super.onRender(context, mouseX, mouseY, dt);

        if (this.expandProgress() <= 0f) return;
        int limit = this.getHeight();
        int settingY = ROW;
        int budget = Math.max(0, limit - ROW);
        for (int i = 0, n = this.settings.size(); i < n; i++) {
            SettingWindow child = this.settings.get(i);
            int rest = child.restingHeight();
            int shown = Math.min(rest, budget);
            child.setVisibleLimit(shown);
            child.setPosition(0, settingY);
            settingY += shown;
            budget -= shown;
        }

        if (this.expandProgress() > 0.01f && !this.settings.isEmpty()) {
            int bg = GUIStyle.withAlpha(GUIStyle.get().bgBase(), this.contentAlpha);
            context.fill(INNER_L, ROW, this.panelWidth - INNER_L, limit, bg);
        }
    }

    @Override
    public void drawHeader(GuiGraphicsExtractor context) {
        GUIStyle style = GUIStyle.get();
        int innerR = this.panelWidth - INNER_L;
        if (this.hover > 0.01f) {
            context.fill(INNER_L, 0, innerR, ROW, GUIStyle.withAlpha(style.bgHover(), this.hover * this.contentAlpha));
        }

        int textY = (ROW - style.bodyFont().fontHeight) / 2;
        int text = GUIStyle.withAlpha(Motion.mix(style.text(), style.textBright(), this.expandProgress()), this.contentAlpha);
        String id = "settings:" + this.name;
        Press.label(style.bodyFont(), context, this.name, PAD, textY, text, id);

        int chevron = Lucide.UI;
        int chevronX = innerR - PAD - chevron;
        Lucide.drawCentered(context, "chevron-right", chevronX + chevron / 2, ROW / 2, chevron,
                GUIStyle.withAlpha(style.textMuted(), this.contentAlpha), this.expandProgress() * 90f);
        context.fill(INNER_L, ROW - 1, innerR, ROW, GUIStyle.withAlpha(style.borderSubtle(), this.contentAlpha));
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.y() < 0 || click.y() >= ROW) return false;
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT && click.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
            return false;
        }
        Press.down("settings:" + this.name);
        this.expanded = !this.expanded;
        if (this.accordionHost != null) this.accordionHost.accept(this);
        return true;
    }
}
