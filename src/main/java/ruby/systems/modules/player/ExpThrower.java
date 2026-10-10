package ruby.systems.modules.player;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        if (RubyClient.client.player == null || RubyClient.client.gameMode == null) return;
        if (RubyClient.client.gui.screen() != null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        int slot = findBottleSlot();
        if (slot == -1) return;

        if (RubyClient.client.player.getInventory().getSelectedSlot() != slot) {
            RubyClient.client.player.getInventory().setSelectedSlot(slot);
        }

        RubyClient.client.gameMode.useItem(RubyClient.client.player, InteractionHand.MAIN_HAND);
        RubyClient.client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        timer = delay.value();
    }

    private int findBottleSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = RubyClient.client.player.getInventory().getItem(i);
            if (stack.is(Items.EXPERIENCE_BOTTLE)) return i;
        }
        return -1;
    }
}
