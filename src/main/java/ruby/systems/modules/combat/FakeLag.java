package ruby.systems.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.helpers.RandomUtils;
import ruby.helpers.blink.BlinkManager;
import ruby.helpers.combat.CombatUtils;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.config.IntegerValue;
import ruby.systems.config.StringListValue;
import ruby.systems.events.Events;
import ruby.systems.events.blink.BlinkPacketEvent;
import ruby.systems.events.TickEvents;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

import java.util.List;

/**
 * LiquidBounce {@code FakeLag} port.
 */
public class FakeLag extends Module {
    public enum Mode { Constant, Dynamic }

    public enum FlushOn {
        EntityInteract,
        BlockInteract,
        Action;

        public boolean test(Packet<?> packet) {
            return switch (this) {
                case EntityInteract -> packet instanceof ServerboundInteractPacket
                        || packet instanceof ServerboundPunchPacket;
                case BlockInteract -> packet instanceof ServerboundUseItemOnPacket
                        || packet instanceof ServerboundSignUpdatePacket;
                case Action -> packet instanceof ServerboundPlayerActionPacket;
            };
        }

        public static FlushOn fromTag(String tag) {
            for (FlushOn value : values()) {
                if (value.name().equalsIgnoreCase(tag.replace(" ", ""))) return value;
            }
            return null;
        }
    }

    public static FakeLag INSTANCE;

    private final DoubleValue rangeMin;
    private final DoubleValue rangeMax;
    private final IntegerValue delayMin;
    private final IntegerValue delayMax;
    private final IntegerValue recoilTime;
    private final EnumValue<Mode> mode;
    private final StringListValue flushOn;

    private int nextDelayMs;
    private long recoilStartMs = 0;
    private boolean enemyNearby;

    public FakeLag() {
        super("Fake Lag", "Holds back packets to avoid enemy hits.", ModuleType.COMBAT);
        INSTANCE = this;

        rangeMin = config.create(new DoubleValue.Builder("Range Min")
                .description("Minimum enemy distance to activate lag.")
                .range(0, 10, 0.01)
                .defaultValue(2.0)
                .build());
        rangeMax = config.create(new DoubleValue.Builder("Range Max")
                .description("Maximum enemy distance to activate lag.")
                .range(0, 10, 0.01)
                .defaultValue(7.94)
                .build());
        delayMin = config.create(new IntegerValue.Builder("Delay Min")
                .description("Minimum lag duration in milliseconds.")
                .range(0, 1000)
                .defaultValue(521)
                .build());
        delayMax = config.create(new IntegerValue.Builder("Delay Max")
                .description("Maximum lag duration in milliseconds.")
                .range(0, 1000)
                .defaultValue(750)
                .build());
        recoilTime = config.create(new IntegerValue.Builder("Recoil Time")
                .description("Cooldown after flushing packets.")
                .range(0, 1000)
                .defaultValue(0)
                .build());
        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Constant always lags. Dynamic only lags when advantageous.")
                .defaultValue(Mode.Dynamic)
                .build());
        flushOn = config.create(new StringListValue.Builder("Flush On")
                .description("Outgoing actions that flush queued packets.")
                .defaultValue("EntityInteract", "BlockInteract", "Action")
                .build());

        nextDelayMs = randomDelay();

        Events.BLINK.register(FakeLag::onBlinkPacket);
        Events.TICK.register(TickEvents.BEGIN, event -> onGameTick());
    }

    private static void onBlinkPacket(BlinkPacketEvent event) {
        FakeLag fakeLag = INSTANCE;
        if (fakeLag == null || !fakeLag.enabled()) return;

        fakeLag.handleBlink(event);
    }

    private void onGameTick() {
        if (!enabled()) return;
        enemyNearby = CombatUtils.findEnemy(rangeMax.value()) != null;
    }

    private void handleBlink(BlinkPacketEvent event) {
        Minecraft mc = RubyClient.client;
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (event.origin() == BlinkManager.Origin.OUTGOING) {
            handleOutgoing(event, player, mc);
            return;
        }

        handleIncomingFlush(event, player);
    }

    private void handleOutgoing(BlinkPacketEvent event, LocalPlayer player, Minecraft mc) {
        if (player.isDeadOrDying() || player.isInWater() || mc.gui.screen() != null) return;

        if (!hasRecoilElapsed()) return;

        if (BlinkManager.isAboveTime(nextDelayMs)) {
            nextDelayMs = randomDelay();
            event.setAction(BlinkManager.Action.FLUSH);
            return;
        }

        Packet<?> packet = event.packet();
        if (packet != null && matchesFlushOn(packet)) {
            resetRecoil();
            return;
        }

        if (packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundPunchPacket) {
            if (shouldFlush(FlushOn.EntityInteract)) {
                resetRecoil();
                return;
            }
        }

        if (packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundSignUpdatePacket) {
            if (shouldFlush(FlushOn.BlockInteract)) {
                resetRecoil();
                return;
            }
        }

        if (packet instanceof ServerboundPlayerActionPacket && shouldFlush(FlushOn.Action)) {
            resetRecoil();
            return;
        }

        if (player.isUsingItem() && isConsumable(player.getActiveItem())) return;

        event.setAction(switch (mode.value()) {
            case Constant -> BlinkManager.Action.QUEUE;
            case Dynamic -> shouldDynamicLag(player) ? BlinkManager.Action.QUEUE : BlinkManager.Action.PASS;
        });
    }

    private void handleIncomingFlush(BlinkPacketEvent event, LocalPlayer player) {
        Packet<?> packet = event.packet();
        switch (packet) {
            case ClientboundSetEntityMotionPacket(
                    int id, Vec3 movement
            ) when id == player.getId() && !movement.equals(Vec3.ZERO) -> {
                resetRecoil();
                event.setAction(BlinkManager.Action.FLUSH);
            }
            
            case ClientboundExplodePacket explosion -> {
                Vec3 knockback = explosion.playerKnockback().orElse(Vec3.ZERO);
                if (!knockback.equals(Vec3.ZERO)) {
                    resetRecoil();
                    event.setAction(BlinkManager.Action.FLUSH);
                }
            }
            
            case ClientboundSetHealthPacket healthUpdateS2CPacket -> {
                resetRecoil();
                event.setAction(BlinkManager.Action.FLUSH);
            }
            
            case null, default -> {}
        }

    }

    private boolean shouldDynamicLag(LocalPlayer player) {
        if (!enemyNearby) return false;

        List<Vec3> positions = BlinkManager.positions();
        net.minecraft.world.phys.Vec3 lagPosition = positions.isEmpty() ? player.getClientPosition() : positions.getFirst();
        double maxRange = rangeMax.value();

        List<Entity> entities = RubyClient.client.level.getEntities(
                player,
                new AABB(lagPosition, lagPosition).inflate(maxRange),
                CombatUtils::shouldBeAttacked
        );

        if (entities.isEmpty()) return false;

        AABB playerBox = player.getDimensions(player.getPose()).makeBoundingBox(lagPosition);
        boolean intersects = entities.stream().anyMatch(entity -> entity.getBoundingBox().intersects(playerBox));

        double serverDistance = entities.stream()
                .mapToDouble(entity -> entity.getClientPosition().distanceTo(lagPosition))
                .min()
                .orElse(Double.MAX_VALUE);
        double clientDistance = entities.stream()
                .mapToDouble(entity -> entity.getClientPosition().distanceTo(player.getClientPosition()))
                .min()
                .orElse(Double.MAX_VALUE);

        return !(serverDistance < clientDistance || intersects);
    }

    private boolean matchesFlushOn(Packet<?> packet) {
        for (String tag : flushOn.value()) {
            FlushOn flush = FlushOn.fromTag(tag);
            if (flush == null) continue;
            if (flush.test(packet)) return true;
        }
        return false;
    }

    private boolean shouldFlush(FlushOn type) {
        for (String tag : flushOn.value()) {
            FlushOn flush = FlushOn.fromTag(tag);
            if (flush == type) return true;
        }
        return false;
    }

    private static boolean isConsumable(ItemStack stack) {
        return !stack.isEmpty() && stack.getUseAnimation() != ItemUseAnimation.NONE;
    }

    private boolean hasRecoilElapsed() {
        return System.currentTimeMillis() - recoilStartMs >= recoilTime.value();
    }

    private void resetRecoil() {
        recoilStartMs = System.currentTimeMillis();
    }

    private int randomDelay() {
        return RandomUtils.randomInt(delayMin.value(), delayMax.value());
    }

    @Override
    public void onDisable() {
        enemyNearby = false;
        BlinkManager.flush(BlinkManager.Origin.OUTGOING);
    }
}
