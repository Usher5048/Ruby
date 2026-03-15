package ruby.systems.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActiveModules extends Module {

    public enum SortMode { Longest, Shortest, Alphabetical }
    public enum ColorMode { Static, Category, Rainbow }

    private final EnumValue<SortMode> sortMode;
    private final EnumValue<ColorMode> colorMode;
    private final BooleanValue background;
    private final IntegerValue rainbowSpeed;

    public ActiveModules() {
        super("ActiveModules", "Displays active modules on the HUD.", ModuleCategory.RENDER);

        sortMode = config.create(new EnumValue.Builder<SortMode>("Sort")
                .description("How to sort the module list.")
                .defaultValue(SortMode.Longest)
                .build());

        colorMode = config.create(new EnumValue.Builder<ColorMode>("Color")
                .description("Coloring mode for module names.")
                .defaultValue(ColorMode.Static)
                .build());

        background = config.create(new BooleanValue.Builder("Background")
                .description("Draw a background behind each module name.")
                .defaultValue(true)
                .build());

        rainbowSpeed = config.create(new IntegerValue.Builder("Rainbow Speed")
                .description("Speed of the rainbow color cycle.")
                .defaultValue(10)
                .range(1, 30)
                .build());
    }

    @Override
    public void onRender2D(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        TextRenderer font = mc.textRenderer;
        int screenWidth = mc.getWindow().getScaledWidth();

        List<Module> active = new ArrayList<>();
        for (Module m : Modules.getActiveModules()) {
            if (m == this) continue;
            active.add(m);
        }
        if (active.isEmpty()) return;

        switch (sortMode.value()) {
            case Longest -> active.sort(Comparator.comparingInt((Module m) -> font.getWidth(m.name())).reversed());
            case Shortest -> active.sort(Comparator.comparingInt(m -> font.getWidth(m.name())));
            case Alphabetical -> active.sort(Comparator.comparing(Module::name));
        }

        int y = 2;
        int lineHeight = font.fontHeight + 1;

        for (int i = 0; i < active.size(); i++) {
            Module m = active.get(i);
            String name = m.name();
            int textWidth = font.getWidth(name);
            int x = screenWidth - textWidth - 4;
            int color = getColor(m, i);

            if (background.value()) {
                context.fill(x - 2, y, screenWidth - 1, y + lineHeight, 0x60000000);
                context.fill(screenWidth - 1, y, screenWidth, y + lineHeight, color);
            }

            context.drawTextWithShadow(font, name, x, y + 1, color);
            y += lineHeight;
        }
    }

    private int getColor(Module module, int index) {
        return switch (colorMode.value()) {
            case Static -> 0xFFCC3344;
            case Category -> getCategoryColor(module.category());
            case Rainbow -> getRainbowColor(index);
        };
    }

    private int getCategoryColor(ModuleCategory category) {
        return switch (category) {
            case COMBAT -> 0xFFE55561;
            case MOVEMENT -> 0xFF55C4E5;
            case PLAYER -> 0xFF55E57E;
            case RENDER -> 0xFFE5A855;
            case WORLD -> 0xFFD4D455;
            case EXPLOIT -> 0xFFD455C4;
            case MISC -> 0xFFBBBBBB;
        };
    }

    private int getRainbowColor(int index) {
        float hue = (float) ((System.currentTimeMillis() % 10000) / 10000.0
                + index * 0.04 * (rainbowSpeed.value() / 10.0));
        hue %= 1.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.55f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }
}
