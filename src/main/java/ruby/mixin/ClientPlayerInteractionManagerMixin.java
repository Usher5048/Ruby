package ruby.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.RubyClient;
import ruby.helpers.PlayerInteractEntity;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.player.BlockBreakingCooldownEvent;
import ruby.systems.modules.Modules;
import ruby.systems.modules.player.BreakDelay;
import ruby.systems.modules.player.SpeedMine;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @WrapMethod(method = "attackEntity")
    private void ruby$onBeforeAttack(PlayerEntity player, Entity target, Operation<Void> original) {
        EntityEvent event = new EntityEvent(
                PlayerInteractEntity.Type.ATTACK,
                target, null, Hand.MAIN_HAND
        );

        if(Events.ENTITY.fire(EntityEvents.BEFORE_INTERACT, event)) return;
        original.call(player, event.entity());
    }

    @Inject(method = "attackEntity", at = @At("TAIL"), cancellable = true)
    private void ruby$onAfterAttack(PlayerEntity player, Entity target, CallbackInfo info) {
        Events.ENTITY.fire(EntityEvents.AFTER_INTERACT, new EntityEvent(
                PlayerInteractEntity.Type.ATTACK,
                target, null, Hand.MAIN_HAND
        ));
    }

    @WrapMethod(method = "interactEntity")
    private ActionResult ruby$onBeforeInteract(
            PlayerEntity player,
            Entity entity, Hand hand,
            Operation<ActionResult> original
    ) {
        EntityEvent event = new EntityEvent(
                PlayerInteractEntity.Type.INTERACT,
                entity, null, hand
        );

        if(Events.ENTITY.fire(EntityEvents.BEFORE_INTERACT, event)) return ActionResult.PASS;
        return original.call(player, event.entity(), event.hand());
    }

    @Inject(method = "interactEntity", at = @At("TAIL"), cancellable = true)
    private void ruby$onAfterInteract(
            PlayerEntity player,
            Entity entity, Hand hand,
            CallbackInfoReturnable<ActionResult> info
    ) {
        Events.ENTITY.fire(EntityEvents.AFTER_INTERACT, new EntityEvent(
                PlayerInteractEntity.Type.INTERACT,
                entity, null, hand
        ));
    }

    @WrapMethod(method = "interactEntityAtLocation")
    private ActionResult ruby$onBeforeInteractAt(
            PlayerEntity player, Entity entity,
            EntityHitResult hitResult, Hand hand,
            Operation<ActionResult> original
    ) {
        EntityEvent event = new EntityEvent(
                PlayerInteractEntity.Type.INTERACT_AT,
                entity, hitResult, hand
        );

        if(Events.ENTITY.fire(EntityEvents.BEFORE_INTERACT, event)) return ActionResult.PASS;
        return original.call(player, event.entity(), event.hitResult(), event.hand());
    }

    @Inject(method = "interactEntityAtLocation", at = @At("TAIL"), cancellable = true)
    private void ruby$onAfterInteractAt(
            PlayerEntity player, Entity entity,
            EntityHitResult hitResult, Hand hand,
            CallbackInfoReturnable<ActionResult> info
    ) {
        Events.ENTITY.fire(EntityEvents.AFTER_INTERACT, new EntityEvent(
                PlayerInteractEntity.Type.INTERACT_AT,
                entity, hitResult, hand
        ));
    }

    @Redirect(
            method = "updateBlockBreakingProgress",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD, ordinal = 1)
    )
    private void ruby$breakDelayCooldownCreative(ClientPlayerInteractionManager manager, int value) {
        ruby$applyBreakDelayCooldown(manager, value);
    }

    @Redirect(
            method = "updateBlockBreakingProgress",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD, ordinal = 2)
    )
    private void ruby$breakDelayCooldownFinish(ClientPlayerInteractionManager manager, int value) {
        ruby$applyBreakDelayCooldown(manager, value);
    }

    @Redirect(
            method = "attackBlock",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;blockBreakingCooldown:I", opcode = Opcodes.PUTFIELD, ordinal = 0)
    )
    private void ruby$breakDelayCooldownStart(ClientPlayerInteractionManager manager, int value) {
        ruby$applyBreakDelayCooldown(manager, value);
    }

    private void ruby$applyBreakDelayCooldown(ClientPlayerInteractionManager manager, int value) {
        BlockBreakingCooldownEvent event = new BlockBreakingCooldownEvent(value);
        BreakDelay breakDelay = Modules.getByClass(BreakDelay.class);
        if (breakDelay != null && breakDelay.enabled()) breakDelay.applyCooldown(event);
        ((ClientPlayerInteractionManagerAccessor) manager).ruby$setBlockBreakingCooldown(event.cooldown);
    }

    @Redirect(
            method = "updateBlockBreakingProgress",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F")
    )
    private float ruby$preventInstaBreak(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        float delta = state.calcBlockBreakingDelta(player, world, pos);
        BreakDelay breakDelay = Modules.getByClass(BreakDelay.class);
        if (breakDelay != null && breakDelay.enabled() && breakDelay.preventInstaBreak() && delta >= 1f) {
            ClientPlayerInteractionManagerAccessor accessor = (ClientPlayerInteractionManagerAccessor) RubyClient.client.interactionManager;
            BlockBreakingCooldownEvent event = new BlockBreakingCooldownEvent(accessor.ruby$getBlockBreakingCooldown());
            breakDelay.applyCooldown(event);
            accessor.ruby$setBlockBreakingCooldown(event.cooldown);
            return 0f;
        }
        return delta;
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void ruby$speedMineInstamine(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        SpeedMine speedMine = Modules.getByClass(SpeedMine.class);
        if (speedMine == null || !speedMine.enabled() || !speedMine.instamine()) return;
        if (RubyClient.client.player == null || RubyClient.client.world == null) return;

        BlockState state = RubyClient.client.world.getBlockState(pos);
        if (!speedMine.filter(state.getBlock())) return;

        if (state.calcBlockBreakingDelta(RubyClient.client.player, RubyClient.client.world, pos) > 0.5f) {
            ClientPlayerInteractionManager manager = (ClientPlayerInteractionManager) (Object) this;
            manager.breakBlock(pos);
            cir.setReturnValue(true);
        }
    }
}
