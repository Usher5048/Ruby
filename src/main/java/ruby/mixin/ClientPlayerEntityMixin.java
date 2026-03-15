package ruby.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.events.Events;
import ruby.systems.events.client.ClientPlayerPreTickEvent;
import ruby.systems.events.client.SendMovementPacketsEvent;
import ruby.systems.modules.combat.Hitboxes;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Shadow private double lastXClient;
    @Shadow private double lastYClient;
    @Shadow private double lastZClient;
    @Shadow private float lastYawClient;
    @Shadow private float lastPitchClient;
    @Shadow private boolean lastOnGround;
    @Shadow private boolean lastHorizontalCollision;
    @Shadow private int ticksSinceLastPositionPacketSent;

    @Shadow @Final public ClientPlayNetworkHandler networkHandler;

    @Shadow protected abstract boolean isCamera();

    @Inject(method = "tick", at = @At("HEAD"))
    private void ruby$preTick(CallbackInfo ci) {
        Hitboxes.onClientTick();
        Events.CLIENT_PLAYER_PRE_TICK.fireEvent(ClientPlayerPreTickEvent.get());
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void ruby$sendMovementPackets(CallbackInfo ci) {
        Events.SEND_MOVEMENT_PACKETS_PRE.fireEvent(SendMovementPacketsEvent.Pre.get());

        if (!isCamera()) {
            ci.cancel();
            return;
        }

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;

        double x = self.getX();
        double y = self.getY();
        double z = self.getZ();
        float yaw = self.getYaw();
        float pitch = self.getPitch();
        boolean onGround = self.isOnGround();
        boolean horizCol = self.horizontalCollision;

        double dx = x - lastXClient;
        double dy = y - lastYClient;
        double dz = z - lastZClient;

        ticksSinceLastPositionPacketSent++;

        boolean positionChanged = (dx * dx + dy * dy + dz * dz) > (2e-4 * 2e-4)
            || ticksSinceLastPositionPacketSent >= 20;
        boolean lookChanged = yaw != lastYawClient || pitch != lastPitchClient;
        boolean groundChanged = onGround != lastOnGround || horizCol != lastHorizontalCollision;

        if (!positionChanged && !lookChanged && !groundChanged) {
            ci.cancel();
            return;
        }

        boolean isMoving = positionChanged;
        float[] out = Hitboxes.buildOutputYawPitch(yaw, pitch, isMoving);
        float outYaw = out[0];
        float outPitch = out[1];

        PlayerMoveC2SPacket packet;
        if (positionChanged && lookChanged) {
            packet = new PlayerMoveC2SPacket.Full(x, y, z, outYaw, outPitch, onGround, horizCol);
        } else if (positionChanged) {
            packet = new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, horizCol);
        } else {
            packet = new PlayerMoveC2SPacket.LookAndOnGround(outYaw, outPitch, onGround, horizCol);
        }

        networkHandler.sendPacket(packet);

        if (positionChanged) {
            lastXClient = x;
            lastYClient = y;
            lastZClient = z;
            ticksSinceLastPositionPacketSent = 0;
        }
        if (lookChanged) {
            lastYawClient = yaw;
            lastPitchClient = pitch;
        }
        lastOnGround = onGround;
        lastHorizontalCollision = horizCol;

        ci.cancel();
    }
}
