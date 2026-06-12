package ruby.systems.modules.world;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
        this.reset(MinecraftClient.getInstance().player);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if(player == null || mc.interactionManager == null || mc.world == null) return;
        if(mc.currentScreen != null) return;
        if(mc.world.getRegistryKey() == World.NETHER) return;

        if(this.cooldownTicks > 0) this.cooldownTicks--;

        switch(this.state) {
            case WAIT_PICKUP -> this.tickWaitPickup(player);
            case PICKING_UP -> this.tickPickup(mc, player);
            case IDLE -> this.tickIdle(mc, player);
        }
    }

    private void tickIdle(MinecraftClient mc, ClientPlayerEntity player) {
        if(!player.isOnFire()) return;
        if(this.cooldownTicks > 0) return;

        int bucketSlot = Slots.findFirst(Slots.HOTBAR, (stack, index) -> stack.isOf(Items.WATER_BUCKET));
        if(bucketSlot == Slots.INVALID_SLOT) return;

        BlockPos placePos = this.findPlacePos(mc, player);
        if(placePos == null) return;

        this.prevSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(bucketSlot);
        player.setPitch(90.0f);

        BlockPos support = placePos.down();
        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(support),
                Direction.UP,
                support,
                false
        );

        mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);

        this.waterPos = placePos;
        if(this.pickup.value()) {
            this.state = State.WAIT_PICKUP;
            this.waitTicks = this.pickupDelay.value();
        } else {
            this.finish(player);
            this.cooldownTicks = this.cooldown.value();
        }
    }

    private void tickWaitPickup(ClientPlayerEntity player) {
        if(this.waitTicks-- > 0) return;
        this.state = State.PICKING_UP;
    }

    private void tickPickup(MinecraftClient mc, ClientPlayerEntity player) {
        if(this.waterPos == null) {
            this.reset(player);
            return;
        }

        int bucketSlot = Slots.findFirst(Slots.HOTBAR, (stack, index) -> stack.isOf(Items.BUCKET));
        if(bucketSlot == Slots.INVALID_SLOT) {
            this.reset(player);
            return;
        }

        BlockState state = mc.world.getBlockState(this.waterPos);
        if(state.getFluidState().getFluid() != Fluids.WATER) {
            this.reset(player);
            return;
        }

        player.getInventory().setSelectedSlot(bucketSlot);
        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(this.waterPos),
                Direction.UP,
                this.waterPos,
                false
        );

        mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        player.swingHand(Hand.MAIN_HAND);

        this.reset(player);
        this.cooldownTicks = this.cooldown.value();
    }

    private BlockPos findPlacePos(MinecraftClient mc, ClientPlayerEntity player) {
        BlockPos feet = player.getBlockPos();
        if(mc.world.getBlockState(feet).isReplaceable()) return feet;

        BlockPos above = feet.up();
        if(mc.world.getBlockState(above).isReplaceable()) return above;
        return null;
    }

    private void finish(ClientPlayerEntity player) {
        if(this.prevSlot != -1) {
            player.getInventory().setSelectedSlot(this.prevSlot);
            this.prevSlot = -1;
        }
        this.state = State.IDLE;
        this.waterPos = null;
    }

    private void reset(ClientPlayerEntity player) {
        if(player != null) this.finish(player);
        else {
            this.state = State.IDLE;
            this.waterPos = null;
            this.prevSlot = -1;
        }
    }
}
