package ruby.systems.modules.render;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
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

    private Perspective perspective;
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
            savedFovScale = RubyClient.client.options.getFovEffectScale().getValue();
            savedBobView = RubyClient.client.options.getBobView().getValue();
            RubyClient.client.options.getFovEffectScale().setValue(0.0);
            RubyClient.client.options.getBobView().setValue(false);
        }

        speedValue = speed.value();
        yaw = RubyClient.client.player.getYaw();
        pitch = RubyClient.client.player.getPitch();
        lastYaw = yaw;
        lastPitch = pitch;
        perspective = RubyClient.client.options.getPerspective();

        Vec3d cam = RubyClient.client.gameRenderer.getCamera().getCameraPos();
        x = prevX = cam.x;
        y = prevY = cam.y;
        z = prevZ = cam.z;

        if (perspective == Perspective.THIRD_PERSON_FRONT) {
            yaw += 180;
            pitch *= -1;
        }

        isSneaking = InputUtils.isKeyPressed(RubyClient.client.options.sneakKey);
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
            RubyClient.client.options.getFovEffectScale().setValue(savedFovScale);
            RubyClient.client.options.getBobView().setValue(savedBobView);
        }

        if (perspective != null) RubyClient.client.options.setPerspective(perspective);
        isSneaking = false;
        forward = backward = right = left = up = down = false;
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null) return;

        if (!RubyClient.client.options.getPerspective().isFirstPerson()) {
            RubyClient.client.options.setPerspective(Perspective.FIRST_PERSON);
        }

        unpressKeys();

        Vec3d forwardVec = Vec3d.fromPolar(0, yaw);
        Vec3d rightVec = Vec3d.fromPolar(0, yaw + 90);

        double velX = 0, velY = 0, velZ = 0;
        double s = InputUtils.isKeyPressed(RubyClient.client.options.sprintKey) ? 1.0 : 0.5;

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
        if (RubyClient.client.currentScreen != null) return false;
        if (InputUtils.isKeyPressed(GLFW.GLFW_KEY_F3)) return false;

        var options = RubyClient.client.options;
        if (key == bindingCode(options.forwardKey)) {
            forward = action != GLFW.GLFW_RELEASE;
            options.forwardKey.setPressed(false);
            return true;
        }
        if (key == bindingCode(options.backKey)) {
            backward = action != GLFW.GLFW_RELEASE;
            options.backKey.setPressed(false);
            return true;
        }
        if (key == bindingCode(options.rightKey)) {
            right = action != GLFW.GLFW_RELEASE;
            options.rightKey.setPressed(false);
            return true;
        }
        if (key == bindingCode(options.leftKey)) {
            left = action != GLFW.GLFW_RELEASE;
            options.leftKey.setPressed(false);
            return true;
        }
        if (key == bindingCode(options.jumpKey)) {
            up = action != GLFW.GLFW_RELEASE;
            options.jumpKey.setPressed(false);
            return true;
        }
        if (key == bindingCode(options.sneakKey)) {
            down = action != GLFW.GLFW_RELEASE;
            options.sneakKey.setPressed(false);
            return true;
        }
        return false;
    }

    private void syncKeysFromInput() {
        var options = RubyClient.client.options;
        forward = InputUtils.isKeyPressed(options.forwardKey);
        backward = InputUtils.isKeyPressed(options.backKey);
        right = InputUtils.isKeyPressed(options.rightKey);
        left = InputUtils.isKeyPressed(options.leftKey);
        up = InputUtils.isKeyPressed(options.jumpKey);
        down = InputUtils.isKeyPressed(options.sneakKey);
    }

    private static int bindingCode(KeyBinding binding) {
        return ((KeyBindingAccessor) binding).getBoundKey().getCode();
    }

    private void unpressKeys() {
        RubyClient.client.options.forwardKey.setPressed(false);
        RubyClient.client.options.backKey.setPressed(false);
        RubyClient.client.options.rightKey.setPressed(false);
        RubyClient.client.options.leftKey.setPressed(false);
        RubyClient.client.options.jumpKey.setPressed(false);
        RubyClient.client.options.sneakKey.setPressed(false);
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw += (float) deltaX;
        pitch += (float) deltaY;
        pitch = MathHelper.clamp(pitch, -90, 90);
    }

    public boolean onScroll(double vertical) {
        if (scrollSensitivity.value() <= 0 || RubyClient.client.currentScreen != null) return false;
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
        return MathHelper.lerp(tickDelta, prevX, x);
    }

    public double getY(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevY, y);
    }

    public double getZ(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevZ, z);
    }

    public float getYaw(float tickDelta) {
        return MathHelper.lerp(tickDelta, lastYaw, yaw);
    }

    public float getPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, lastPitch, pitch);
    }
}
