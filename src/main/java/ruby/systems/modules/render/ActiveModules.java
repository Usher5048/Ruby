package ruby.systems.modules.render;

import net.minecraft.client.MinecraftClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.ColorValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.StringListValue;
import ruby.systems.events.Render2DEvent;
import ruby.systems.gui.GUIStyle;
import ruby.systems.hud.HudAlignment;
import ruby.systems.hud.HudBox;
import ruby.systems.hud.HudRenderer;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActiveModules extends Module {

    public enum Sort { Alphabetical, Biggest, Smallest }
    public enum ColorMode { Flat, Random, Rainbow }

    private static final int INFO_COLOR = 0xFFafafaf;
    private static final double ROW_SPACING = 0.82;

    private static final double RAINBOW_SPEED = 0.05;
    private static final double RAINBOW_SPREAD = 0.01;
    private static final float RAINBOW_SATURATION = 1.0f;
    private static final float RAINBOW_BRIGHTNESS = 1.0f;

    private final EnumValue<Sort> sort;
    private final StringListValue hiddenModules;
    private final BooleanValue activeInfo;
    private final BooleanValue showKeybind;
    private final BooleanValue shadow;
    private final EnumValue<ColorMode> colorMode;
    private final ColorValue flatColor;
    private final ColorValue keybindColor;

    private final HudBox box = new HudBox();
    private final List<Module> modules = new ArrayList<>();

    private double rainbowHue1;
    private double rainbowHue2;
    private double emptySpace;

    public ActiveModules() {
        super("Active Modules", "Displays your active modules.", ModuleType.RENDER);

        this.sort = this.config.create(new EnumValue.Builder<Sort>("Sort")
                .description("How to sort active modules.")
                .defaultValue(Sort.Biggest)
                .build());

        this.hiddenModules = this.config.create(new StringListValue.Builder("Hidden Modules")
                .description("Module names excluded from the list.")
                .defaultValue(List.of())
                .build());

        this.activeInfo = this.config.create(new BooleanValue.Builder("Module Info")
                .description("Shows info from the module next to the name.")
                .defaultValue(true)
                .build());

        this.showKeybind = this.config.create(new BooleanValue.Builder("Show Keybind")
                .description("Shows the module keybind next to its name.")
                .defaultValue(false)
                .build());

        this.shadow = this.config.create(new BooleanValue.Builder("Shadow")
                .description("Renders shadow behind text.")
                .defaultValue(true)
                .build());

        this.colorMode = this.config.create(new EnumValue.Builder<ColorMode>("Color Mode")
                .description("Color used for active module names.")
                .defaultValue(ColorMode.Rainbow)
                .build());

        this.flatColor = this.config.create(new ColorValue.Builder("Flat Color")
                .description("Color used when color mode is Flat.")
                .defaultValue(0xFFE11919)
                .visible(() -> this.colorMode.value() == ColorMode.Flat)
                .build());

        this.keybindColor = this.config.create(new ColorValue.Builder("Keybind Color")
                .description("Color used for keybind labels.")
                .defaultValue(0xFFafafaf)
                .visible(this.showKeybind::value)
                .build());
    }

    @Override
    public void render2D(Render2DEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        HudRenderer renderer = HudRenderer.INSTANCE;
        double delta = event.getTickCounter().getDynamicDeltaTicks() / 20.0;

        this.collectModules();
        if (this.modules.isEmpty()) return;

        renderer.begin(event.getContext(), GUIStyle.get().bodyFont(), delta);

        this.emptySpace = renderer.textWidth(" ", this.shadow.value(), 1.0);
        this.sortModules(renderer);
        this.updateBoxSize(renderer);
        this.box.setPos(0, 0);
        var context = event.getContext();
        this.renderList(renderer, this.box.getRenderX(context), this.box.getRenderY(context));
    }

    private void collectModules() {
        this.modules.clear();
        for (Module module : Modules.getActiveModules()) {
            if (module == this) continue;
            if (this.hiddenModules.value().contains(module.name())) continue;
            this.modules.add(module);
        }
    }

    private void sortModules(HudRenderer renderer) {
        this.modules.sort(switch (this.sort.value()) {
            case Alphabetical -> Comparator.comparing(Module::name);
            case Biggest -> Comparator.comparingDouble((Module m) -> this.getModuleWidth(renderer, m)).reversed();
            case Smallest -> Comparator.comparingDouble(m -> this.getModuleWidth(renderer, m));
        });
    }

    private double rowHeight(HudRenderer renderer) {
        return renderer.textHeight(this.shadow.value(), 1.0) * ROW_SPACING;
    }

    private void updateBoxSize(HudRenderer renderer) {
        double width = 0;
        double rowH = this.rowHeight(renderer);
        for (Module module : this.modules) {
            width = Math.max(width, this.getModuleWidth(renderer, module));
        }
        this.box.setSize(width, rowH * this.modules.size());
    }

    private void renderList(HudRenderer renderer, double x, double y) {
        this.rainbowHue1 += RAINBOW_SPEED * renderer.delta;
        if (this.rainbowHue1 > 1) this.rainbowHue1 -= 1;
        else if (this.rainbowHue1 < -1) this.rainbowHue1 += 1;
        this.rainbowHue2 = this.rainbowHue1;

        double rowH = this.rowHeight(renderer);
        for (Module module : this.modules) {
            double rowWidth = this.getModuleWidth(renderer, module);
            double offset = this.box.alignX(rowWidth, HudAlignment.Auto);
            this.renderModule(renderer, module, x + offset, y);
            y += rowH;
        }
    }

    private void renderModule(HudRenderer renderer, Module module, double x, double y) {
        int color = this.moduleColor(module);
        boolean shadow = this.shadow.value();

        renderer.text(module.name(), x, y, color, shadow, 1.0);
        double textLength = renderer.textWidth(module.name(), shadow, 1.0);

        if (this.showKeybind.value() && !module.keybind.isUnbound()) {
            String keybindStr = " [" + module.keybind + "]";
            renderer.text(keybindStr, x + textLength, y, this.keybindColor.opaque(), shadow, 1.0);
            textLength += renderer.textWidth(keybindStr, shadow, 1.0);
        }

        if (this.activeInfo.value()) {
            String info = module.getInfoString();
            if (info != null) {
                renderer.text(info, x + textLength + this.emptySpace, y, INFO_COLOR, shadow, 1.0);
            }
        }
    }

    private int moduleColor(Module module) {
        return switch (this.colorMode.value()) {
            case Flat -> this.flatColor.opaque();
            case Random -> module.hudColor();
            case Rainbow -> this.nextRainbowColor();
        };
    }

    private double getModuleWidth(HudRenderer renderer, Module module) {
        boolean shadow = this.shadow.value();
        double width = renderer.textWidth(module.name(), shadow, 1.0);

        if (this.showKeybind.value() && !module.keybind.isUnbound()) {
            width += renderer.textWidth(" [" + module.keybind + "]", shadow, 1.0);
        }

        if (this.activeInfo.value()) {
            String info = module.getInfoString();
            if (info != null) {
                width += this.emptySpace + renderer.textWidth(info, shadow, 1.0);
            }
        }

        return width;
    }

    private int nextRainbowColor() {
        this.rainbowHue2 += RAINBOW_SPREAD;
        int rgb = java.awt.Color.HSBtoRGB(
                (float) this.rainbowHue2,
                RAINBOW_SATURATION,
                RAINBOW_BRIGHTNESS
        );
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }
}
