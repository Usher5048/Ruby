package ruby.systems.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import ruby.RubyClient;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

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
    private double originalGamma;

    public Fullbright() {
        super("Fullbright", "Makes everything fully bright.", ModuleType.RENDER);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Method to apply fullbright.")
                .defaultValue(Mode.Potion)
                .build());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (mode.value() == Mode.Potion) {
            // Apply night vision effect (duration high enough to never flicker)
            MobEffectInstance nightVision = new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 260, 0, false, false, false
            );
            player.addEffect(nightVision);
            return;
        }

        this.originalGamma = RubyClient.client.options.gamma().get();
        RubyClient.client.options.gamma().set(100d);
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mode.value() == Mode.Potion) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
            return;
        }

        RubyClient.client.options.gamma().set(this.originalGamma);
    }
}
