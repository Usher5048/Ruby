package ruby.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    @Shadow private void sendSprintingPacket() {}

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void ruby$sendMovementPackets(CallbackInfo ci) {
        if (!Hitboxes.shouldOverrideMovementPackets()) return;

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;

        sendSprintingPacket();
        if (!isCamera()) return;

        double dx = self.getX() - lastXClient;
        double dy = self.getY() - lastYClient;
        double dz = self.getZ() - lastZClient;
        double dyaw = self.getYaw() - lastYawClient;
        double dpitch = self.getPitch() - lastPitchClient;

        ticksSinceLastPositionPacketSent++;

        boolean positionChanged = MathHelper.squaredMagnitude(dx, dy, dz) > MathHelper.square(2.0E-4)
                || ticksSinceLastPositionPacketSent >= 20;
        boolean lookChanged = dyaw != 0.0 || dpitch != 0.0;

        float yaw = self.getYaw();
        float pitch = self.getPitch();
        boolean onGround = self.isOnGround();
        boolean horizCol = self.horizontalCollision;

        if (positionChanged && lookChanged) {
            float[] out = Hitboxes.buildOutputYawPitch(yaw, pitch, true);
            networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    self.getEntityPos(), out[0], out[1], onGround, horizCol));
        } else if (positionChanged) {
            networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    self.getEntityPos(), onGround, horizCol));
        } else if (lookChanged) {
            float[] out = Hitboxes.buildOutputYawPitch(yaw, pitch, false);
            networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    out[0], out[1], onGround, horizCol));
        } else if (onGround != lastOnGround || horizCol != lastHorizontalCollision) {
            networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(onGround, horizCol));
        }

        if (positionChanged) {
            lastXClient = self.getX();
            lastYClient = self.getY();
            lastZClient = self.getZ();
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
