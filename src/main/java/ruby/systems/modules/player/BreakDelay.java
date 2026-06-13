package ruby.systems.modules.player;

import ruby.RubyClient;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.events.player.BlockBreakingCooldownEvent;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Break delay ported from Meteor Client.
 */
public class BreakDelay extends Module {

    private final IntegerValue cooldown;
    private final BooleanValue noInstaBreak;

    private boolean breakBlockCooldown;

    public BreakDelay() {
        super("Break Delay", "Changes the delay between breaking blocks.", ModuleType.PLAYER);

        cooldown = config.create(new IntegerValue.Builder("Cooldown")
                .description("Block break cooldown in ticks.")
                .range(0, 5).defaultValue(0).build());
        noInstaBreak = config.create(new BooleanValue.Builder("No Insta Break")
                .description("Prevents misbreaking instantly-breakable blocks.")
                .defaultValue(false).build());
    }

    public void applyCooldown(BlockBreakingCooldownEvent event) {
        if (breakBlockCooldown) {
            event.cooldown = 5;
            breakBlockCooldown = false;
        } else {
            event.cooldown = cooldown.value();
        }
    }

    public boolean preventInstaBreak() {
        return enabled() && noInstaBreak.value();
    }

    @Override
    public void tick() {
        if (noInstaBreak.value() && RubyClient.client.options.attackKey.isPressed()) {
            breakBlockCooldown = true;
        }
    }
}
