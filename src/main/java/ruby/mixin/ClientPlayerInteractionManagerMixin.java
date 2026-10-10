package ruby.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ruby.RubyClient;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.events.player.BlockBreakingCooldownEvent;
import ruby.systems.modules.Modules;
import ruby.systems.modules.player.BreakDelay;
import ruby.systems.modules.player.SpeedMine;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @WrapMethod(method = "attack")
    private void ruby$onBeforeAttack(Player player, Entity target, Operation<Void> original) {
        EntityEvent event = new EntityEvent(false, target, null, InteractionHand.MAIN_HAND);
        if(Events.ENTITY.fire(EntityEvents.BEFORE_INTERACT, event)) return;
        original.call(player, event.entity());
    }

    @Inject(method = "attack", at = @At("TAIL"), cancellable = true)
    private void ruby$onAfterAttack(Player player, Entity target, CallbackInfo info) {
        Events.ENTITY.fire(EntityEvents.AFTER_INTERACT, new EntityEvent(
                false, target,
                null, InteractionHand.MAIN_HAND
        ));
    }

    @WrapMethod(method = "interact")
    private InteractionResult ruby$onBeforeInteract(
            Player player,
            Entity entity,
            EntityHitResult hitResult,
            InteractionHand hand,
            Operation<InteractionResult> original
    ) {
        EntityEvent event = new EntityEvent(true, entity, hitResult, hand);
        if(Events.ENTITY.fire(EntityEvents.BEFORE_INTERACT, event)) return InteractionResult.PASS;
        return original.call(player, event.entity(), hitResult, event.hand());
    }

    @Inject(method = "interact", at = @At("TAIL"), cancellable = true)
    private void ruby$onAfterInteract(
            Player player,
            Entity entity,
            EntityHitResult hitResult,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> info
    ) {
        Events.ENTITY.fire(EntityEvents.AFTER_INTERACT, new EntityEvent(
                true, entity,
                hitResult, hand
        ));
    }

    @Redirect(
            method = "continueDestroyBlock",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1)
    )
    private void ruby$breakDelayCooldownCreative(MultiPlayerGameMode manager, int value) {
        ruby$applyBreakDelayCooldown(manager, value);
    }

    @Redirect(
            method = "continueDestroyBlock",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2)
    )
    private void ruby$breakDelayCooldownFinish(MultiPlayerGameMode manager, int value) {
        ruby$applyBreakDelayCooldown(manager, value);
    }

    @Redirect(
            method = "startDestroyBlock",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 0)
    )
    private void ruby$breakDelayCooldownStart(MultiPlayerGameMode manager, int value) {
        ruby$applyBreakDelayCooldown(manager, value);
    }

    private void ruby$applyBreakDelayCooldown(MultiPlayerGameMode manager, int value) {
        BlockBreakingCooldownEvent event = new BlockBreakingCooldownEvent(value);
        BreakDelay breakDelay = Modules.getByClass(BreakDelay.class);
        if (breakDelay != null && breakDelay.enabled()) breakDelay.applyCooldown(event);
        ((ClientPlayerInteractionManagerAccessor) manager).ruby$setBlockBreakingCooldown(event.cooldown);
    }

    @Redirect(
            method = "continueDestroyBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F")
    )
    private float ruby$preventInstaBreak(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        float delta = state.getDestroyProgress(player, world, pos);
        BreakDelay breakDelay = Modules.getByClass(BreakDelay.class);
        if (breakDelay != null && breakDelay.enabled() && breakDelay.preventInstaBreak() && delta >= 1f) {
            ClientPlayerInteractionManagerAccessor accessor = (ClientPlayerInteractionManagerAccessor) RubyClient.client.gameMode;
            BlockBreakingCooldownEvent event = new BlockBreakingCooldownEvent(accessor.ruby$getBlockBreakingCooldown());
            breakDelay.applyCooldown(event);
            accessor.ruby$setBlockBreakingCooldown(event.cooldown);
            return 0f;
        }
        return delta;
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void ruby$speedMineInstamine(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        SpeedMine speedMine = Modules.getByClass(SpeedMine.class);
        if (speedMine == null || !speedMine.enabled() || !speedMine.instamine()) return;
        if (RubyClient.client.player == null || RubyClient.client.level == null) return;

        BlockState state = RubyClient.client.level.getBlockState(pos);
        if (!speedMine.filter(state.getBlock())) return;

        if (state.getDestroyProgress(RubyClient.client.player, RubyClient.client.level, pos) > 0.5f) {
            MultiPlayerGameMode manager = (MultiPlayerGameMode) (Object) this;
            manager.destroyBlock(pos);
            cir.setReturnValue(true);
        }
    }
}
