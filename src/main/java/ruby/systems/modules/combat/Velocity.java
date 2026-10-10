package ruby.systems.modules.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.helpers.RandomUtils;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * LiquidBounce {@code Velocity} port with JumpReset mode.
 */
public class Velocity extends Module {
    public enum Mode { JumpReset }

    public static Velocity INSTANCE;

    private final EnumValue<Mode> mode;
    private final DoubleValue chance;
    private final BooleanValue jumpByReceivedHits;
    private final IntegerValue hitsUntilJumpMin;
    private final IntegerValue hitsUntilJumpMax;
    private final BooleanValue jumpByDelay;
    private final IntegerValue untilJumpMin;
    private final IntegerValue untilJumpMax;

    private int limitUntilJump;
    private boolean isFallDamage;
    private int hitsUntilJump;
    private int ticksUntilJump;

    public Velocity() {
        super("Velocity", "Modifies knockback taken from hits.", ModuleType.COMBAT);
        INSTANCE = this;

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Velocity reduction mode.")
                .defaultValue(Mode.JumpReset)
                .build());
        chance = config.create(new DoubleValue.Builder("Chance")
                .description("Chance to perform jump reset.")
                .range(0, 100, 0.1)
                .defaultValue(100.0)
                .build());
        jumpByReceivedHits = config.create(new BooleanValue.Builder("Jump By Received Hits")
                .description("Wait for multiple hits before jump resetting.")
                .defaultValue(false)
                .build());
        hitsUntilJumpMin = config.create(new IntegerValue.Builder("Hits Until Jump Min")
                .range(0, 10)
                .defaultValue(2)
                .build());
        hitsUntilJumpMax = config.create(new IntegerValue.Builder("Hits Until Jump Max")
                .range(0, 10)
                .defaultValue(2)
                .build());
        jumpByDelay = config.create(new BooleanValue.Builder("Jump By Delay")
                .description("Wait for a number of ticks before jump resetting.")
                .defaultValue(false)
                .build());
        untilJumpMin = config.create(new IntegerValue.Builder("Until Jump Min")
                .range(0, 20)
                .defaultValue(0)
                .build());
        untilJumpMax = config.create(new IntegerValue.Builder("Until Jump Max")
                .range(0, 20)
                .defaultValue(0)
                .build());

        resetLimits();

        Events.PACKET.register(PacketEvents.RECEIVE, Velocity::onReceivePacket);
    }

    private static void onReceivePacket(PacketEvent event) {
        Velocity velocity = INSTANCE;
        if (velocity == null || !velocity.enabled()) return;
        if (velocity.mode.value() != Mode.JumpReset) return;
        if (!(event.packet() instanceof ClientboundSetEntityMotionPacket(
                int id, Vec3 movement
        ))) return;

        LocalPlayer player = RubyClient.client.player;
        if (player == null || id != player.getId()) return;

        velocity.isFallDamage = movement.x() == 0.0
                && movement.z() == 0.0
                && movement.y() < 0;
    }

    public boolean shouldJumpReset() {
        if (!enabled() || mode.value() != Mode.JumpReset) return false;

        LocalPlayer player = RubyClient.client.player;
        if (player == null) return false;

        if (player.hurtTime != 9 || !player.onGround() || !player.isSprinting()
                || isFallDamage || !isCooldownOver() || !RandomUtils.chance(chance.value().floatValue())) {
            updateLimit(player);
            return false;
        }

        resetLimits();
        return true;
    }

    private boolean isCooldownOver() {
        if (jumpByReceivedHits.value()) return limitUntilJump >= hitsUntilJump;
        if (jumpByDelay.value()) return limitUntilJump >= ticksUntilJump;
        return true;
    }

    private void updateLimit(LocalPlayer player) {
        if (jumpByReceivedHits.value()) {
            if (player.hurtTime == 9) limitUntilJump++;
            return;
        }
        limitUntilJump++;
    }

    private void resetLimits() {
        limitUntilJump = 0;
        hitsUntilJump = RandomUtils.randomInt(hitsUntilJumpMin.value(), hitsUntilJumpMax.value());
        ticksUntilJump = RandomUtils.randomInt(untilJumpMin.value(), untilJumpMax.value());
    }

    @Override
    public void onDisable() {
        isFallDamage = false;
        limitUntilJump = 0;
        resetLimits();
    }
}
