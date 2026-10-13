package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import ruby.systems.config.*;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;
import ruby.systems.modules.Keybind;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

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
    private float chipHover = 0f;
    private float enabledMix = 0f;
    private Consumer<ModuleWindow> accordionHost;

    private boolean lastInList = false;
    private int bodyClip;
    private boolean drawBottomSeparator = true;
    private float contentAlpha = 1f;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean lastKeybindTor;
    private boolean lastShowToasts;
    private boolean metaFolded;
    private final List<SettingWindow> settings = new ArrayList<>();
    private String keyLabel = "None";
    private int keySerial = Integer.MIN_VALUE;

    public ModuleWindow(int x, int y, Module module) {
        super(x, y, PANEL_WIDTH, ROW_H);
        this.expanded = false;

        this.module = module;
        this.enabledMix = module.enabled() ? 1f : 0f;
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
                this.addSetting(new SettingToggleWindow(0, settingY, PANEL_WIDTH, bv));
                settingY += 26;
            } else if (value instanceof DoubleValue dv) {
                this.addSetting(new SettingSliderWindow(0, settingY, PANEL_WIDTH, dv));
                settingY += 44;
            } else if (value instanceof IntegerValue iv) {
                this.addSetting(new SettingSliderWindow(0, settingY, PANEL_WIDTH, iv));
                settingY += 44;
            } else if (value instanceof EnumValue<?> ev) {
                this.addSetting(new SettingEnumWindow(0, settingY, PANEL_WIDTH, ev));
                settingY += 26;
            } else if (value instanceof ListValue<?> lv) {
                this.addSetting(new SettingListWindow(0, settingY, PANEL_WIDTH, lv));
                settingY += 26;
            } else if (value instanceof ColorValue cv) {
                this.addSetting(new SettingColorPickerWindow(0, settingY, PANEL_WIDTH, cv));
                settingY += 26;
            }
        }

        this.addSetting(new SettingPairWindow(0, settingY, PANEL_WIDTH, this.moduleTOR, this.moduleToasts));

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

    /** Panel-local outer bottom, in this module's coordinates. 0 until the list is laid out. */
    public void setBodyClip(int bodyClip) {
        this.bodyClip = bodyClip;
    }

    public void setDrawBottomSeparator(boolean drawBottomSeparator) {
        this.drawBottomSeparator = drawBottomSeparator;
    }

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    /** Live windows were built with Hold bind and Show toasts as two full rows. */
    private void foldMeta() {
        if (this.metaFolded) return;
        this.metaFolded = true;
        SettingWindow hold = null;
        SettingWindow toasts = null;
        for (SettingWindow setting : this.settings) {
            if (!(setting instanceof SettingToggleWindow)) continue;
            String name = setting.value().name();
            if ("Hold bind".equals(name)) hold = setting;
            else if ("Show toasts".equals(name)) toasts = setting;
        }
        if (hold == null || toasts == null) return;
        this.settings.remove(hold);
        this.settings.remove(toasts);
        this.windows().remove(hold);
        this.windows().remove(toasts);
        this.addSetting(new SettingPairWindow(0, 0, PANEL_WIDTH, this.moduleTOR, this.moduleToasts));
    }

    private void addSetting(SettingWindow setting) {
        this.settings.add(setting);
        this.addWindow(setting);
    }

    @Override
    public int layoutStamp() {
        int header = this.getHeaderHeight();
        if (this.expandProgress() <= 0f) return header;
        int stamp = Float.floatToIntBits(this.expandProgress());
        stamp = stamp * 31 + header;
        stamp = stamp * 31 + Float.floatToIntBits(ruby.systems.gui.ClientSettings.get().dropdownScale());
        var children = this.windows();
        for (int i = 0, n = children.size(); i < n; i++) {
            stamp = stamp * 31 + children.get(i).layoutStamp();
        }
        return stamp;
    }

    @Override
    public int getWidth() {
        return PANEL_WIDTH;
    }

    @Override
    public int getHeaderHeight() {
        int extra = this.lastInList && this.expandProgress() <= 0f ? LAST_ROW_PAD : 0;
        return ROW_H + extra;
    }

    private int restingBody() {
        int h = 0;
        for (Window child : this.windows()) {
            if (child instanceof SettingWindow setting) h += setting.restingHeight();
            else h += child.getHeight();
        }
        return h;
    }

    @Override
    public int getHeight() {
        int header = this.getHeaderHeight();
        if (this.expandProgress() <= 0f) return header;
        int full = ROW_H + this.restingBody();
        if (this.lastInList) full += LAST_ROW_PAD;
        if (this.expandProgress() >= 1f) return full;
        return header + (int) ((full - header) * Motion.standard(this.expandProgress()));
    }

    @Override
    protected int getExpandedHeight() {
        int full = ROW_H + this.restingBody();
        if (this.lastInList) full += LAST_ROW_PAD;
        return full;
    }

    @Override
    public void onTick() {
        super.onTick();
        this.foldMeta();

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
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.tickHover(dt);

        super.onRender(context, mouseX, mouseY, dt);

        if (this.expandProgress() > 0f) {
            int limit = this.getHeight();
            int settingY = this.getHeaderHeight();
            int budget = Math.max(0, limit - settingY);
            for (int i = 0, n = this.settings.size(); i < n; i++) {
                SettingWindow child = this.settings.get(i);
                int rest = child.restingHeight();
                int shown = Math.min(rest, budget);
                child.setVisibleLimit(shown);
                child.setPosition(0, settingY);
                settingY += shown;
                budget -= shown;
            }
        }

        GUIStyle style = GUIStyle.get();
        if (this.expandProgress() > 0.01f && !this.windows().isEmpty()) {
            int bg = GUIStyle.withAlpha(style.bgBase(), this.contentAlpha);
            int w = PANEL_WIDTH;
            int innerR = w - INNER_L;
            int top = this.getHeaderHeight();
            int bottom = this.getHeight();
            // The container stroke owns the outer pixel and the inner corner is radius-1.
            // A full-radius fill leaves a gap of panel color along that corner.
            boolean meetsContainer = this.bodyClip > top && bottom >= this.bodyClip - 1;
            if (meetsContainer || this.lastInList) {
                int y2 = meetsContainer ? Math.min(bottom, this.bodyClip) : bottom;
                ModuleTypeWindow.fillRowBackground(context, INNER_L, top, innerR, y2, bg, true);
            } else {
                context.fill(INNER_L, top, innerR, bottom, bg);
            }
        }
    }

    @Override
    public void drawHeader(GuiGraphicsExtractor context) {
        GUIStyle style = GUIStyle.get();
        int w = PANEL_WIDTH;
        int innerR = w - INNER_L;
        int hdr = this.getHeaderHeight();
        float alpha = this.contentAlpha;

        if (this.hoverProgress > 0.01f) {
            int hoverColor = GUIStyle.withAlpha(style.bgHover(), this.hoverProgress * alpha);
            boolean roundBottom = this.lastInList && !this.expanded;
            ModuleTypeWindow.fillRowBackground(context, INNER_L, 0, innerR, hdr, hoverColor, roundBottom);
        }

        boolean chips = ruby.systems.gui.ClientSettings.get().showKeybinds();
        KeyChip chip = chips ? this.keyChip(style) : null;
        int textY = (ROW_H - style.bodyFont().fontHeight) / 2;
        int nameColor = GUIStyle.withAlpha(Motion.mix(style.text(), style.ruby(), this.enabledMix), alpha);
        int nameLimit = chips ? chip.x() - 8 : PANEL_WIDTH - PAD;
        Press.label(style.bodyFont(), context, this.fittedName(style, nameLimit), PAD, textY, nameColor, "mod:" + this.module.name());
        if (chips) this.drawKeyChip(context, style, chip, alpha);

        if (this.drawBottomSeparator && !this.lastInList) {
            context.fill(INNER_L, hdr - 1, innerR, hdr, GUIStyle.withAlpha(style.borderSubtle(), alpha));
        }
    }

    private void tickHover(float dt) {
        float seconds = GuiTime.toSeconds(dt);
        float hoverTarget = this.isHeaderHovered() ? 1f : 0f;
        this.hoverProgress = Motion.follow(this.hoverProgress, hoverTarget, seconds, Motion.HOVER);
        float chipTarget = this.isChipHovered(GUIStyle.get()) ? 1f : 0f;
        this.chipHover = Motion.follow(this.chipHover, chipTarget, seconds, Motion.HOVER);
        float enabledTarget = this.module.enabled() ? 1f : 0f;
        this.enabledMix = Motion.follow(this.enabledMix, enabledTarget, seconds, Motion.FAST);
    }

    private String keyLabel() {
        if (this.waitingForBind) return "Press";
        int serial = this.module.keybind.serialize();
        if (serial != this.keySerial) {
            this.keySerial = serial;
            this.keyLabel = this.module.keybind.toString();
        }
        return this.keyLabel;
    }

    private KeyChip keyChip(GUIStyle style) {
        int width = PANEL_WIDTH;
        String label = this.keyLabel();
        int textW = style.bodyFont().getWidth(label);
        int minW = style.bodyFont().getWidth("None");
        int w = Math.max(textW, minW) + 16;
        int h = style.bodyFont().fontHeight + 8;
        int x = width - INNER_L - PAD - w;
        int y = (ROW_H - h) / 2;
        int textX = x + (w - textW) / 2;
        int textY = y + (h - style.bodyFont().fontHeight) / 2;
        return new KeyChip(x, y, w, h, textX, textY, label);
    }

    private String fittedName(GUIStyle style, int limitX) {
        String name = this.module.name();
        int maxW = limitX - PAD;
        if (maxW <= 0 || style.bodyFont().getWidth(name) <= maxW) return name;
        String ellipsis = "…";
        while (name.length() > 1 && style.bodyFont().getWidth(name + ellipsis) > maxW) {
            name = name.substring(0, name.length() - 1);
        }
        return name + ellipsis;
    }

    private void drawKeyChip(GuiGraphicsExtractor context, GUIStyle style, KeyChip chip, float alpha) {
        boolean listening = this.waitingForBind;
        boolean unbound = !listening && this.module.keybind.isUnbound();
        float hover = this.chipHover;
        int fill = listening ? style.rubyBg() : Motion.mix(style.bgBase(), style.bgHover(), hover);
        int edge = listening ? style.ruby() : Motion.mix(style.borderPanel(), style.textMuted(), hover * 0.4f);
        int text = listening ? style.ruby() : (unbound ? style.textMuted() : style.textBright());

        String id = "bind:" + this.module.name();
        float press = Press.sample(id);
        text = Motion.mix(text, Motion.mix(text, 0xFFFFFFFF, 0.55f), press);

        var pose = context.pose();
        pose.pushMatrix();
        if (press >= 0.02f) {
            float cx = chip.x() + chip.w() / 2f;
            float cy = chip.y() + chip.h() / 2f;
            pose.translate(cx, cy);
            pose.scale(1f - 0.08f * press, 1f - 0.08f * press);
            pose.translate(-cx, -cy);
        }
        ModuleTypeWindow.drawRoundedBadge(context, chip.x(), chip.y(), chip.x() + chip.w(), chip.y() + chip.h(),
                GUIStyle.withAlpha(fill, alpha), GUIStyle.withAlpha(edge, alpha));
        style.bodyFont().draw(context, chip.label(), chip.textX(), chip.textY(), GUIStyle.withAlpha(text, alpha));
        pose.popMatrix();
    }

    private boolean isChipHovered(GUIStyle style) {
        KeyChip chip = this.keyChip(style);
        return this.lastMouseX >= chip.x() && this.lastMouseX < chip.x() + chip.w()
                && this.lastMouseY >= chip.y() && this.lastMouseY < chip.y() + chip.h();
    }

    private boolean isHeaderHovered() {
        int hitH = this.lastInList && !this.expanded ? this.getHeaderHeight() : ROW_H;
        return this.lastMouseX >= INNER_L && this.lastMouseX < PANEL_WIDTH - INNER_L
                && this.lastMouseY >= 0 && this.lastMouseY < hitH;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.y() >= this.getHeaderHeight()) return false;

        if (ruby.systems.gui.ClientSettings.get().showKeybinds()) {
            GUIStyle style = GUIStyle.get();
            KeyChip chip = this.keyChip(style);
            if (click.x() >= chip.x() && click.x() < chip.x() + chip.w()
                    && click.y() >= chip.y() && click.y() < chip.y() + chip.h()) {
                Press.down("bind:" + this.module.name());
                if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                    this.waitingForBind = false;
                    if (!this.module.keybind.isUnbound()) this.module.keybind.unbind();
                    return true;
                }
                if (this.waitingForBind || click.button() != InputConstants.MOUSE_BUTTON_LEFT) {
                    this.waitingForBind = false;
                    return true;
                }
                this.waitingForBind = true;
                return true;
            }

            if (this.waitingForBind) {
                this.waitingForBind = false;
                if (Keybind.canBindTo(click.button(), false)) {
                    this.module.keybind.mouse(click.button());
                }
                return true;
            }
        }

        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            Press.down("mod:" + this.module.name());
            Modules.toggle(this.module);
            return true;
        }

        if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            this.expanded = !this.expanded;
            if (this.accordionHost != null) this.accordionHost.accept(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        return this.waitingForBind;
    }

    @Override
    public boolean onKeyRelease(KeyEvent input) {
        if (!this.waitingForBind) return false;

        this.waitingForBind = false;
        int key = input.key();
        if (key == InputConstants.KEY_ESCAPE) return true;
        if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
            this.module.keybind.unbind();
            return true;
        }
        if (Keybind.canBindTo(key, true)) this.module.keybind.key(key);
        return true;
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        return this.waitingForBind;
    }

    @Override
    public boolean onFocusRemoved() {
        this.waitingForBind = false;
        return true;
    }

    private record KeyChip(int x, int y, int w, int h, int textX, int textY, String label) {}
}
