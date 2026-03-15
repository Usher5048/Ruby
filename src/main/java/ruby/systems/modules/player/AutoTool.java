package ruby.systems.modules.player;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleCategory;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Meteor's AutoTool: On StartBreakingBlockEvent, finds the best tool for the targeted block.
 * Uses ItemStack.getMiningSpeedMultiplier(BlockState) to score tools.
 * Adapted to tick-based: checks crosshair target each tick while attack button is held.
 * Swaps the selected slot so both client and server agree on the active tool.
 */
public class AutoTool extends Module {
    private final BooleanValue antiBreak;

    private int prevSlot = -1;
    private boolean swapped = false;

    // Exposed for mixins to read
    public static boolean silentSwapped = false;
    public static int visualSlot = -1;

    public AutoTool() {
        super("Auto Tool", "Automatically switches to the best tool for mining.", ModuleCategory.PLAYER);

        antiBreak = config.create(new BooleanValue.Builder("Anti Break")
                .description("Stops using tools that are about to break.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (!mc.options.attackKey.isPressed()) {
            if (swapped && prevSlot != -1) {
                player.getInventory().setSelectedSlot(prevSlot);
                prevSlot = -1;
                swapped = false;
                silentSwapped = false;
                visualSlot = -1;
            }
            return;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;
        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockState state = mc.world.getBlockState(blockHit.getBlockPos());
        if (state.isAir()) return;

        int bestSlot = -1;
        double bestSpeed = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (antiBreak.value() && stack.isDamageable()) {
                if (stack.getMaxDamage() - stack.getDamage() <= 3) continue;
            }

            double speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        int currentSlot = player.getInventory().getSelectedSlot();
        double currentSpeed = player.getInventory().getStack(currentSlot).getMiningSpeedMultiplier(state);
        if (currentSpeed >= bestSpeed) return;

        if (bestSlot != -1 && bestSlot != currentSlot) {
            if (!swapped) {
                prevSlot = currentSlot;
                swapped = true;
            }
            player.getInventory().setSelectedSlot(bestSlot);
            silentSwapped = true;
            visualSlot = prevSlot;
        }
    }

    @Override
    public void onRender2D(DrawContext context) {
        if (!swapped) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        int activeSlot = mc.player.getInventory().getSelectedSlot();
        ItemStack toolStack = mc.player.getInventory().getStack(activeSlot);
        if (toolStack.isEmpty()) return;

        TextRenderer font = mc.textRenderer;
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        String toolName = toolStack.getName().getString();
        int textWidth = font.getWidth(toolName);
        int totalWidth = 16 + 4 + textWidth;
        int centerX = screenWidth / 2;

        int bgX = centerX - totalWidth / 2 - 4;
        int bgY = screenHeight - 60;

        context.fill(bgX, bgY, bgX + totalWidth + 8, bgY + 20, 0x80000000);

        int iconX = centerX - totalWidth / 2;
        context.drawItem(toolStack, iconX, bgY + 2);

        context.drawTextWithShadow(font, toolName, iconX + 20, bgY + 6, 0xFFCC3344);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && swapped && prevSlot != -1) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
        swapped = false;
        prevSlot = -1;
        silentSwapped = false;
        visualSlot = -1;
    }
}
