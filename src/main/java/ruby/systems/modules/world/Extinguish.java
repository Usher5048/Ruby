package ruby.systems.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ruby.helpers.Slots;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

public class Extinguish extends Module {
    private enum State { IDLE, WAIT_PICKUP, PICKING_UP }

    private final IntegerValue cooldown;
    private final IntegerValue pickupDelay;
    private final BooleanValue pickup;

    private State state = State.IDLE;
    private BlockPos waterPos;
    private int cooldownTicks;
    private int waitTicks;
    private int prevSlot = -1;

    public Extinguish() {
        super("Extinguish", "Places water under you when on fire, then picks it up.", ModuleType.WORLD);

        cooldown = config.create(new IntegerValue.Builder("Cooldown")
                .description("Ticks between extinguish attempts.")
                .defaultValue(20).min(0).max(100)
                .build());

        pickup = config.create(new BooleanValue.Builder("Pickup")
                .description("Pick the water back up with a bucket.")
                .defaultValue(true)
                .build());

        pickupDelay = config.create(new IntegerValue.Builder("Pickup Delay")
                .description("Ticks to wait before picking water up.")
                .defaultValue(3).min(1).max(20)
                .visible(pickup::value)
                .build());
    }

    @Override
    public void onDisable() {
        this.reset(Minecraft.getInstance().player);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if(player == null || mc.gameMode == null || mc.level == null) return;
        if(mc.gui.screen() != null) return;
        if(mc.level.dimension() == Level.NETHER) return;

        if(this.cooldownTicks > 0) this.cooldownTicks--;

        switch(this.state) {
            case WAIT_PICKUP -> this.tickWaitPickup(player);
            case PICKING_UP -> this.tickPickup(mc, player);
            case IDLE -> this.tickIdle(mc, player);
        }
    }

    private void tickIdle(Minecraft mc, LocalPlayer player) {
        if(!player.isOnFire()) return;
        if(this.cooldownTicks > 0) return;

        int bucketSlot = Slots.findFirst(Slots.HOTBAR, (stack, index) -> stack.is(Items.WATER_BUCKET));
        if(bucketSlot == Slots.INVALID_SLOT) return;

        BlockPos placePos = this.findPlacePos(mc, player);
        if(placePos == null) return;

        this.prevSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(bucketSlot);
        player.setXRot(90.0f);

        BlockPos support = placePos.below();
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(support),
                Direction.UP,
                support,
                false
        );

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);

        this.waterPos = placePos;
        if(this.pickup.value()) {
            this.state = State.WAIT_PICKUP;
            this.waitTicks = this.pickupDelay.value();
        } else {
            this.finish(player);
            this.cooldownTicks = this.cooldown.value();
        }
    }

    private void tickWaitPickup(LocalPlayer player) {
        if(this.waitTicks-- > 0) return;
        this.state = State.PICKING_UP;
    }

    private void tickPickup(Minecraft mc, LocalPlayer player) {
        if(this.waterPos == null) {
            this.reset(player);
            return;
        }

        int bucketSlot = Slots.findFirst(Slots.HOTBAR, (stack, index) -> stack.is(Items.BUCKET));
        if(bucketSlot == Slots.INVALID_SLOT) {
            this.reset(player);
            return;
        }

        BlockState state = mc.level.getBlockState(this.waterPos);
        if(state.getFluidState().getType() != Fluids.WATER) {
            this.reset(player);
            return;
        }

        player.getInventory().setSelectedSlot(bucketSlot);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(this.waterPos),
                Direction.UP,
                this.waterPos,
                false
        );

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);

        this.reset(player);
        this.cooldownTicks = this.cooldown.value();
    }

    private BlockPos findPlacePos(Minecraft mc, LocalPlayer player) {
        BlockPos feet = player.blockPosition();
        if(mc.level.getBlockState(feet).canBeReplaced()) return feet;

        BlockPos above = feet.above();
        if(mc.level.getBlockState(above).canBeReplaced()) return above;
        return null;
    }

    private void finish(LocalPlayer player) {
        if(this.prevSlot != -1) {
            player.getInventory().setSelectedSlot(this.prevSlot);
            this.prevSlot = -1;
        }
        this.state = State.IDLE;
        this.waterPos = null;
    }

    private void reset(LocalPlayer player) {
        if(player != null) this.finish(player);
        else {
            this.state = State.IDLE;
            this.waterPos = null;
            this.prevSlot = -1;
        }
    }
}
