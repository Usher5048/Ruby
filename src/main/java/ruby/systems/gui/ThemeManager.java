package ruby.systems.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public final class ThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        ConfigManager.writeIntPublic(stream, this.ruby);
        ConfigManager.writeIntPublic(stream, this.rubyHover);
        ConfigManager.writeIntPublic(stream, this.rubyBg);
        ConfigManager.writeIntPublic(stream, this.rubyActive);
        ConfigManager.writeIntPublic(stream, this.bgPanel);
        ConfigManager.writeIntPublic(stream, this.bgBase);
        ConfigManager.writeIntPublic(stream, this.bgHover);
        ConfigManager.writeIntPublic(stream, this.bgElevated);
        ConfigManager.writeIntPublic(stream, this.text);
        ConfigManager.writeIntPublic(stream, this.textBright);
        ConfigManager.writeIntPublic(stream, this.textMuted);
        ConfigManager.writeIntPublic(stream, this.catInactive);
        ConfigManager.writeIntPublic(stream, this.arrowInactive);
        ConfigManager.writeIntPublic(stream, this.borderPanel);
        ConfigManager.writeIntPublic(stream, this.borderSubtle);
        ConfigManager.writeIntPublic(stream, this.border);
        ConfigManager.writeIntPublic(stream, this.trackOff);
        ConfigManager.writeIntPublic(stream, this.overlayDim);
    }

    public void readFromProfile(ByteArrayInputStream stream) {
        this.ruby = ConfigManager.readIntPublic(stream);
        this.rubyHover = ConfigManager.readIntPublic(stream);
        this.rubyBg = ConfigManager.readIntPublic(stream);
        this.rubyActive = ConfigManager.readIntPublic(stream);
        this.bgPanel = ConfigManager.readIntPublic(stream);
        this.bgBase = ConfigManager.readIntPublic(stream);
        this.bgHover = ConfigManager.readIntPublic(stream);
        this.bgElevated = ConfigManager.readIntPublic(stream);
        this.text = ConfigManager.readIntPublic(stream);
        this.textBright = ConfigManager.readIntPublic(stream);
        this.textMuted = ConfigManager.readIntPublic(stream);
        this.catInactive = ConfigManager.readIntPublic(stream);
        this.arrowInactive = ConfigManager.readIntPublic(stream);
        this.borderPanel = ConfigManager.readIntPublic(stream);
        this.borderSubtle = ConfigManager.readIntPublic(stream);
        this.border = ConfigManager.readIntPublic(stream);
        this.trackOff = ConfigManager.readIntPublic(stream);
        this.overlayDim = ConfigManager.readIntPublic(stream);
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
        this.save();
    }

    public void load() {
        File file = this.themeFile();
        if (!file.isFile()) {
            this.pendingApply = true;
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            ThemeData data = ThemeManager.GSON.fromJson(reader, ThemeData.class);
            if (data == null) {
                this.pendingApply = true;
                return;
            }
            if (data.ruby != null) this.ruby = data.ruby;
            if (data.rubyHover != null) this.rubyHover = data.rubyHover;
            if (data.rubyBg != null) this.rubyBg = data.rubyBg;
            if (data.rubyActive != null) this.rubyActive = data.rubyActive;
            if (data.bgPanel != null) this.bgPanel = data.bgPanel;
            if (data.bgBase != null) this.bgBase = data.bgBase;
            if (data.bgHover != null) this.bgHover = data.bgHover;
            if (data.bgElevated != null) this.bgElevated = data.bgElevated;
            if (data.text != null) this.text = data.text;
            if (data.textBright != null) this.textBright = data.textBright;
            if (data.textMuted != null) this.textMuted = data.textMuted;
            if (data.catInactive != null) this.catInactive = data.catInactive;
            if (data.arrowInactive != null) this.arrowInactive = data.arrowInactive;
            if (data.borderPanel != null) this.borderPanel = data.borderPanel;
            if (data.borderSubtle != null) this.borderSubtle = data.borderSubtle;
            if (data.border != null) this.border = data.border;
            if (data.trackOff != null) this.trackOff = data.trackOff;
            if (data.overlayDim != null) this.overlayDim = data.overlayDim;
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to load theme", e);
        }

        this.pendingApply = true;
    }

    public void save() {
        File file = this.themeFile();
        file.getParentFile().mkdirs();

        ThemeData data = new ThemeData();
        data.ruby = this.ruby;
        data.rubyHover = this.rubyHover;
        data.rubyBg = this.rubyBg;
        data.rubyActive = this.rubyActive;
        data.bgPanel = this.bgPanel;
        data.bgBase = this.bgBase;
        data.bgHover = this.bgHover;
        data.bgElevated = this.bgElevated;
        data.text = this.text;
        data.textBright = this.textBright;
        data.textMuted = this.textMuted;
        data.catInactive = this.catInactive;
        data.arrowInactive = this.arrowInactive;
        data.borderPanel = this.borderPanel;
        data.borderSubtle = this.borderSubtle;
        data.border = this.border;
        data.trackOff = this.trackOff;
        data.overlayDim = this.overlayDim;

        try (FileWriter writer = new FileWriter(file)) {
            ThemeManager.GSON.toJson(data, writer);
        } catch (Exception e) {
            RubyClient.LOGGER.error("Failed to save theme", e);
        }
    }

    private File themeFile() {
        return new File(RubyClient.client.runDirectory, "config/ruby/theme.json");
    }

    public void setColor(ThemeColor color, int value) {
        color.setter().accept(value);
        this.apply();
        this.save();
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
