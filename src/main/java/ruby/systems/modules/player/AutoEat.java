package ruby.systems.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's AutoEat: monitors hunger and automatically eats food from hotbar.
 * Threshold modes: when hunger drops below value, finds the best food and eats it.
 * Supports hunger-based priority from FoodComponent.nutrition().
 */
public class AutoEat extends Module {
    private final IntegerValue hungerThreshold;
    private final BooleanValue offhand;

    private boolean eating = false;
    private int prevSlot = -1;

    public AutoEat() {
        super("Auto Eat", "Automatically eats food when you are hungry.", ModuleType.PLAYER);

        hungerThreshold = config.create(new IntegerValue.Builder("Hunger")
                .description("Hunger level to start eating at.")
                .defaultValue(16).min(1).max(20)
                .build());

        offhand = config.create(new BooleanValue.Builder("Offhand")
                .description("Also check the offhand for food.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;

        int hunger = player.getHungerManager().getFoodLevel();

        // If currently eating, keep holding use key
        if (eating) {
            if (!player.isUsingItem()) {
                // Finished eating or interrupted
                eating = false;
                if (prevSlot != -1) {
                    player.getInventory().setSelectedSlot(prevSlot);
                    prevSlot = -1;
                }
            }
            return;
        }

        // Check if we need to eat
        if (hunger >= hungerThreshold.value()) return;

        // Check offhand first
        if (offhand.value()) {
            ItemStack offhandStack = player.getOffHandStack();
            if (isFood(offhandStack)) {
                mc.interactionManager.interactItem(player, Hand.OFF_HAND);
                eating = true;
                return;
            }
        }

        // Find best food in hotbar
        int bestSlot = -1;
        int bestNutrition = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!isFood(stack)) continue;

            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food != null && food.nutrition() > bestNutrition) {
                bestNutrition = food.nutrition();
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            prevSlot = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(bestSlot);
            mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            eating = true;
        }
    }

    private boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.get(DataComponentTypes.FOOD) != null;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && eating && prevSlot != -1) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
        eating = false;
        prevSlot = -1;
    }
}
