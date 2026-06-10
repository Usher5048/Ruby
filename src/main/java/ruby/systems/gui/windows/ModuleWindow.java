package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.*;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Keybind;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.util.LinkedHashSet;
import java.util.function.Consumer;

public class ModuleWindow extends CollapsibleWindow {

    private static final int ROW_H = 31;
    private static final int LAST_ROW_PAD = 4;
    private static final int PANEL_WIDTH = ModulePanelWindow.WIDTH;
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int SETTING_GAP = 2;

    private final Module module;
    private final BooleanValue moduleTOR;
    private final BooleanValue moduleToasts;

    private boolean waitingForBind;
    private float hoverProgress = 0f;
    private Consumer<ModuleWindow> accordionHost;

    private boolean lastInList = false;
    private float contentAlpha = 1f;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean lastKeybindTor;
    private boolean lastShowToasts;

    public ModuleWindow(int x, int y, Module module) {
        super(x, y, PANEL_WIDTH, ROW_H);
        this.expanded = false;

        this.module = module;
        this.moduleTOR = (BooleanValue) new BooleanValue.Builder("Hold bind")
                .defaultValue(this.module.keybind.togglesOnRelease())
                .build();

        this.moduleToasts = (BooleanValue) new BooleanValue.Builder("Show toasts")
                .defaultValue(this.module.showsToasts())
                .build();

        int settingY = this.getHeaderHeight() + 8;
        for (String key : module.config.getAll()) {
            Value<?> value = module.config.get(key);
            if (value instanceof BooleanValue bv) {
                this.addWindow(new SettingToggleWindow(0, settingY, PANEL_WIDTH, bv));
                settingY += 26;
            } else if (value instanceof DoubleValue dv) {
                this.addWindow(new SettingSliderWindow(0, settingY, PANEL_WIDTH, dv));
                settingY += 44;
            } else if (value instanceof IntegerValue iv) {
                this.addWindow(new SettingSliderWindow(0, settingY, PANEL_WIDTH, iv));
                settingY += 44;
            } else if (value instanceof EnumValue<?> ev) {
                this.addWindow(new SettingEnumWindow(0, settingY, PANEL_WIDTH, ev));
                settingY += 26;
            } else if (value instanceof ListValue<?> lv) {
                this.addWindow(new SettingListWindow(0, settingY, PANEL_WIDTH, lv));
                settingY += 26;
            }
        }

        this.addWindow(new SettingToggleWindow(0, settingY, PANEL_WIDTH, this.moduleTOR));
        settingY += 26;
        this.addWindow(new SettingToggleWindow(0, settingY, PANEL_WIDTH, this.moduleToasts));

        this.lastKeybindTor = this.module.keybind.togglesOnRelease();
        this.lastShowToasts = this.module.showsToasts();
    }

    public Module module() {
        return this.module;
    }

    public void setAccordionHost(Consumer<ModuleWindow> host) {
        this.accordionHost = host;
    }

    public void setLastInList(boolean lastInList) {
        this.lastInList = lastInList;
    }

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    @Override
    public int getWidth() {
        this.width = PANEL_WIDTH;
        for (Window child : this.windows()) {
            child.setDimensions(PANEL_WIDTH, child.getHeight());
        }
        return PANEL_WIDTH;
    }

    @Override
    public int getHeaderHeight() {
        int extra = this.lastInList && !this.expanded ? LAST_ROW_PAD : 0;
        return ROW_H + extra;
    }

    @Override
    protected int getExpandedHeight() {
        int h = ROW_H + 8;
        for (Window child : this.windows())
            h += child.getHeight() + SETTING_GAP;
        h += 8;
        if (this.lastInList) h += LAST_ROW_PAD;
        return h;
    }

    @Override
    public void onTick() {
        super.onTick();

        boolean tor = this.module.keybind.togglesOnRelease();
        if (tor != this.lastKeybindTor) {
            this.moduleTOR.setValue(tor);
            this.lastKeybindTor = tor;
        } else if (tor != this.moduleTOR.value()) {
            this.module.keybind.togglesOnRelease(this.moduleTOR.value());
            this.lastKeybindTor = this.moduleTOR.value();
        }

        boolean toasts = this.module.showsToasts();
        if (toasts != this.lastShowToasts) {
            this.moduleToasts.setValue(toasts);
            this.lastShowToasts = toasts;
        } else if (toasts != this.moduleToasts.value()) {
            this.module.showsToasts(this.moduleToasts.value());
            this.lastShowToasts = this.moduleToasts.value();
        }
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        super.onRender(context, mouseX, mouseY, dt);

        int settingY = this.getHeaderHeight() + 8;

        LinkedHashSet<String> keys = new LinkedHashSet<>(this.module.config.getAll());
        keys.add(this.moduleTOR.name());
        keys.add(this.moduleToasts.name());

        for (String key : keys) {
            Value<?> value = this.module.config.get(key);
            if (value == null) {
                if (this.moduleToasts.name().equals(key)) value = this.moduleToasts;
                if (this.moduleTOR.name().equals(key)) value = this.moduleTOR;
            }

            for (Window child : this.windows()) {
                if (!(child instanceof SettingWindow sWin)) continue;
                if (sWin.value() != value) continue;
                child.setPosition(0, settingY);
                settingY += child.getHeight();
                break;
            }
        }

        GUIStyle style = GUIStyle.get();
        if (this.expandProgress() > 0.01f && !this.windows().isEmpty()) {
            int bg = GUIStyle.withAlpha(style.bgBase(), this.contentAlpha);
            int w = PANEL_WIDTH;
            int innerR = w - INNER_L;
            int bottom = this.getHeight();
            if (this.lastInList) {
                ModuleTypeWindow.fillBottomRoundedRect(context, INNER_L, this.getHeaderHeight(), innerR, bottom, GUIStyle.RADIUS_PANEL, bg);
            } else {
                context.fill(INNER_L, this.getHeaderHeight(), innerR, bottom, bg);
            }
        }
    }

    @Override
    public void drawHeader(DrawContext context) {
        GUIStyle style = GUIStyle.get();
        int w = PANEL_WIDTH;
        int innerR = w - INNER_L;
        int hdr = this.getHeaderHeight();
        float alpha = this.contentAlpha;

        boolean hovered = this.isHeaderHovered();
        float hoverTarget = hovered ? 1f : 0f;
        this.hoverProgress += (hoverTarget - this.hoverProgress) * 0.25f;
        if (Math.abs(this.hoverProgress - hoverTarget) < 0.01f) this.hoverProgress = hoverTarget;

        if (this.hoverProgress > 0.01f) {
            int hoverColor = GUIStyle.withAlpha(style.bgHover(), this.hoverProgress * alpha);
            boolean roundBottom = this.lastInList && !this.expanded;
            ModuleTypeWindow.fillRowBackground(context, INNER_L, 0, innerR, hdr, hoverColor, roundBottom);
        }

        int textY = (ROW_H - style.bodyFont().fontHeight) / 2;
        int nameColor = this.module.enabled()
                ? GUIStyle.withAlpha(style.ruby(), alpha)
                : GUIStyle.withAlpha(style.text(), alpha);
        this.drawText(style.bodyFont(), context, this.module.name(), PAD, textY, nameColor);

        String keyName = this.waitingForBind ? "..." : this.module.keybind.toString();
        int keyW = style.badgeFont().getWidth(keyName);
        int badgePadX = 6;
        int badgeW = keyW + badgePadX * 2;
        int badgeH = style.badgeFont().fontHeight + 2;

        int dotsRight = innerR - PAD;
        int badgeX = dotsRight - 8 - badgeW;
        int badgeY = (ROW_H - badgeH) / 2;
        int keyTextY = badgeY + (badgeH - style.badgeFont().fontHeight) / 2;

        ModuleTypeWindow.drawMenuDots(context, dotsRight, ROW_H / 2, GUIStyle.withAlpha(0xFF333333, alpha));

        int badgeBg = GUIStyle.withAlpha(style.bgHover(), alpha);
        int badgeBorder = GUIStyle.withAlpha(style.border(), alpha);
        ModuleTypeWindow.drawRoundedBadge(context, badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, badgeBg, badgeBorder);

        style.badgeFont().draw(context, keyName, badgeX + badgePadX, keyTextY,
                GUIStyle.withAlpha(style.textMuted(), alpha));

        if (!this.lastInList) {
            context.fill(INNER_L, hdr - 1, innerR, hdr, GUIStyle.withAlpha(style.borderSubtle(), alpha));
        }
    }

    private boolean isHeaderHovered() {
        int hitH = this.lastInList && !this.expanded ? this.getHeaderHeight() : ROW_H;
        return this.lastMouseX >= INNER_L && this.lastMouseX < PANEL_WIDTH - INNER_L
                && this.lastMouseY >= 0 && this.lastMouseY < hitH;
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (this.waitingForBind) {
            this.waitingForBind = false;
            if (Keybind.canBindTo(click.button(), false)) {
                this.module.keybind.mouse(click.button());
                return true;
            }
        }

        if (click.y() >= this.getHeaderHeight()) return false;

        GUIStyle style = GUIStyle.get();
        String keyName = this.waitingForBind ? "..." : this.module.keybind.toString();
        int keyW = style.badgeFont().getWidth(keyName);
        int badgeW = keyW + 12;
        int innerR = PANEL_WIDTH - INNER_L;
        int badgeX = innerR - PAD - 8 - badgeW;

        if (click.x() >= badgeX && click.x() < badgeX + badgeW) {
            this.waitingForBind = true;
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Modules.toggle(this.module);
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.expanded = !this.expanded;
            if (this.accordionHost != null) this.accordionHost.accept(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        return this.waitingForBind;
    }

    @Override
    public boolean onKeyRelease(KeyInput input) {
        if (!this.waitingForBind) return false;

        this.waitingForBind = false;
        if (!Keybind.canBindTo(input.key(), true)) this.module.keybind.unbind();
        else this.module.keybind.key(input.key());

        return true;
    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        if (!this.waitingForBind) return false;
        return input.key() == GLFW.GLFW_KEY_ESCAPE;
    }

    @Override
    public boolean onFocusRemoved() {
        this.waitingForBind = false;
        return true;
    }
}
