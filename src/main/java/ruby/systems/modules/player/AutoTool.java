package ruby.systems.modules.player;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Uses the best hotbar tool for mining while rendering the player's chosen slot.
 */
public class AutoTool extends Module {
    public static AutoTool INSTANCE;

    private static final int SLOT_SPACING = 20;
    private static final int SELECTION_WIDTH = 24;
    private static final int SELECTION_HEIGHT = 23;
    private static final int SELECTION_OVERLAP = SELECTION_WIDTH - SLOT_SPACING;

    public static void renderHotbarSelection(DrawContext context, RenderPipeline pipeline, Identifier texture,
            int x, int y, int width, int height) {
        boolean dual = shouldUseMiningSlot() && miningSlot != visualSlot;
        int gap = dual ? Math.abs(miningSlot - visualSlot) : 0;

        if (dual && gap == 1) {
            if (miningSlot > visualSlot) {
                context.enableScissor(x, y, x + SELECTION_WIDTH - SELECTION_OVERLAP, y + SELECTION_HEIGHT);
            } else {
                context.enableScissor(x + SELECTION_OVERLAP, y, x + SELECTION_WIDTH, y + SELECTION_HEIGHT);
            }
        }

        context.drawGuiTexture(pipeline, texture, x, y, width, height);

        if (dual && gap == 1) {
            context.disableScissor();
        }

        if (!dual) return;

        int mx = context.getScaledWindowWidth() / 2 - 92 + miningSlot * SLOT_SPACING;
        int my = context.getScaledWindowHeight() - SELECTION_HEIGHT;

        if (gap == 1) {
            if (miningSlot > visualSlot) {
                context.enableScissor(mx + SELECTION_OVERLAP, my, mx + SELECTION_WIDTH, my + SELECTION_HEIGHT);
            } else {
                context.enableScissor(mx, my, mx + SELECTION_WIDTH - SELECTION_OVERLAP, my + SELECTION_HEIGHT);
            }
        }

        context.drawGuiTexture(pipeline, texture, mx, my, SELECTION_WIDTH, SELECTION_HEIGHT);

        if (gap == 1) {
            context.disableScissor();
        }
    }

    private final BooleanValue antiBreak;

    /** Real hotbar slot used for mining and server packets. */
    public static int miningSlot = -1;
    /** True while mining with a different slot than the one shown client-side. */
    public static boolean silentSwapped = false;
    /** Hotbar slot shown in the HUD and first-person hand. */
    public static int visualSlot = -1;

    public AutoTool() {
        super("Auto Tool", "Automatically switches to the best tool for mining.", ModuleType.PLAYER);
        INSTANCE = this;

        antiBreak = config.create(new BooleanValue.Builder("Anti Break")
                .description("Stops using tools that are about to break.")
                .defaultValue(true)
                .build());
    }

    public static boolean shouldUseMiningSlot() {
        return silentSwapped && miningSlot >= 0 && INSTANCE != null && INSTANCE.enabled();
    }

    public static boolean shouldSpoofVisualSlot() {
        return shouldUseMiningSlot() && visualSlot >= 0 && AutoToolVisualContext.isActive();
    }

    public static boolean shouldSuppressVanillaHeldItemTooltip() {
        return shouldUseMiningSlot();
    }

    private static void clearMiningState() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;

        int display = visualSlot;
        boolean wasSilent = silentSwapped;

        miningSlot = -1;
        silentSwapped = false;
        visualSlot = -1;

        if (player != null && wasSilent && display >= 0) {
            AutoToolServerSlot.restoreVisualSlot(player, display);
        }
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (!silentSwapped) {
            visualSlot = player.getInventory().getSelectedSlot();
        }

        if (!mc.options.attackKey.isPressed()) {
            clearMiningState();
            return;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) {
            clearMiningState();
            return;
        }
        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            clearMiningState();
            return;
        }

        BlockState state = mc.world.getBlockState(blockHit.getBlockPos());
        if (state.isAir()) {
            clearMiningState();
            return;
        }

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

        int displaySlot = visualSlot >= 0 ? visualSlot : player.getInventory().getSelectedSlot();
        double displaySpeed = player.getInventory().getStack(displaySlot).getMiningSpeedMultiplier(state);

        if (bestSlot == -1 || displaySpeed >= bestSpeed) {
            if (silentSwapped) {
                AutoToolServerSlot.restoreVisualSlot(player, displaySlot);
            }
            miningSlot = displaySlot;
            silentSwapped = false;
            return;
        }

        miningSlot = bestSlot;
        boolean wasSilent = silentSwapped;
        silentSwapped = bestSlot != displaySlot;

        if (silentSwapped) {
            AutoToolServerSlot.applyMiningSlot(player, miningSlot);
        } else if (wasSilent) {
            AutoToolServerSlot.restoreVisualSlot(player, displaySlot);
        }
    }

    @Override
    public void onDisable() {
        clearMiningState();
    }
}
