package ruby.systems.modules.combat;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.helpers.Rotations;
import ruby.helpers.Slots;
import ruby.mixin.PlayerInteractEntityC2SPacketAccessor;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.HashMap;

public class ShieldBreaker extends Module {
    public final DoubleValue range = this.config.create(new DoubleValue.Builder("range")
            .description("The max distance you can be from the target")
            .defaultValue(4)
            .range(0, 10)
            .build());

    public final BooleanValue allowInventory = this.config.create(new BooleanValue.Builder("allow-inventory")
            .description("Whether to allow taking items from the inventory rather than just the hotbar")
            .defaultValue(false)
            .build());

    public final BooleanValue onlyOnHit = this.config.create(new BooleanValue.Builder("only-on-hit")
            .description("Whether to only disable shields upon hitting the target")
            .defaultValue(true)
            .build());

    public final DoubleValue vectorRange = this.config.create(new DoubleValue.Builder("shield-vector-range")
            .description("The range of the angle of the shield that can be considered facing you")
            .defaultValue(0.1)
            .range(0, 0.3, 0.01)
            .build());

    private final HashMap<Entity, Integer> delays = new HashMap<>();
    private boolean skipNext = false;

    public ShieldBreaker() {
        super("Shield Breaker", "Automatically disables the shield of your opponent.", ModuleType.COMBAT);

        Events.PACKET.register(PacketEvents.SEND, event -> {
            if(RubyClient.client.world == null) return;
            if(RubyClient.client.player == null) return;
            if(RubyClient.client.interactionManager == null) return;

            if(!this.enabled()) return;
            if(!(event.packet() instanceof PlayerInteractEntityC2SPacket interactPacket)) return;
            if(!this.onlyOnHit.value()) return;

            if(this.skipNext) {
                this.skipNext = false;
                return;
            }

            Entity entity = RubyClient.client.world.getEntityById(((PlayerInteractEntityC2SPacketAccessor) interactPacket).getEntityID());
            if(entity == null) return;

            if(!(entity instanceof PlayerEntity player)) return;
            if(!player.isUsingItem() || !player.getActiveItem().isOf(Items.SHIELD))
                return;

            Vec3d look = player.getRotationVector(player.getPitch(), player.getBodyYaw());
            Vec3d distVec = RubyClient.client.player.getEntityPos()
                    .subtract(player.getEntityPos())
                    .normalize();

            double dot = look.dotProduct(distVec);
            if(dot <= this.vectorRange.value()) return;

            int prevSlot = RubyClient.client.player.getInventory().getSelectedSlot();
            ItemStack hand = RubyClient.client.player.getMainHandStack();
            int slotIdx = Slots.findFirst(
                    this.allowInventory.value() ? Slots.INVENTORY : Slots.HOTBAR,
                    (stack, slot) -> stack.isIn(ItemTags.AXES)
            );

            if(!hand.isIn(ItemTags.AXES)) {
                if(slotIdx == -1) return;
                if(Slots.HOTBAR.contains(slotIdx)) RubyClient.client.player.getInventory().setSelectedSlot(slotIdx);
                else Slots.swap(Slots.indexToID(prevSlot), Slots.indexToID(slotIdx));
            }

            this.skipNext = true;
            event.setCancelled(true);
            RubyClient.client.interactionManager.attackEntity(RubyClient.client.player, entity);

            if(!hand.isIn(ItemTags.AXES)) {
                if(slotIdx == -1) return;
                if(Slots.HOTBAR.contains(slotIdx)) RubyClient.client.player.getInventory().setSelectedSlot(prevSlot);
                else Slots.swap(Slots.indexToID(prevSlot), Slots.indexToID(slotIdx));
            }
        });
    }

    @Override
    public void tick() {
        if(RubyClient.client.player == null) return;
        if(RubyClient.client.world == null) return;
        if(RubyClient.client.interactionManager == null) return;
        if(RubyClient.client.getNetworkHandler() == null) return;

        if(this.onlyOnHit.value())
            return;

        for(Entity entity : RubyClient.client.world.getEntities()) {
            if(entity == RubyClient.client.player) continue;
            if(!entity.isAlive()) continue;
            if(!entity.isAttackable()) continue;
            if(entity.getType() != EntityType.PLAYER) continue;

            PlayerEntity player = (PlayerEntity) entity;
            if(!player.isUsingItem() || !player.getActiveItem().isOf(Items.SHIELD)) {
                this.delays.put(player, 0);
                continue;
            }

            this.delays.put(player, this.delays.getOrDefault(player, 0) + 1);

            if(entity.distanceTo(RubyClient.client.player) > this.range.value()) continue;
            if(this.delays.get(player) < 8) continue;

            Vec3d look = player.getRotationVector(player.getPitch(), player.getBodyYaw());
            Vec3d distVec = RubyClient.client.player.getEntityPos()
                    .subtract(player.getEntityPos())
                    .normalize();

            double dot = look.dotProduct(distVec);
            if(dot <= this.vectorRange.value()) continue;

            int prevSlot = RubyClient.client.player.getInventory().getSelectedSlot();
            ItemStack hand = RubyClient.client.player.getMainHandStack();
            int slotIdx = Slots.findFirst(
                    this.allowInventory.value() ? Slots.INVENTORY : Slots.HOTBAR,
                    (stack, slot) -> stack.isIn(ItemTags.AXES)
            );

            if(!hand.isIn(ItemTags.AXES)) {
                if(slotIdx == -1) continue;
                if(Slots.HOTBAR.contains(slotIdx)) RubyClient.client.player.getInventory().setSelectedSlot(slotIdx);
                else Slots.swap(Slots.indexToID(prevSlot), Slots.indexToID(slotIdx));
            }

            RubyClient.client.player.swingHand(Hand.MAIN_HAND);
            Rotations.serverLookAt(
                    EntityAnchorArgumentType.EntityAnchor.EYES,
                    entity.getEntityPos().offset(Direction.UP, entity.getHeight() / 2)
            );

            RubyClient.client.interactionManager.attackEntity(RubyClient.client.player, entity);

            if(!hand.isIn(ItemTags.AXES)) {
                if(slotIdx == -1) continue;
                if(Slots.HOTBAR.contains(slotIdx)) RubyClient.client.player.getInventory().setSelectedSlot(prevSlot);
                else Slots.swap(Slots.indexToID(prevSlot), Slots.indexToID(slotIdx));
            }
        }
    }
}

