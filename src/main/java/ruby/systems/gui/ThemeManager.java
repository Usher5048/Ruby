package ruby.systems.gui;

import ruby.systems.config.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import ruby.systems.config.ColorValue;

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
    private int borderPanel = 0xFF221C1E;
    private int borderSubtle = 0xFF121012;
    private int border = 0xFF181416;
    private int trackOff = 0xFF1A1617;
    private int overlayDim = 0x88000000;
    private int scrollbar = 0xFF4A4648;

    private List<ThemeColor> colorRows;
    private List<ThemeGroup> themeGroups;
    private List<ColorValue> palette;
    private int paletteVersion;
    private static final int PALETTE = 1;
    /** Bumped when the catalog is regrouped so a hotswap rebuilds the cached list. */
    private int catalogVersion;
    private static final int CATALOG = 2;

    private ThemeManager() {}

    public static ThemeManager get() {
        return Holder.INSTANCE;
    }

    public List<ThemeColor> colors() {
        this.ensureCatalog();
        return this.colorRows;
    }

    public List<ThemeGroup> groups() {
        this.ensureCatalog();
        return this.themeGroups;
    }

    private void ensureCatalog() {
        if (this.colorRows != null && this.themeGroups != null && this.catalogVersion == CATALOG) return;
        this.catalogVersion = CATALOG;
        List<ThemeColor> list = new ArrayList<>();
        List<ThemeGroup> groups = new ArrayList<>();
        groups.add(this.bundle(list, "Accent",
                new ThemeColor("Ruby", () -> this.ruby, v -> this.ruby = v, ThemeManager.DEFAULT_RUBY),
                new ThemeColor("Hover", () -> this.rubyHover, v -> this.rubyHover = v, ThemeManager.DEFAULT_RUBY_HOVER),
                new ThemeColor("Background", () -> this.rubyBg, v -> this.rubyBg = v, 0xFF0F0C0D),
                new ThemeColor("Active", () -> this.rubyActive, v -> this.rubyActive = v, 0xFF141012)));
        groups.add(this.bundle(list, "Surfaces",
                new ThemeColor("Panel", () -> this.bgPanel, v -> this.bgPanel = v, 0xFF090809),
                new ThemeColor("Base", () -> this.bgBase, v -> this.bgBase = v, 0xFF050405),
                new ThemeColor("Hover", () -> this.bgHover, v -> this.bgHover = v, 0xFF121012),
                new ThemeColor("Raised", () -> this.bgElevated, v -> this.bgElevated = v, 0xFF0E0C0D)));
        groups.add(this.bundle(list, "Text",
                new ThemeColor("Primary", () -> this.text, v -> this.text = v, 0xFFB8B4B5),
                new ThemeColor("Bright", () -> this.textBright, v -> this.textBright = v, 0xFFD4D0D1),
                new ThemeColor("Muted", () -> this.textMuted, v -> this.textMuted = v, 0xFF5A5557),
                new ThemeColor("Category", () -> this.catInactive, v -> this.catInactive = v, 0xFF8A8688),
                new ThemeColor("Arrow", () -> this.arrowInactive, v -> this.arrowInactive = v, 0xFF333333)));
        groups.add(this.bundle(list, "Borders",
                new ThemeColor("Panel", this::panelBorder, v -> this.borderPanel = v, 0xFF221C1E),
                new ThemeColor("Divider", () -> this.borderSubtle, v -> this.borderSubtle = v, 0xFF121012),
                new ThemeColor("Control", () -> this.border, v -> this.border = v, 0xFF181416)));
        groups.add(this.bundle(list, "Chrome",
                new ThemeColor("Toggle", () -> this.trackOff, v -> this.trackOff = v, 0xFF1A1617),
                new ThemeColor("Dim", () -> this.overlayDim, v -> this.overlayDim = v, 0x88000000),
                new ThemeColor("Scrollbar", this::scrollbarColor, v -> this.scrollbar = v, 0xFF4A4648)));
        this.colorRows = list;
        this.themeGroups = groups;
    }

    private ThemeGroup bundle(List<ThemeColor> into, String name, ThemeColor... colors) {
        List<ThemeColor> group = new ArrayList<>();
        for (ThemeColor color : colors) {
            into.add(color);
            group.add(color);
        }
        return new ThemeGroup(name, group);
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
                this.scrollbarColor(),
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
        ConfigManager.writeInt(stream, this.panelBorder());
        ConfigManager.writeInt(stream, this.trackOff);
        ConfigManager.writeInt(stream, this.overlayDim);
        ConfigManager.writeInt(stream, this.scrollbarColor());
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
        if (stream.available() >= 4) this.scrollbar = ConfigManager.readInt(stream);
        this.syncPalette();
    }

    public void resetDefaults() {
        GUIStyle defaults = GUIStyle.defaultStyle();
        this.ruby = defaults.ruby();
        this.assignDefaults();
        this.apply();
        this.syncPalette();
    }

    /** Restores the built-in palette without touching fonts. Profile load applies it later. */
    public void assignDefaults() {
        this.ruby = ThemeManager.DEFAULT_RUBY;
        this.rubyHover = ThemeManager.DEFAULT_RUBY_HOVER;
        this.rubyBg = 0xFF0F0C0D;
        this.rubyActive = 0xFF141012;
        this.bgPanel = 0xFF090809;
        this.bgBase = 0xFF050405;
        this.bgHover = 0xFF121012;
        this.bgElevated = 0xFF0E0C0D;
        this.text = 0xFFB8B4B5;
        this.textBright = 0xFFD4D0D1;
        this.textMuted = 0xFF5A5557;
        this.catInactive = 0xFF8A8688;
        this.arrowInactive = 0xFF333333;
        this.borderPanel = 0xFF221C1E;
        this.borderSubtle = 0xFF121012;
        this.border = 0xFF181416;
        this.trackOff = 0xFF1A1617;
        this.overlayDim = 0x88000000;
        this.scrollbar = 0xFF4A4648;
    }

    /**
     * One control per family. Hover, raised surfaces, and the quiet borders follow
     * the color the user actually picks.
     */
    public List<ColorValue> palette() {
        if (this.palette != null && this.paletteVersion == PALETTE) return this.palette;
        this.paletteVersion = PALETTE;
        List<ColorValue> list = new ArrayList<>();
        list.add(this.color("Accent", this.ruby, this::applyAccent));
        list.add(this.color("Selection", this.rubyBg, this::applySelection));
        list.add(this.color("Panel", this.bgPanel, this::applyPanel));
        list.add(this.color("Background", this.bgBase, this::applyBackground));
        list.add(this.color("Text", this.text, this::applyText));
        list.add(this.color("Muted", this.textMuted, this::applyMuted));
        list.add(this.color("Border", this.panelBorder(), this::applyBorder));
        list.add(this.color("Scrollbar", this.scrollbarColor(), this::applyScrollbar));
        this.palette = list;
        return list;
    }

    private ColorValue color(String name, int current, IntConsumer apply) {
        return (ColorValue) new ColorValue.Builder(name)
                .defaultValue(current)
                .changed(value -> {
                    apply.accept(value);
                    this.apply();
                })
                .build();
    }

    private void syncPalette() {
        if (this.palette == null || this.paletteVersion != PALETTE) return;
        int[] now = {
                this.ruby, this.rubyBg, this.bgPanel, this.bgBase,
                this.text, this.textMuted, this.panelBorder(), this.scrollbarColor()
        };
        for (int i = 0; i < now.length && i < this.palette.size(); i++) {
            this.palette.get(i).setQuiet(now[i] | 0xFF000000);
        }
    }

    public void setDimPercent(int percent) {
        this.writeDim(percent);
        this.apply();
    }

    /** Profile load. The style is applied later, once fonts exist. */
    public void setDimQuiet(int percent) {
        this.writeDim(percent);
    }

    private void writeDim(int percent) {
        int alpha = Math.round(Math.clamp(percent, 0, 100) * 255f / 100f);
        this.overlayDim = (alpha << 24) | (this.overlayDim & 0x00FFFFFF);
    }

    public int dimPercent() {
        return Math.round(((this.overlayDim >>> 24) & 0xFF) * 100f / 255f);
    }

    private void applyAccent(int value) {
        this.ruby = value | 0xFF000000;
        this.rubyHover = ThemeManager.lighten(this.ruby, 0.2f);
    }

    private void applySelection(int value) {
        this.rubyBg = value | 0xFF000000;
        this.rubyActive = ThemeManager.lighten(this.rubyBg, 0.08f);
    }

    private void applyPanel(int value) {
        this.bgPanel = value | 0xFF000000;
    }

    private void applyBackground(int value) {
        this.bgBase = value | 0xFF000000;
        this.bgElevated = ThemeManager.lighten(this.bgBase, 0.05f);
        this.bgHover = ThemeManager.lighten(this.bgBase, 0.1f);
    }

    private void applyText(int value) {
        this.text = value | 0xFF000000;
        this.textBright = ThemeManager.lighten(this.text, 0.28f);
    }

    private void applyMuted(int value) {
        this.textMuted = value | 0xFF000000;
        this.catInactive = ThemeManager.lighten(this.textMuted, 0.42f);
        this.arrowInactive = ThemeManager.darken(this.textMuted, 0.45f);
    }

    private void applyBorder(int value) {
        this.borderPanel = value | 0xFF000000;
        this.border = ThemeManager.darken(this.borderPanel, 0.18f);
        this.borderSubtle = ThemeManager.darken(this.borderPanel, 0.42f);
    }

    private void applyScrollbar(int value) {
        this.scrollbar = value | 0xFF000000;
        this.trackOff = ThemeManager.darken(this.scrollbar, 0.68f);
    }

    private static int lighten(int color, float t) {
        return Motion.mix(color | 0xFF000000, 0xFFFFFFFF, t);
    }

    private static int darken(int color, float t) {
        return Motion.mix(color | 0xFF000000, 0xFF000000, t);
    }

    /** Older outlines were either invisible (0x06FFFFFF) or louder than the panel (0xFF3E383A). */
    private int panelBorder() {
        if (this.borderPanel == 0x06FFFFFF || this.borderPanel == 0xFF3E383A) this.borderPanel = 0xFF221C1E;
        return this.borderPanel;
    }

    /** Hotswap and older profiles leave this at 0. The picker always writes an opaque color. */
    private int scrollbarColor() {
        if (this.scrollbar == 0) this.scrollbar = 0xFF4A4648;
        return this.scrollbar;
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

    public record ThemeGroup(String name, List<ThemeColor> colors) {}

    private static final class Holder {
        private static final ThemeManager INSTANCE = new ThemeManager();
    }
}
