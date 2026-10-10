package ruby.systems.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if(player == null || mc.gameMode == null || mc.level == null) return;
        if(mc.gui.screen() != null) return;

        if(this.throwTicks > 0) {
            this.throwTicks--;
            if(this.throwTicks == 0) this.restoreSlot(player);
            return;
        }

        if(!this.needsHealing(player)) return;
        if(!player.onGround()) return;

        int slot = this.findPotionSlot(player);
        if(slot == -1) return;

        this.prevSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(slot);
        player.setXRot(88.0f);

        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        this.throwTicks = 4;
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player != null) this.restoreSlot(mc.player);
        this.throwTicks = 0;
    }

    private void restoreSlot(LocalPlayer player) {
        if(this.prevSlot != -1) {
            player.getInventory().setSelectedSlot(this.prevSlot);
            this.prevSlot = -1;
        }
    }

    private boolean needsHealing(LocalPlayer player) {
        float health = player.getHealth();
        if(this.considerAbsorption.value()) health += player.getAbsorptionAmount();
        return health <= player.getMaxHealth() * this.healthPercent.value().floatValue() / 100.0f;
    }

    private int findPotionSlot(LocalPlayer player) {
        return Slots.findFirst(Slots.HOTBAR, (stack, index) -> this.isUsefulPotion(stack, player));
    }

    private boolean isUsefulPotion(ItemStack stack, LocalPlayer player) {
        if(stack.isEmpty() || !stack.is(Items.SPLASH_POTION)) return false;

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if(contents == null) return false;

        float health = player.getHealth();
        for(MobEffectInstance effect : contents.getAllEffects()) {
            if(this.instantHealthOnly.value()) {
                if(effect.getEffect().value() == MobEffects.INSTANT_HEALTH.value()) return true;
                continue;
            }
            if(effect.getEffect().value() == MobEffects.INSTANT_HEALTH.value() && health < player.getMaxHealth())
                return true;
            if(effect.getEffect().value() == MobEffects.REGENERATION.value() && !player.hasEffect(MobEffects.REGENERATION))
                return true;
        }
        return false;
    }
}
