package ruby.systems.modules.render;

import net.minecraft.block.Blocks;
import ruby.RubyClient;
import ruby.systems.config.BlockListValue;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

// TODO: Add connected textures for glass, bookshelves, etc.
public class TextureTweaks extends Module {
    public enum WrapMode {
        SINGLE_SIDE,
        FULL_BLOCK
    }

    public final BooleanValue wrapEnabled = this.config.create(new BooleanValue.Builder("Texture Wrapping")
            .description("Whether or not to wrap side textures of some blocks, similar to OptiFine's better grass")
            .changed(v -> RubyClient.client.worldRenderer.reload())
            .defaultValue(true)
            .build());

    public final EnumValue<WrapMode> wrapMode = this.config.create(new EnumValue.Builder<WrapMode>("Wrap Mode")
            .description("How to wrap the textures on blocks")
            .defaultValue(WrapMode.SINGLE_SIDE)
            .visible(this.wrapEnabled::value)
            .changed(v -> RubyClient.client.worldRenderer.reload())
            .build());

    public final BlockListValue wrappedBlocks = this.config.create(new BlockListValue.Builder("Wrapped Blocks")
            .description("Which blocks to wrap the side textures of")
            .visible(this.wrapEnabled::value)
            .defaultValue(
                    Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM,
                    Blocks.WARPED_NYLIUM, Blocks.CRIMSON_NYLIUM, Blocks.DIRT_PATH
            ).build());

    public TextureTweaks() {
        super("Texture Tweaks", "Tweaks the textures used for some blocks", ModuleType.RENDER);
    }

    @Override public void onEnable() {
        RubyClient.client.worldRenderer.reload();
    }
    @Override public void onDisable() {
        RubyClient.client.worldRenderer.reload();
    }
}
