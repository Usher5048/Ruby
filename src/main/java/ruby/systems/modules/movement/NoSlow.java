package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Prevents various blocks and actions from slowing the player down.
 * Simplified tick-based version that counteracts slowdowns by
 * maintaining sprint state and removing slowdown effects.
 */
public class NoSlow extends Module {

    private final BooleanValue items;
    private final BooleanValue soulSand;
    private final BooleanValue berryBush;
    private final BooleanValue honeyBlock;

    public NoSlow() {
        super("No Slow", "Allows you to move normally when using items that slow you.", ModuleType.MOVEMENT);

        items = config.create(new BooleanValue.Builder("Items")
                .description("Prevents item use from slowing you.")
                .defaultValue(true)
                .build());

        soulSand = config.create(new BooleanValue.Builder("Soul Sand")
                .description("Prevents soul sand from slowing you.")
                .defaultValue(true)
                .build());

        berryBush = config.create(new BooleanValue.Builder("Berry Bush")
                .description("Prevents berry bushes from slowing you.")
                .defaultValue(true)
                .build());

        honeyBlock = config.create(new BooleanValue.Builder("Honey Block")
                .description("Prevents honey blocks from slowing you.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // If using an item and items setting is enabled, maintain sprint
        if (items.value() && player.isUsingItem()) {
            if (player.forwardSpeed > 0) {
                player.setSprinting(true);
            }
        }
    }

    // Public accessors for potential mixin use
    public boolean shouldCancelItemSlow() {
        return enabled() && items.value();
    }

    public boolean shouldCancelSoulSandSlow() {
        return enabled() && soulSand.value();
    }

    public boolean shouldCancelBerryBushSlow() {
        return enabled() && berryBush.value();
    }

    public boolean shouldCancelHoneyBlockSlow() {
        return enabled() && honeyBlock.value();
    }
}
