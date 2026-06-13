package ruby.systems.gui;

import com.google.gson.annotations.SerializedName;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class ThemeManager {
    private static final int DEFAULT_RUBY = 0xFFA82938;
    private static final int DEFAULT_RUBY_HOVER = 0xFFC23344;

    private int ruby = ThemeManager.DEFAULT_RUBY;
    private int rubyHover = ThemeManager.DEFAULT_RUBY_HOVER;
    private boolean pendingApply = false;
    private int rubyBg = 0xFF0F0C0D;
    private int rubyActive = 0xFF141012;
    private int bgPanel = 0xFF090809;
    private int bgBase = 0xFF050405;
    private int bgHover = 0xFF121012;
    private int bgElevated = 0xFF0E0C0D;
    private int text = 0xFFB8B4B5;
    private int textBright = 0xFFD4D0D1;
    private int textMuted = 0xFF5A5557;
    private int catInactive = 0xFF8A8688;
    private int arrowInactive = 0xFF333333;
    private int borderPanel = 0x06FFFFFF;
    private int borderSubtle = 0xFF121012;
    private int border = 0xFF181416;
    private int trackOff = 0xFF1A1617;
    private int overlayDim = 0x88000000;

    private ThemeManager() {}

    public static ThemeManager get() {
        return Holder.INSTANCE;
    }

    public List<ThemeColor> colors() {
        List<ThemeColor> list = new ArrayList<>();
        list.add(new ThemeColor("Ruby", () -> this.ruby, v -> this.ruby = v, ThemeManager.DEFAULT_RUBY));
        list.add(new ThemeColor("Ruby Hover", () -> this.rubyHover, v -> this.rubyHover = v, ThemeManager.DEFAULT_RUBY_HOVER));
        list.add(new ThemeColor("Ruby Background", () -> this.rubyBg, v -> this.rubyBg = v, 0xFF0F0C0D));
        list.add(new ThemeColor("Ruby Active", () -> this.rubyActive, v -> this.rubyActive = v, 0xFF141012));
        list.add(new ThemeColor("Panel", () -> this.bgPanel, v -> this.bgPanel = v, 0xFF090809));
        list.add(new ThemeColor("Base", () -> this.bgBase, v -> this.bgBase = v, 0xFF050405));
        list.add(new ThemeColor("Hover", () -> this.bgHover, v -> this.bgHover = v, 0xFF121012));
        list.add(new ThemeColor("Elevated", () -> this.bgElevated, v -> this.bgElevated = v, 0xFF0E0C0D));
        list.add(new ThemeColor("Text", () -> this.text, v -> this.text = v, 0xFFB8B4B5));
        list.add(new ThemeColor("Text Bright", () -> this.textBright, v -> this.textBright = v, 0xFFD4D0D1));
        list.add(new ThemeColor("Text Muted", () -> this.textMuted, v -> this.textMuted = v, 0xFF5A5557));
        list.add(new ThemeColor("Category", () -> this.catInactive, v -> this.catInactive = v, 0xFF8A8688));
        list.add(new ThemeColor("Arrow", () -> this.arrowInactive, v -> this.arrowInactive = v, 0xFF333333));
        list.add(new ThemeColor("Panel Border", () -> this.borderPanel, v -> this.borderPanel = v, 0x06FFFFFF));
        list.add(new ThemeColor("Subtle Border", () -> this.borderSubtle, v -> this.borderSubtle = v, 0xFF121012));
        list.add(new ThemeColor("Border", () -> this.border, v -> this.border = v, 0xFF181416));
        list.add(new ThemeColor("Track Off", () -> this.trackOff, v -> this.trackOff = v, 0xFF1A1617));
        list.add(new ThemeColor("Overlay", () -> this.overlayDim, v -> this.overlayDim = v, 0x88000000));
        return list;
    }

    public void applyIfNeeded() {
        if (!this.pendingApply) return;
        this.pendingApply = false;
        this.apply();
    }

    public void apply() {
        GUIStyle base = GUIStyle.defaultStyle();
        GUIStyle.set(new GUIStyle(
                this.ruby, this.rubyHover, this.rubyBg, this.rubyActive,
                this.bgPanel, this.bgBase, this.bgHover, this.bgElevated,
                this.text, this.textBright, this.textMuted, this.catInactive, this.arrowInactive,
                this.borderPanel, this.borderSubtle, this.border, this.trackOff, this.overlayDim,
                base.logoFont(), base.subHeaderFont(), base.bodyFont(), base.labelFont(),
                base.badgeFont(), base.profileFont(), base.monospaceFont()
        ));
    }

    public void writeToProfile(ByteArrayOutputStream stream) {
        ConfigManager.writeInt(stream, this.ruby);
        ConfigManager.writeInt(stream, this.rubyHover);
        ConfigManager.writeInt(stream, this.rubyBg);
        ConfigManager.writeInt(stream, this.rubyActive);
        ConfigManager.writeInt(stream, this.bgPanel);
        ConfigManager.writeInt(stream, this.bgBase);
        ConfigManager.writeInt(stream, this.bgHover);
        ConfigManager.writeInt(stream, this.bgElevated);
        ConfigManager.writeInt(stream, this.text);
        ConfigManager.writeInt(stream, this.textBright);
        ConfigManager.writeInt(stream, this.textMuted);
        ConfigManager.writeInt(stream, this.catInactive);
        ConfigManager.writeInt(stream, this.arrowInactive);
        ConfigManager.writeInt(stream, this.borderPanel);
        ConfigManager.writeInt(stream, this.borderSubtle);
        ConfigManager.writeInt(stream, this.border);
        ConfigManager.writeInt(stream, this.trackOff);
        ConfigManager.writeInt(stream, this.overlayDim);
    }

    public void readFromProfile(ByteArrayInputStream stream) {
        this.ruby = ConfigManager.readInt(stream);
        this.rubyHover = ConfigManager.readInt(stream);
        this.rubyBg = ConfigManager.readInt(stream);
        this.rubyActive = ConfigManager.readInt(stream);
        this.bgPanel = ConfigManager.readInt(stream);
        this.bgBase = ConfigManager.readInt(stream);
        this.bgHover = ConfigManager.readInt(stream);
        this.bgElevated = ConfigManager.readInt(stream);
        this.text = ConfigManager.readInt(stream);
        this.textBright = ConfigManager.readInt(stream);
        this.textMuted = ConfigManager.readInt(stream);
        this.catInactive = ConfigManager.readInt(stream);
        this.arrowInactive = ConfigManager.readInt(stream);
        this.borderPanel = ConfigManager.readInt(stream);
        this.borderSubtle = ConfigManager.readInt(stream);
        this.border = ConfigManager.readInt(stream);
        this.trackOff = ConfigManager.readInt(stream);
        this.overlayDim = ConfigManager.readInt(stream);
        this.pendingApply = true;
    }

    public void resetDefaults() {
        GUIStyle defaults = GUIStyle.defaultStyle();
        this.ruby = defaults.ruby();
        this.rubyHover = defaults.rubyHover();
        this.rubyBg = defaults.rubyBg();
        this.rubyActive = defaults.rubyActive();
        this.bgPanel = defaults.bgPanel();
        this.bgBase = defaults.bgBase();
        this.bgHover = defaults.bgHover();
        this.bgElevated = defaults.bgElevated();
        this.text = defaults.text();
        this.textBright = defaults.textBright();
        this.textMuted = defaults.textMuted();
        this.catInactive = defaults.catInactive();
        this.arrowInactive = defaults.arrowInactive();
        this.borderPanel = defaults.borderPanel();
        this.borderSubtle = defaults.borderSubtle();
        this.border = defaults.border();
        this.trackOff = defaults.trackOff();
        this.overlayDim = defaults.overlayDim();
        this.apply();
    }

    private File themeFile() {
        return new File(RubyClient.client.runDirectory, "config/ruby/theme.json");
    }

    public void setColor(ThemeColor color, int value) {
        color.setter().accept(value);
        this.apply();
    }

    public record ThemeColor(String name, IntSupplier getter, IntConsumer setter, int defaultValue) {
        public int get() {
            return this.getter.getAsInt();
        }
    }

    private static class ThemeData {
        @SerializedName("ruby") Integer ruby;
        @SerializedName("rubyHover") Integer rubyHover;
        @SerializedName("rubyBg") Integer rubyBg;
        @SerializedName("rubyActive") Integer rubyActive;
        @SerializedName("bgPanel") Integer bgPanel;
        @SerializedName("bgBase") Integer bgBase;
        @SerializedName("bgHover") Integer bgHover;
        @SerializedName("bgElevated") Integer bgElevated;
        @SerializedName("text") Integer text;
        @SerializedName("textBright") Integer textBright;
        @SerializedName("textMuted") Integer textMuted;
        @SerializedName("catInactive") Integer catInactive;
        @SerializedName("arrowInactive") Integer arrowInactive;
        @SerializedName("borderPanel") Integer borderPanel;
        @SerializedName("borderSubtle") Integer borderSubtle;
        @SerializedName("border") Integer border;
        @SerializedName("trackOff") Integer trackOff;
        @SerializedName("overlayDim") Integer overlayDim;
    }

    private static final class Holder {
        private static final ThemeManager INSTANCE = new ThemeManager();
    }
}
