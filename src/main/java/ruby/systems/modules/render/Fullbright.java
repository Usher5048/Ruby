package ruby.systems.modules.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's Fullbright modes:
 * - Potion: Applies Night Vision effect (works without mixins)
 * - Gamma: Sets gamma to max (requires mixin to override options — not yet implemented)
 * - Luminance: Client-side max light level (requires world renderer mixin — not yet implemented)
 */
public class Fullbright extends Module {
    public enum Mode { Potion, Gamma }

    private final EnumValue<Mode> mode;

    public Fullbright() {
        super("Fullbright", "Makes everything fully bright.", ModuleCategory.RENDER);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Method to apply fullbright.")
                .defaultValue(Mode.Potion)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (mode.value() == Mode.Potion) {
            // Apply night vision effect (duration high enough to never flicker)
            StatusEffectInstance nightVision = new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, 260, 0, false, false, false
            );
            player.addStatusEffect(nightVision);
        }
        // Gamma mode would require a mixin to override GameOptions.getGamma()
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mode.value() == Mode.Potion) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }
}
