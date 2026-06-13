package ruby.systems.modules.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import ruby.RubyClient;
import ruby.mixin.ClientPlayerInteractionManagerAccessor;
import ruby.systems.config.BlockListValue;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Speed mine ported from Meteor Client.
 */
public class SpeedMine extends Module {

    public enum Mode { Normal, Haste, Damage }
    public enum ListMode { Whitelist, Blacklist }

    public final EnumValue<Mode> mode;
    private final BlockListValue blocks;
    private final EnumValue<ListMode> blocksFilter;
    public final DoubleValue modifier;
    private final IntegerValue hasteAmplifier;
    private final BooleanValue instamine;

    public SpeedMine() {
        super("Speed Mine", "Allows you to quickly mine blocks.", ModuleType.PLAYER);

        mode = config.create(new EnumValue.Builder<Mode>("Mode").defaultValue(Mode.Damage).build());
        blocks = config.create(new BlockListValue.Builder("Blocks")
                .defaultValue(java.util.List.of())
                .visible(() -> mode.value() != Mode.Haste).build());
        blocksFilter = config.create(new EnumValue.Builder<ListMode>("Blocks Filter")
                .visible(() -> mode.value() != Mode.Haste).defaultValue(ListMode.Blacklist).build());
        modifier = config.create(new DoubleValue.Builder("Modifier")
                .description("Mining speed modifier for Normal mode.")
                .defaultValue(1.4).range(0, 5, 0.1)
                .visible(() -> mode.value() == Mode.Normal).build());
        hasteAmplifier = config.create(new IntegerValue.Builder("Haste Amplifier")
                .range(1, 5).defaultValue(2)
                .visible(() -> mode.value() == Mode.Haste).build());
        instamine = config.create(new BooleanValue.Builder("Instamine")
                .defaultValue(true).visible(() -> mode.value() == Mode.Damage).build());
    }

    @Override
    public void onDisable() {
        removeHaste();
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null || RubyClient.client.world == null) return;

        if (mode.value() == Mode.Haste) {
            StatusEffectInstance haste = RubyClient.client.player.getStatusEffect(StatusEffects.HASTE);
            if (haste == null || haste.getAmplifier() <= hasteAmplifier.value() - 1) {
                RubyClient.client.player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.HASTE, -1, hasteAmplifier.value() - 1, false, false, false
                ));
            }
        } else if (mode.value() == Mode.Damage && RubyClient.client.interactionManager != null) {
            ClientPlayerInteractionManagerAccessor accessor =
                    (ClientPlayerInteractionManagerAccessor) RubyClient.client.interactionManager;
            BlockPos pos = accessor.ruby$getBreakingPos();
            if (pos == null) return;

            float progress = accessor.ruby$getBreakingProgress();
            if (progress <= 0) return;

            BlockState state = RubyClient.client.world.getBlockState(pos);
            if (progress + state.calcBlockBreakingDelta(RubyClient.client.player, RubyClient.client.world, pos) >= 0.7f) {
                accessor.ruby$setBreakingProgress(1f);
            }
        }
    }

    private void removeHaste() {
        if (RubyClient.client.player == null) return;
        StatusEffectInstance haste = RubyClient.client.player.getStatusEffect(StatusEffects.HASTE);
        if (haste != null && !haste.shouldShowIcon()) {
            RubyClient.client.player.removeStatusEffect(StatusEffects.HASTE);
        }
    }

    public boolean filter(Block block) {
        if (blocksFilter.value() == ListMode.Blacklist) return !blocks.value().contains(block);
        return blocks.value().contains(block);
    }

    public boolean instamine() {
        return enabled() && mode.value() == Mode.Damage && instamine.value();
    }
}
