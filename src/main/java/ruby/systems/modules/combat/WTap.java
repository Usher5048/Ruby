package ruby.systems.modules.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import ruby.RubyClient;
import ruby.helpers.PlayerInteractEntity;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.events.Events;
import ruby.systems.events.entity.EntityEvent;
import ruby.systems.events.entity.EntityEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * W-tap knockback boost — ported from LiquidBounce SuperKnockback WTap mode.
 */
public class WTap extends Module {
    private enum State { IDLE, WAIT_BLOCK, BLOCKING, WAIT_ALLOW }

    private final IntegerValue hurtTime;
    private final IntegerValue chance;
    private final IntegerValue blockDelayMin;
    private final IntegerValue blockDelayMax;
    private final IntegerValue allowDelayMin;
    private final IntegerValue allowDelayMax;
    private final BooleanValue onlyOnMove;
    private final BooleanValue onlyForward;

    private State state = State.IDLE;
    private int tickCounter;
    private boolean cancelMovement;

    public WTap() {
        super("WTap", "Briefly stops forward movement after hitting for extra knockback.", ModuleType.COMBAT);

        hurtTime = config.create(new IntegerValue.Builder("Hurt Time")
                .description("Maximum target hurt time to trigger on.")
                .range(0, 10).defaultValue(10).build());
        chance = config.create(new IntegerValue.Builder("Chance")
                .description("Chance to trigger per hit.")
                .range(0, 100).defaultValue(100).build());
        blockDelayMin = config.create(new IntegerValue.Builder("Block Delay Min")
                .description("Minimum ticks before movement is blocked.")
                .range(0, 10).defaultValue(0).build());
        blockDelayMax = config.create(new IntegerValue.Builder("Block Delay Max")
                .description("Maximum ticks before movement is blocked.")
                .range(0, 10).defaultValue(1).build());
        allowDelayMin = config.create(new IntegerValue.Builder("Allow Delay Min")
                .description("Minimum ticks before movement is allowed again.")
                .range(0, 10).defaultValue(0).build());
        allowDelayMax = config.create(new IntegerValue.Builder("Allow Delay Max")
                .description("Maximum ticks before movement is allowed again.")
                .range(0, 10).defaultValue(1).build());
        onlyOnMove = config.create(new BooleanValue.Builder("Only On Move")
                .description("Only trigger while moving.")
                .defaultValue(true).build());
        onlyForward = config.create(new BooleanValue.Builder("Only Forward")
                .description("Only trigger while moving forward, not while strafing.")
                .defaultValue(true).build());

        Events.ENTITY.register(EntityEvents.AFTER_INTERACT, this::onAttack);
    }

    public boolean blocksMovement() {
        return enabled() && cancelMovement;
    }

    @Override
    public void tick() {
        if (!enabled() || state == State.IDLE) return;

        switch (state) {
            case WAIT_BLOCK -> {
                if (--tickCounter <= 0) {
                    state = State.BLOCKING;
                    cancelMovement = true;
                }
            }
            case BLOCKING -> {
                if (!hasForwardImpulse()) {
                    state = State.WAIT_ALLOW;
                    tickCounter = randomDelay(allowDelayMin.value(), allowDelayMax.value());
                }
            }
            case WAIT_ALLOW -> {
                if (--tickCounter <= 0) {
                    state = State.IDLE;
                    cancelMovement = false;
                }
            }
            default -> {}
        }
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        cancelMovement = false;
        tickCounter = 0;
    }

    private void onAttack(EntityEvent event) {
        if(event.type() != PlayerInteractEntity.Type.ATTACK) return;
        if (!enabled() || state != State.IDLE) return;
        if (!shouldOperate(event.entity())) return;
        if (!shouldStopSprinting(event.entity())) return;

        state = State.WAIT_BLOCK;
        tickCounter = randomDelay(blockDelayMin.value(), blockDelayMax.value());
    }

    private boolean shouldStopSprinting(Entity target) {
        ClientPlayerEntity player = RubyClient.client.player;
        if (player == null || !player.isSprinting()) return false;
        if (Criticals.wouldDoCriticalHit()) return false;
        if (!(target instanceof LivingEntity living) || living.hurtTime > hurtTime.value()) return false;
        return chance.value() >= ThreadLocalRandom.current().nextInt(101);
    }

    private boolean shouldOperate(Entity target) {
        ClientPlayerEntity player = RubyClient.client.player;
        if (player == null) return false;
        if (player.isTouchingWater() || player.isSubmergedInWater()) return false;
        if (!player.isOnGround()) return false;

        if (!onlyOnMove.value()) return true;

        float sideways = player.input.getMovementInput().x;
        float forward = player.input.getMovementInput().y;
        boolean moving = forward != 0f || sideways != 0f;
        if (!moving) return false;
        return !onlyForward.value() || sideways == 0f;
    }

    private boolean hasForwardImpulse() {
        ClientPlayerEntity player = RubyClient.client.player;
        if (player == null) return true;
        return player.input.getMovementInput().y == 0f && player.input.getMovementInput().x == 0f;
    }

    private static int randomDelay(int min, int max) {
        if (max < min) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        if (min == max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
