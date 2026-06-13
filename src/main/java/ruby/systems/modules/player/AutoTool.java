package ruby.systems.modules.player;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import ruby.mixin.ClientPlayerInteractionManagerAccessor;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Uses the best hotbar tool for mining while rendering the player's chosen slot.
 */
public class AutoTool extends Module {
    public enum PreferredWeapon { Sword, Axe, Mace, Any }

    public static AutoTool INSTANCE;

    private static final int SLOT_SPACING = 20;
    private static final int SELECTION_WIDTH = 24;
    private static final int SELECTION_HEIGHT = 23;

    public static void renderHotbarSelection(DrawContext context, RenderPipeline pipeline, Identifier texture,
                                             int x, int y, int width, int height) {
        boolean dual = shouldUseMiningSlot() && miningSlot != visualSlot;

        context.drawGuiTexture(pipeline, texture, x, y, width, height);

        if (!dual) return;

        int mx = context.getScaledWindowWidth() / 2 - 92 + miningSlot * SLOT_SPACING;
        int my = context.getScaledWindowHeight() - SELECTION_HEIGHT;

        int hotbarX = context.getScaledWindowWidth() / 2 - 91;
        int hotbarY = context.getScaledWindowHeight() - 22;
        Identifier hotbarTexture = Identifier.ofVanilla("hud/hotbar");

        int x1 = mx + 2;
        int x2 = mx + SELECTION_WIDTH - 2;
        if(miningSlot - visualSlot == 1) x1++;
        if(visualSlot - miningSlot == 1) x2--;

        context.enableScissor(x1, my, x2, my + SELECTION_HEIGHT);
        context.drawGuiTexture(pipeline, hotbarTexture, hotbarX, hotbarY, 182, 22, GUIStyle.get().rubyHover());
        context.disableScissor();
    }

    private final BooleanValue antiBreak;
    private final BooleanValue autoWeapon;
    private final EnumValue<PreferredWeapon> preferredWeapon;
    private final BooleanValue autoShieldBreak;
    private final BooleanValue autoMace;
    private final IntegerValue switchBack;

    private int weaponRestoreSlot = -1;
    private int weaponRestoreTicks;

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
        autoWeapon = config.create(new BooleanValue.Builder("Auto Weapon")
                .description("Automatically selects the best weapon when attacking.")
                .defaultValue(true)
                .build());
        preferredWeapon = config.create(new EnumValue.Builder<PreferredWeapon>("Preferred")
                .description("Preferred weapon type when no special case applies.")
                .defaultValue(PreferredWeapon.Sword)
                .build());
        autoShieldBreak = config.create(new BooleanValue.Builder("Auto Shield Break")
                .description("Prefer an axe against blocking shields.")
                .defaultValue(true)
                .build());
        autoMace = config.create(new BooleanValue.Builder("Auto Mace")
                .description("Prefer a mace when a smash attack is available.")
                .defaultValue(true)
                .build());
        switchBack = config.create(new IntegerValue.Builder("Switch Back")
                .description("Ticks until the previous slot is restored after attacking.")
                .range(1, 300)
                .defaultValue(10)
                .build());

        Events.ENTITY.register(EntityEvents.BEFORE_ATTACK, event -> {
            if (!this.enabled() || !this.autoWeapon.value()) return;
            if (!(event.entity() instanceof LivingEntity target)) return;
            if (isWeaponBusy()) return;
            this.selectWeaponOnAttack(target);
        });
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

    private boolean isWeaponBusy() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return true;
        return player.isUsingItem() && !player.getActiveItem().isEmpty();
    }

    private void selectWeaponOnAttack(LivingEntity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int weaponSlot = determineWeaponSlot(target);
        if (weaponSlot < 0) return;

        int currentSlot = player.getInventory().getSelectedSlot();
        if (currentSlot == weaponSlot) return;

        if (weaponRestoreSlot < 0) {
            weaponRestoreSlot = currentSlot;
        }

        weaponRestoreTicks = switchBack.value();
        AutoToolServerSlot.applyMiningSlot(player, weaponSlot);
    }

    private int determineWeaponSlot(LivingEntity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return -1;

        boolean requiresShield = autoShieldBreak.value() && wouldBlockHit(target);
        boolean requiresMace = autoMace.value() && canMaceSmash(player);

        int preferredSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (requiresMace && stack.getItem() instanceof MaceItem) return i;
            if (requiresShield && stack.isIn(ItemTags.AXES)) return i;

            if (matchesPreferred(stack)) preferredSlot = i;
        }

        return preferredSlot;
    }

    private boolean matchesPreferred(ItemStack stack) {
        return switch (preferredWeapon.value()) {
            case Sword -> stack.isIn(ItemTags.SWORDS);
            case Axe -> stack.isIn(ItemTags.AXES);
            case Mace -> stack.isOf(Items.MACE);
            case Any -> stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isOf(Items.MACE);
        };
    }

    private static boolean canMaceSmash(ClientPlayerEntity player) {
        return MaceItem.shouldDealAdditionalDamage(player);
    }

    private static boolean wouldBlockHit(LivingEntity target) {
        if (!(target instanceof PlayerEntity player)) return false;
        if (!player.isUsingItem() || !player.getActiveItem().isOf(Items.SHIELD)) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        Vec3d look = player.getRotationVector(player.getPitch(), player.getBodyYaw());
        Vec3d distVec = mc.player.getEntityPos().subtract(player.getEntityPos()).normalize();
        return look.dotProduct(distVec) > 0;
    }

    private void tickWeaponRestore() {
        if (weaponRestoreTicks <= 0 || weaponRestoreSlot < 0) return;

        weaponRestoreTicks--;
        if (weaponRestoreTicks > 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player != null) {
            AutoToolServerSlot.restoreVisualSlot(player, weaponRestoreSlot);
        }
        weaponRestoreSlot = -1;
    }

    private void clearWeaponState() {
        if (weaponRestoreSlot >= 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayerEntity player = mc.player;
            if (player != null) {
                AutoToolServerSlot.restoreVisualSlot(player, weaponRestoreSlot);
            }
        }
        weaponRestoreSlot = -1;
        weaponRestoreTicks = 0;
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) return;

        tickWeaponRestore();
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
        clearWeaponState();
    }

    public static final class AutoToolServerSlot {
        private static boolean applyingMiningSlot;

        private AutoToolServerSlot() {
        }

        public static boolean isApplyingMiningSlot() {
            return applyingMiningSlot;
        }

        /** Keeps the real selected slot (and server sync) on the mining tool. */
        public static void applyMiningSlot(ClientPlayerEntity player, int slot) {
            if (player == null || slot < 0 || slot > 8) return;

            PlayerInventory inv = player.getInventory();
            if (inv.getSelectedSlot() == slot) return;

            applyingMiningSlot = true;
            try {
                inv.setSelectedSlot(slot);
            } finally {
                applyingMiningSlot = false;
            }

            syncSelectedSlot();
        }

        /** Restores the player's visible slot to the server when mining ends. */
        public static void restoreVisualSlot(ClientPlayerEntity player, int slot) {
            if (player == null || slot < 0 || slot > 8) return;

            PlayerInventory inv = player.getInventory();
            if (inv.getSelectedSlot() == slot) {
                syncSelectedSlot();
                return;
            }

            applyingMiningSlot = true;
            try {
                inv.setSelectedSlot(slot);
            } finally {
                applyingMiningSlot = false;
            }

            syncSelectedSlot();
        }

        private static void syncSelectedSlot() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.interactionManager != null) {
                ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).ruby$syncSelectedSlot();
            }
        }
    }

    public static final class AutoToolVisualContext {
        private static int depth;

        private AutoToolVisualContext() {
        }

        public static void enter() {
            depth++;
        }

        public static void exit() {
            if (depth > 0) depth--;
        }

        public static boolean isActive() {
            return depth > 0;
        }
    }

    public static final class AutoToolVisualSlotSpoof {
        private AutoToolVisualSlotSpoof() {
        }

        public static int beginVisualSwap() {
            if (!silentSwapped || visualSlot < 0) return -1;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return -1;

            PlayerInventory inv = mc.player.getInventory();
            int savedSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(visualSlot);
            return savedSlot;
        }

        public static void endVisualSwap(int savedSlot) {
            if (savedSlot < 0) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            mc.player.getInventory().setSelectedSlot(savedSlot);
        }
    }
}
