package ruby.systems.modules.player;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ProjectileItem;
import ruby.RubyClient;
import ruby.helpers.RandomUtils;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.StringListValue;
import ruby.systems.events.Events;
import ruby.systems.events.client.UseCooldownEvent;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;
import ruby.systems.modules.ModuleType;

/**
 * LiquidBounce {@code FastPlace} port.
 */
public class FastPlace extends Module {
    public enum ApplyTo {
        Blocks,
        Projectiles;

        static boolean matches(String tag, Item item) {
            ApplyTo type = fromTag(tag);
            if (type == null) return false;
            return switch (type) {
                case Blocks -> item instanceof BlockItem;
                case Projectiles -> item instanceof ProjectileItem;
            };
        }

        static ApplyTo fromTag(String tag) {
            for (ApplyTo value : values()) {
                if (value.name().equalsIgnoreCase(tag.replace(" ", ""))) return value;
            }
            return null;
        }
    }

    private final IntegerValue cooldownMin;
    private final IntegerValue cooldownMax;
    private final StringListValue applyTo;
    private final IntegerValue startDelay;

    public FastPlace() {
        super("Fast Place", "Allows you to place blocks faster.", ModuleType.PLAYER);

        cooldownMin = config.create(new IntegerValue.Builder("Cooldown Min")
                .description("Minimum item use cooldown in ticks.")
                .range(0, 4)
                .defaultValue(0)
                .build());
        cooldownMax = config.create(new IntegerValue.Builder("Cooldown Max")
                .description("Maximum item use cooldown in ticks.")
                .range(0, 4)
                .defaultValue(0)
                .build());
        applyTo = config.create(new StringListValue.Builder("Apply To")
                .description("Item types affected by fast place.")
                .defaultValue("Blocks")
                .build());
        startDelay = config.create(new IntegerValue.Builder("Start Delay")
                .description("Milliseconds since use key was pressed before activating.")
                .range(0, 1000)
                .defaultValue(0)
                .build());

        Events.USE_COOLDOWN.register(FastPlace::onUseCooldown);
    }

    private static void onUseCooldown(UseCooldownEvent event) {
        FastPlace fastPlace = Modules.getByClass(FastPlace.class);
        if (fastPlace == null || !fastPlace.enabled()) return;
        fastPlace.applyCooldown(event);
    }

    private void applyCooldown(UseCooldownEvent event) {
        var player = RubyClient.client.player;
        if (player == null) return;

        Item mainHand = player.getMainHandStack().getItem();
        Item offHand = player.getOffHandStack().getItem();
        boolean matches = applyTo.value().stream()
                .anyMatch(tag -> ApplyTo.matches(tag, mainHand) || ApplyTo.matches(tag, offHand));
        if (!matches) return;

        if (startDelay.value() > 0 && !RubyClient.client.options.useKey.isPressed()) return;

        event.setCooldown(RandomUtils.randomInt(cooldownMin.value(), cooldownMax.value()));
    }
}
