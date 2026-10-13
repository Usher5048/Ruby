package ruby.systems.modules.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;
import ruby.helpers.world.ChunkReloadHelper;
import ruby.mixin.KeyBindingAccessor;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from Meteor Client freecam.
 */
public class Freecam extends Module {

    private final DoubleValue speed;
    private final DoubleValue scrollSensitivity;
    private final BooleanValue staySneaking;
    private final BooleanValue toggleOnDamage;
    private final BooleanValue toggleOnDeath;
    private final BooleanValue toggleOnLog;
    private final BooleanValue reloadChunks;
    private final BooleanValue renderHands;
    private final BooleanValue staticView;

    public double x, y, z;
    public double prevX, prevY, prevZ;
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private CameraType perspective;
    private double speedValue;
    private boolean isSneaking;

    private boolean forward, backward, right, left, up, down;

    private double savedFovScale;
    private boolean savedBobView;

    public Freecam() {
        super("Freecam", "Allows the camera to move away from the player.", ModuleType.RENDER);

        speed = config.create(new DoubleValue.Builder("Speed")
                .defaultValue(1.0).range(0, 10, 0.1).build());
        scrollSensitivity = config.create(new DoubleValue.Builder("Scroll Sensitivity")
                .description("Change speed with scroll wheel. 0 to disable.")
                .defaultValue(0.0).range(0, 2, 0.05).build());
        staySneaking = config.create(new BooleanValue.Builder("Stay Sneaking")
                .defaultValue(true).build());
        toggleOnDamage = config.create(new BooleanValue.Builder("Toggle On Damage")
                .defaultValue(false).build());
        toggleOnDeath = config.create(new BooleanValue.Builder("Toggle On Death")
                .defaultValue(false).build());
        toggleOnLog = config.create(new BooleanValue.Builder("Toggle On Log")
                .defaultValue(true).build());
        reloadChunks = config.create(new BooleanValue.Builder("Reload Chunks")
                .description("Disables cave culling.")
                .defaultValue(true).build());
        renderHands = config.create(new BooleanValue.Builder("Show Hands")
                .defaultValue(true).build());
        staticView = config.create(new BooleanValue.Builder("Static")
                .description("Disables view bobbing and FOV effects.")
                .defaultValue(true).build());
    }

    @Override
    public void onEnable() {
        if (RubyClient.client.player == null) return;

        if (staticView.value()) {
            savedFovScale = RubyClient.client.options.fovEffectScale().get();
            savedBobView = RubyClient.client.options.bobView().get();
            RubyClient.client.options.fovEffectScale().set(0.0);
            RubyClient.client.options.bobView().set(false);
        }

        speedValue = speed.value();
        yaw = RubyClient.client.player.getYRot();
        pitch = RubyClient.client.player.getXRot();
        lastYaw = yaw;
        lastPitch = pitch;
        perspective = RubyClient.client.options.getCameraType();

        Vec3 cam = RubyClient.client.gameRenderer.mainCamera().position();
        x = prevX = cam.x;
        y = prevY = cam.y;
        z = prevZ = cam.z;

        if (perspective == CameraType.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        isSneaking = InputUtils.isKeyPressed(RubyClient.client.options.keyShift);
        syncKeysFromInput();
        unpressKeys();

        if (reloadChunks.value()) {
            ChunkReloadHelper.schedule();
        }
    }

    @Override
    public void onDisable() {
        if (reloadChunks.value()) {
            ChunkReloadHelper.schedule();
        }

        if (staticView.value()) {
            RubyClient.client.options.fovEffectScale().set(savedFovScale);
            RubyClient.client.options.bobView().set(savedBobView);
        }

        if (perspective != null) RubyClient.client.options.setCameraType(perspective);
        isSneaking = false;
        forward = backward = right = left = up = down = false;
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null) return;

        if (!RubyClient.client.options.getCameraType().isFirstPerson()) {
            RubyClient.client.options.setCameraType(CameraType.FIRST_PERSON);
        }

        unpressKeys();

        Vec3 forwardVec = Vec3.directionFromRotation(0, yaw);
        Vec3 rightVec = Vec3.directionFromRotation(0, yaw + 90);

        double velX = 0, velY = 0, velZ = 0;
        double s = InputUtils.isKeyPressed(RubyClient.client.options.keySprint) ? 1.0 / 20 : 0.5 / 20;

        boolean movingForward = false;
        if (forward) {
            velX += forwardVec.x * s * speedValue;
            velZ += forwardVec.z * s * speedValue;
            movingForward = true;
        }
        if (backward) {
            velX -= forwardVec.x * s * speedValue;
            velZ -= forwardVec.z * s * speedValue;
            movingForward = true;
        }

        boolean movingSideways = false;
        if (right) {
            velX += rightVec.x * s * speedValue;
            velZ += rightVec.z * s * speedValue;
            movingSideways = true;
        }
        if (left) {
            velX -= rightVec.x * s * speedValue;
            velZ -= rightVec.z * s * speedValue;
            movingSideways = true;
        }

        if (movingForward && movingSideways) {
            double diagonal = 1 / Math.sqrt(2);
            velX *= diagonal;
            velZ *= diagonal;
        }

        if (up) velY += s * speedValue;
        if (down) velY -= s * speedValue;

        prevX = x;
        prevY = y;
        prevZ = z;
        x += velX;
        y += velY;
        z += velZ;
    }

    public void onScreenOpen() {
        unpressKeys();
        prevX = x;
        prevY = y;
        prevZ = z;
        lastYaw = yaw;
        lastPitch = pitch;
    }

    public boolean onKey(int key, int action) {
        if (RubyClient.client.gui.screen() != null) return false;
        if (InputUtils.isKeyPressed(InputConstants.KEY_F3)) return false;

        var options = RubyClient.client.options;
        if (key == bindingCode(options.keyUp)) {
            forward = action != InputConstants.RELEASE;
            options.keyUp.setDown(false);
            return true;
        }
        if (key == bindingCode(options.keyDown)) {
            backward = action != InputConstants.RELEASE;
            options.keyDown.setDown(false);
            return true;
        }
        if (key == bindingCode(options.keyRight)) {
            right = action != InputConstants.RELEASE;
            options.keyRight.setDown(false);
            return true;
        }
        if (key == bindingCode(options.keyLeft)) {
            left = action != InputConstants.RELEASE;
            options.keyLeft.setDown(false);
            return true;
        }
        if (key == bindingCode(options.keyJump)) {
            up = action != InputConstants.RELEASE;
            options.keyJump.setDown(false);
            return true;
        }
        if (key == bindingCode(options.keyShift)) {
            down = action != InputConstants.RELEASE;
            options.keyShift.setDown(false);
            return true;
        }
        return false;
    }

    private void syncKeysFromInput() {
        var options = RubyClient.client.options;
        forward = InputUtils.isKeyPressed(options.keyUp);
        backward = InputUtils.isKeyPressed(options.keyDown);
        right = InputUtils.isKeyPressed(options.keyRight);
        left = InputUtils.isKeyPressed(options.keyLeft);
        up = InputUtils.isKeyPressed(options.keyJump);
        down = InputUtils.isKeyPressed(options.keyShift);
    }

    private static int bindingCode(KeyMapping binding) {
        return ((KeyBindingAccessor) binding).getBoundKey().getValue();
    }

    private void unpressKeys() {
        RubyClient.client.options.keyUp.setDown(false);
        RubyClient.client.options.keyDown.setDown(false);
        RubyClient.client.options.keyRight.setDown(false);
        RubyClient.client.options.keyLeft.setDown(false);
        RubyClient.client.options.keyJump.setDown(false);
        RubyClient.client.options.keyShift.setDown(false);
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw += (float) deltaX;
        pitch += (float) deltaY;
        pitch = Mth.clamp(pitch, -90, 90);
    }

    public boolean onScroll(double vertical) {
        if (scrollSensitivity.value() <= 0 || RubyClient.client.gui.screen() != null) return false;
        speedValue += vertical * 0.25 * scrollSensitivity.value() * speedValue;
        speedValue = Math.max(0.1, speedValue);
        return true;
    }

    public boolean renderHands() {
        return !enabled() || renderHands.value();
    }

    public boolean staySneaking() {
        return enabled() && staySneaking.value() && isSneaking
                && RubyClient.client.player != null && !RubyClient.client.player.getAbilities().flying;
    }

    public boolean reloadChunks() {
        return enabled() && reloadChunks.value();
    }

    public double getX(float tickDelta) {
        return Mth.lerp(tickDelta, prevX, x);
    }

    public double getY(float tickDelta) {
        return Mth.lerp(tickDelta, prevY, y);
    }

    public double getZ(float tickDelta) {
        return Mth.lerp(tickDelta, prevZ, z);
    }

    public float getYaw(float tickDelta) {
        return Mth.lerp(tickDelta, lastYaw, yaw);
    }

    public float getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, lastPitch, pitch);
    }
}
