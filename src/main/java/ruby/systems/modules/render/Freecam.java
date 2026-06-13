package ruby.systems.modules.render;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.helpers.input.KeybindUtil;
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

        isSneaking = KeybindUtil.isBindingDown(RubyClient.client.options.sneakKey);
        readKeys();
        unpressKeys();
    }

    @Override
    public void onDisable() {
        if (reloadChunks.value() && RubyClient.client.worldRenderer != null) {
            RubyClient.client.execute(() -> RubyClient.client.worldRenderer.reload());
        }

        if (perspective != null) RubyClient.client.options.setPerspective(perspective);
        isSneaking = false;
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null) return;

        if (!RubyClient.client.options.getPerspective().isFirstPerson()) {
            RubyClient.client.options.setPerspective(Perspective.FIRST_PERSON);
        }

        readKeys();
        unpressKeys();

        Vec3d forwardVec = Vec3d.fromPolar(0, yaw);
        Vec3d rightVec = Vec3d.fromPolar(0, yaw + 90);

        double velX = 0, velY = 0, velZ = 0;
        double s = KeybindUtil.isBindingDown(RubyClient.client.options.sprintKey) ? 1.0 : 0.5;

        if (forward) { velX += forwardVec.x * s * speedValue; velZ += forwardVec.z * s * speedValue; }
        if (backward) { velX -= forwardVec.x * s * speedValue; velZ -= forwardVec.z * s * speedValue; }
        if (right) { velX += rightVec.x * s * speedValue; velZ += rightVec.z * s * speedValue; }
        if (left) { velX -= rightVec.x * s * speedValue; velZ -= rightVec.z * s * speedValue; }
        if (up) velY += s * speedValue;
        if (down) velY -= s * speedValue;

        prevX = x; prevY = y; prevZ = z;
        x += velX; y += velY; z += velZ;
    }

    private void readKeys() {
        forward = KeybindUtil.isBindingDown(RubyClient.client.options.forwardKey);
        backward = KeybindUtil.isBindingDown(RubyClient.client.options.backKey);
        right = KeybindUtil.isBindingDown(RubyClient.client.options.rightKey);
        left = KeybindUtil.isBindingDown(RubyClient.client.options.leftKey);
        up = KeybindUtil.isBindingDown(RubyClient.client.options.jumpKey);
        down = KeybindUtil.isBindingDown(RubyClient.client.options.sneakKey);
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
