package ruby.systems.modules.player;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import ruby.RubyClient;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Automatically throws experience bottles.
 */
public class ExpThrower extends Module {

    private final IntegerValue delay;
    private int timer;

    public ExpThrower() {
        super("Exp Thrower", "Automatically throws experience bottles.", ModuleType.PLAYER);

        delay = config.create(new IntegerValue.Builder("Delay")
                .description("Delay between throws in ticks.")
                .range(0, 20).defaultValue(0).build());
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null || RubyClient.client.interactionManager == null) return;
        if (RubyClient.client.currentScreen != null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        int slot = findBottleSlot();
        if (slot == -1) return;

        if (RubyClient.client.player.getInventory().getSelectedSlot() != slot) {
            RubyClient.client.player.getInventory().setSelectedSlot(slot);
        }

        RubyClient.client.interactionManager.interactItem(RubyClient.client.player, Hand.MAIN_HAND);
        RubyClient.client.player.swingHand(Hand.MAIN_HAND);
        timer = delay.value();
    }

    private int findBottleSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = RubyClient.client.player.getInventory().getStack(i);
            if (stack.isOf(Items.EXPERIENCE_BOTTLE)) return i;
        }
        return -1;
    }
}
