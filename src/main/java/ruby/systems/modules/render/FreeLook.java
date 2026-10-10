package ruby.systems.modules.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;
import ruby.systems.config.BooleanValue;
import ruby.systems.config.DoubleValue;
import ruby.systems.config.EnumValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from Meteor Client free-look.
 */
public class FreeLook extends Module {

    public enum Mode { Player, Camera }

    public final EnumValue<Mode> mode;
    public final BooleanValue togglePerspective;
    public final BooleanValue arrows;
    public final DoubleValue arrowSpeed;
    public final DoubleValue sensitivity;

    public float cameraYaw;
    public float cameraPitch;

    private CameraType prePers;

    public FreeLook() {
        super("FreeLook", "Allows more rotation options in third person.", ModuleType.RENDER);

        mode = config.create(new EnumValue.Builder<Mode>("Mode")
                .description("Which entity to rotate.")
                .defaultValue(Mode.Player)
                .build());
        togglePerspective = config.create(new BooleanValue.Builder("Toggle Perspective")
                .defaultValue(true).build());
        arrows = config.create(new BooleanValue.Builder("Arrow Keys")
                .description("Control camera rotation with arrow keys.")
                .defaultValue(true).build());
        arrowSpeed = config.create(new DoubleValue.Builder("Arrow Speed")
                .defaultValue(4.0).range(0, 10, 0.5).build());
        sensitivity = config.create(new DoubleValue.Builder("Camera Sensitivity")
                .description("Mouse sensitivity in camera mode.")
                .defaultValue(8.0).range(0.1, 20, 0.5).build());
    }

    @Override
    public void onEnable() {
        if (RubyClient.client.player == null) return;
        cameraYaw = RubyClient.client.player.getYRot();
        cameraPitch = RubyClient.client.player.getXRot();
        prePers = RubyClient.client.options.getCameraType();
        if (prePers != CameraType.THIRD_PERSON_BACK && togglePerspective.value()) {
            RubyClient.client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    @Override
    public void onDisable() {
        if (prePers != null && togglePerspective.value()
                && RubyClient.client.options.getCameraType() != prePers) {
            RubyClient.client.options.setCameraType(prePers);
        }
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null) return;

        if (arrows.value()) {
            for (int i = 0; i < arrowSpeed.value() * 2; i++) {
                if (mode.value() == Mode.Player) {
                    if (isKey(InputConstants.KEY_LEFT)) cameraYaw -= 0.5f;
                    if (isKey(InputConstants.KEY_RIGHT)) cameraYaw += 0.5f;
                    if (isKey(InputConstants.KEY_UP)) cameraPitch -= 0.5f;
                    if (isKey(InputConstants.KEY_DOWN)) cameraPitch += 0.5f;
                } else {
                    float yaw = RubyClient.client.player.getYRot();
                    float pitch = RubyClient.client.player.getXRot();
                    if (isKey(InputConstants.KEY_LEFT)) yaw -= 0.5f;
                    if (isKey(InputConstants.KEY_RIGHT)) yaw += 0.5f;
                    if (isKey(InputConstants.KEY_UP)) pitch -= 0.5f;
                    if (isKey(InputConstants.KEY_DOWN)) pitch += 0.5f;
                    RubyClient.client.player.setYRot(yaw);
                    RubyClient.client.player.setXRot(Mth.clamp(pitch, -90, 90));
                }
            }
        }

        cameraPitch = Mth.clamp(cameraPitch, -90, 90);
        RubyClient.client.player.setXRot(Mth.clamp(RubyClient.client.player.getXRot(), -90, 90));
    }

    public boolean playerMode() {
        return enabled() && RubyClient.client.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                && mode.value() == Mode.Player;
    }

    public boolean cameraMode() {
        return enabled() && mode.value() == Mode.Camera;
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        cameraYaw += (float) (deltaX / sensitivity.value());
        cameraPitch = Mth.clamp(cameraPitch + (float) (deltaY / sensitivity.value()), -90, 90);
    }

    private static boolean isKey(int key) {
        return InputUtils.isKeyPressed(key);
    }
}
