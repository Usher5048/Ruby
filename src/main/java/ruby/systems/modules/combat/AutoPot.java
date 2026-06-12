package ruby.systems.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import ruby.helpers.Slots;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class AutoPot extends Module {
    private final DoubleValue healthPercent;
    private final BooleanValue considerAbsorption;
    private final BooleanValue instantHealthOnly;

    private int prevSlot = -1;
    private int throwTicks;

    public AutoPot() {
        super("Auto Pot", "Throws splash potions when low on health.", ModuleType.COMBAT);

        healthPercent = config.create(new DoubleValue.Builder("Health")
                .description("Health percentage to throw at.")
                .range(1, 100, 1)
                .defaultValue(40.0)
                .build());

        considerAbsorption = config.create(new BooleanValue.Builder("Absorption")
                .description("Include absorption hearts when checking health.")
                .defaultValue(true)
                .build());

        instantHealthOnly = config.create(new BooleanValue.Builder("Instant Health Only")
                .description("Only throw potions with instant health.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if(player == null || mc.interactionManager == null || mc.world == null) return;
        if(mc.currentScreen != null) return;

        if(this.throwTicks > 0) {
            this.throwTicks--;
            if(this.throwTicks == 0) this.restoreSlot(player);
            return;
        }

        if(!this.needsHealing(player)) return;
        if(!player.isOnGround()) return;

        int slot = this.findPotionSlot(player);
        if(slot == -1) return;

        this.prevSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        player.setPitch(88.0f);

        mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
        player.swingHand(Hand.MAIN_HAND);
        this.throwTicks = 4;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if(mc.player != null) this.restoreSlot(mc.player);
        this.throwTicks = 0;
    }

    private void restoreSlot(ClientPlayerEntity player) {
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

    private int findPotionSlot(ClientPlayerEntity player) {
        return Slots.findFirst(Slots.HOTBAR, (stack, index) -> this.isUsefulPotion(stack, player));
    }

    private boolean isUsefulPotion(ItemStack stack, ClientPlayerEntity player) {
        if(stack.isEmpty() || !stack.isOf(Items.SPLASH_POTION)) return false;

        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if(contents == null) return false;

        float health = player.getHealth();
        for(StatusEffectInstance effect : contents.getEffects()) {
            if(this.instantHealthOnly.value()) {
                if(effect.getEffectType().value() == StatusEffects.INSTANT_HEALTH) return true;
                continue;
            }
            if(effect.getEffectType().value() == StatusEffects.INSTANT_HEALTH && health < player.getMaxHealth())
                return true;
            if(effect.getEffectType().value() == StatusEffects.REGENERATION && !player.hasStatusEffect(StatusEffects.REGENERATION))
                return true;
        }
        return false;
    }
}
