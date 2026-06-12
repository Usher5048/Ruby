package ruby.systems.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import ruby.helpers.Slots;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class AutoGapple extends Module {
    private final DoubleValue healthPercent;
    private final BooleanValue considerAbsorption;
    private final BooleanValue enchanted;

    private int prevSlot = -1;
    private boolean eating;

    public AutoGapple() {
        super("Auto Gapple", "Automatically eats golden apples when low.", ModuleType.COMBAT);

        healthPercent = config.create(new DoubleValue.Builder("Health")
                .description("Health percentage to eat at.")
                .range(1, 100, 1)
                .defaultValue(40.0)
                .build());

        considerAbsorption = config.create(new BooleanValue.Builder("Absorption")
                .description("Include absorption hearts when checking health.")
                .defaultValue(true)
                .build());

        enchanted = config.create(new BooleanValue.Builder("Enchanted")
                .description("Allow enchanted golden apples.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if(player == null || mc.interactionManager == null) return;
        if(mc.currentScreen != null) return;

        if(this.eating) {
            if(!player.isUsingItem()) this.stopEating(player);
            return;
        }

        if(!this.needsHealing(player)) return;

        int slot = this.findGappleSlot(player);
        if(slot == -1) return;

        this.prevSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        this.eating = true;

        mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if(mc.player != null) this.stopEating(mc.player);
    }

    private void stopEating(ClientPlayerEntity player) {
        this.eating = false;
        if(this.prevSlot != -1) {
            player.getInventory().setSelectedSlot(this.prevSlot);
            this.prevSlot = -1;
        }
    }

    private boolean needsHealing(ClientPlayerEntity player) {
        float health = player.getHealth();
        if(this.considerAbsorption.value()) health += player.getAbsorptionAmount();
        return health <= player.getMaxHealth() * this.healthPercent.value().floatValue() / 100.0f;
    }

    private int findGappleSlot(ClientPlayerEntity player) {
        int normal = Slots.findFirst(Slots.HOTBAR, (stack, index) -> stack.isOf(Items.GOLDEN_APPLE));
        if(normal != Slots.INVALID_SLOT) return normal;

        if(!this.enchanted.value()) return Slots.INVALID_SLOT;
        return Slots.findFirst(Slots.HOTBAR, (stack, index) -> stack.isOf(Items.ENCHANTED_GOLDEN_APPLE));
    }
}
