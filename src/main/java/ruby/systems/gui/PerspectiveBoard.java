package ruby.systems.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;
import ruby.mixin.MouseHandlerAccessor;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** Click GUI rasterized to a texture, then drawn as a world-space quad. */
public final class PerspectiveBoard {
    private static final Logger LOGGER = LoggerFactory.getLogger("Ruby");
    private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder()
            .withLocation(RubyClient.identifier("pipeline/perspective_board"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withCull(false)
            .build());
    private static final float DISTANCE = 2.15f;
    private static final float PANEL_BLOCKS = 2.15f;

    private static final Vector3f RIGHT = new Vector3f();
    private static final Vector3f UP = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Vector4f WHITE = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f ZERO = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();

    private static StagedVertexBuffer buffer;
    private static TextureTarget target;
    private static GuiRenderState state;
    private static GuiRenderer renderer;
    private static GpuBufferSlice projectionBuffer;
    private static com.mojang.blaze3d.ProjectionType projectionType;
    private static boolean haveProjection;
    private static boolean frameReady;
    private static boolean drawing;
    private static boolean failed;
    private static float halfWidth = 1f;
    private static float halfHeight = 0.5f;
    private static int originX;
    private static int originY;
    private static int spanW = 1;
    private static int spanH = 1;
    private static int sample = 1;
    private static boolean captureScissor;
    private static float u0;
    private static float v0;
    private static float u1 = 1f;
    private static float v1 = 1f;
    private static Vec3 anchor;
    private static boolean placed;
    private static boolean dragging;
    private static boolean driving;
    private static float grabX;
    private static float grabY;
    private static float grabDistance;
    private static boolean aimed;
    private static boolean watchGrab;
    private static float watchYaw;
    private static float watchPitch;
    private static int mappedX;
    private static int mappedY;

    private PerspectiveBoard() {}

    public static boolean active() {
        return PerspectiveBoard.enabled() && PerspectiveBoard.haveProjection && PerspectiveBoard.placed;
    }

    private static boolean enabled() {
        Minecraft mc = RubyClient.client;
        return ClientSettings.get().perspective()
                && mc.player != null
                && mc.gui.screen() instanceof ClickGUI;
    }

    public static void reseat() {
        PerspectiveBoard.placed = false;
        PerspectiveBoard.dragging = false;
        PerspectiveBoard.aimed = false;
        PerspectiveBoard.anchor = null;
    }

    public static void tick() {
        if (!PerspectiveBoard.enabled()) {
            PerspectiveBoard.releaseCamera();
            PerspectiveBoard.driveMovement(false);
            PerspectiveBoard.placed = false;
            PerspectiveBoard.dragging = false;
            PerspectiveBoard.aimed = false;
            return;
        }
        PerspectiveBoard.holdCamera();
        PerspectiveBoard.fitExtents();
        if (!PerspectiveBoard.placed) PerspectiveBoard.place();
        PerspectiveBoard.watchGrab();
        if (PerspectiveBoard.dragging) PerspectiveBoard.dragToAim();
        PerspectiveBoard.aimed = PerspectiveBoard.overPanels();
        PerspectiveBoard.driveMovement(true);
    }

    public static boolean aimed() {
        return PerspectiveBoard.aimed;
    }

    public static boolean isDragging() {
        return PerspectiveBoard.dragging;
    }

    public static void beginDrag() {
        Camera camera = PerspectiveBoard.camera();
        if (camera == null || PerspectiveBoard.anchor == null) return;
        Hit hit = PerspectiveBoard.aimHit();
        float localX = hit == null ? 0f : hit.localX;
        float localY = hit == null ? 0f : hit.localY;
        PerspectiveBoard.dragging = true;
        PerspectiveBoard.grabX = localX;
        PerspectiveBoard.grabY = localY;
        Vec3 eye = camera.position();
        Vec3 grabbed = PerspectiveBoard.anchor
                .add(PerspectiveBoard.RIGHT.x * localX, PerspectiveBoard.RIGHT.y * localX, PerspectiveBoard.RIGHT.z * localX)
                .add(PerspectiveBoard.UP.x * localY, PerspectiveBoard.UP.y * localY, PerspectiveBoard.UP.z * localY);
        PerspectiveBoard.grabDistance = (float) eye.distanceTo(grabbed);
        if (PerspectiveBoard.grabDistance < 0.75f) PerspectiveBoard.grabDistance = DISTANCE;
    }

    private static void watchGrab() {
        boolean held = InputUtils.isMousePressed(InputConstants.MOUSE_BUTTON_LEFT)
                || InputUtils.isMousePressed(InputConstants.MOUSE_BUTTON_RIGHT);
        if (!held) {
            PerspectiveBoard.watchGrab = false;
            if (PerspectiveBoard.dragging) PerspectiveBoard.endDrag();
            return;
        }
        Camera camera = PerspectiveBoard.camera();
        if (camera == null) return;
        if (!PerspectiveBoard.watchGrab) {
            PerspectiveBoard.watchGrab = true;
            PerspectiveBoard.watchYaw = camera.yRot();
            PerspectiveBoard.watchPitch = camera.xRot();
            return;
        }
        if (PerspectiveBoard.dragging) return;
        float yaw = net.minecraft.util.Mth.wrapDegrees(camera.yRot() - PerspectiveBoard.watchYaw);
        float pitch = camera.xRot() - PerspectiveBoard.watchPitch;
        if (yaw * yaw + pitch * pitch < 9f) return;
        PerspectiveBoard.beginDrag();
    }

    public static void endDrag() {
        PerspectiveBoard.dragging = false;
    }

    public static void dragToAim() {
        if (!PerspectiveBoard.dragging || PerspectiveBoard.anchor == null) return;
        Camera camera = PerspectiveBoard.camera();
        if (camera == null) return;
        Vector3f dir = PerspectiveBoard.look(camera);
        if (dir == null) return;
        Vec3 eye = camera.position();
        Vec3 point = eye.add(dir.x * PerspectiveBoard.grabDistance, dir.y * PerspectiveBoard.grabDistance, dir.z * PerspectiveBoard.grabDistance);
        for (int i = 0; i < 2; i++) {
            PerspectiveBoard.facePlayer();
            PerspectiveBoard.anchor = point.subtract(
                    PerspectiveBoard.RIGHT.x * PerspectiveBoard.grabX,
                    PerspectiveBoard.RIGHT.y * PerspectiveBoard.grabX,
                    PerspectiveBoard.RIGHT.z * PerspectiveBoard.grabX
            ).subtract(
                    PerspectiveBoard.UP.x * PerspectiveBoard.grabY,
                    PerspectiveBoard.UP.y * PerspectiveBoard.grabY,
                    PerspectiveBoard.UP.z * PerspectiveBoard.grabY
            );
        }
    }

    private static void facePlayer() {
        Camera camera = PerspectiveBoard.camera();
        if (camera == null || PerspectiveBoard.anchor == null) return;
        Vec3 eye = camera.position();
        float dx = (float) (eye.x - PerspectiveBoard.anchor.x);
        float dy = (float) (eye.y - PerspectiveBoard.anchor.y);
        float dz = (float) (eye.z - PerspectiveBoard.anchor.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-3f) return;
        PerspectiveBoard.NORMAL.set(dx / len, dy / len, dz / len);
        PerspectiveBoard.RIGHT.set(0f, 1f, 0f).cross(PerspectiveBoard.NORMAL);
        if (PerspectiveBoard.RIGHT.lengthSquared() < 1.0e-6f) PerspectiveBoard.RIGHT.set(1f, 0f, 0f);
        else PerspectiveBoard.RIGHT.normalize();
        PerspectiveBoard.UP.set(PerspectiveBoard.NORMAL).cross(PerspectiveBoard.RIGHT).normalize();
    }

    public static boolean scroll(double vertical) {
        if (!PerspectiveBoard.buttonHeld() || PerspectiveBoard.anchor == null || vertical == 0d) return false;
        float next = PerspectiveBoard.grabDistance + (float) vertical * 0.35f;
        if (!PerspectiveBoard.dragging) {
            Camera camera = PerspectiveBoard.camera();
            if (camera == null) return false;
            Vec3 eye = camera.position();
            double dx = PerspectiveBoard.anchor.x - eye.x;
            double dy = PerspectiveBoard.anchor.y - eye.y;
            double dz = PerspectiveBoard.anchor.z - eye.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-3) return true;
            next = (float) len + (float) vertical * 0.35f;
            next = Math.clamp(next, 0.9f, 8f);
            double scale = next / len;
            PerspectiveBoard.anchor = eye.add(dx * scale, dy * scale, dz * scale);
            return true;
        }
        PerspectiveBoard.grabDistance = Math.clamp(next, 0.9f, 8f);
        PerspectiveBoard.dragToAim();
        return true;
    }

    private static boolean buttonHeld() {
        return InputUtils.isMousePressed(InputConstants.MOUSE_BUTTON_LEFT)
                || InputUtils.isMousePressed(InputConstants.MOUSE_BUTTON_RIGHT);
    }

    public static boolean suppressBlur() {
        return PerspectiveBoard.drawing;
    }

    public static RenderTarget renderTarget() {
        return PerspectiveBoard.target;
    }

    public static int supersample() {
        return Math.max(1, PerspectiveBoard.sample);
    }

    public static GuiRenderer guiRenderer() {
        return PerspectiveBoard.renderer;
    }

    public static void beginFrame() {
        PerspectiveBoard.frameReady = false;
    }

    public static void captureProjection(Matrix4f projection) {
        if (projection == null) return;
        PerspectiveBoard.projectionBuffer = RenderSystem.getProjectionMatrixBuffer();
        PerspectiveBoard.projectionType = RenderSystem.getProjectionType();
        PerspectiveBoard.haveProjection = PerspectiveBoard.projectionBuffer != null
                && PerspectiveBoard.projectionType != null;
    }

    public static boolean captureScissor() {
        return PerspectiveBoard.captureScissor;
    }

    public static GuiGraphicsExtractor extractor(GuiGraphicsExtractor original) {
        if (!PerspectiveBoard.active()) return original;
        PerspectiveBoard.ensureMapped();
        PerspectiveBoard.prepareTarget();
        PerspectiveBoard.ensureRenderer();
        PerspectiveBoard.state.reset();
        Minecraft mc = RubyClient.client;
        PerspectiveBoard.captureScissor = true;
        try {
            return new GuiGraphicsExtractor(mc, PerspectiveBoard.state, PerspectiveBoard.mappedX, PerspectiveBoard.mappedY);
        } finally {
            PerspectiveBoard.captureScissor = false;
        }
    }

    public static int mouseX(int original) {
        if (!PerspectiveBoard.active()) return original;
        PerspectiveBoard.ensureMapped();
        return PerspectiveBoard.mappedX;
    }

    public static int mouseY(int original) {
        if (!PerspectiveBoard.active()) return original;
        PerspectiveBoard.ensureMapped();
        return PerspectiveBoard.mappedY;
    }

    public static double[] toGui(double guiX, double guiY) {
        if (!PerspectiveBoard.active()) return new double[] {guiX, guiY};
        Minecraft mc = RubyClient.client;
        double scale = Math.max(1, mc.getWindow().getGuiScale());
        Hit hit = PerspectiveBoard.aimHit();
        if (hit == null) return new double[] {-100000, -100000};
        return new double[] {hit.fbX / scale, hit.fbY / scale};
    }

    public static void composite() {
        if (!PerspectiveBoard.active() || PerspectiveBoard.failed) return;
        try {
            PerspectiveBoard.prepareTarget();
            PerspectiveBoard.ensureRenderer();
            PerspectiveBoard.clearTarget();
            PerspectiveBoard.drawing = true;
            try {
                PerspectiveBoard.renderer.render();
                PerspectiveBoard.renderer.endFrame();
            } finally {
                PerspectiveBoard.drawing = false;
            }
            PerspectiveBoard.draw();
        } catch (Throwable thrown) {
            PerspectiveBoard.drawing = false;
            PerspectiveBoard.failed = true;
            LOGGER.error("Perspective interface failed", thrown);
        }
    }

    private static void ensureMapped() {
        if (PerspectiveBoard.frameReady) return;
        PerspectiveBoard.frameReady = true;
        PerspectiveBoard.fitExtents();
        Hit hit = PerspectiveBoard.aimHit();
        if (hit == null) {
            PerspectiveBoard.mappedX = -100000;
            PerspectiveBoard.mappedY = -100000;
            return;
        }
        double scale = Math.max(1, RubyClient.client.getWindow().getGuiScale());
        PerspectiveBoard.mappedX = (int) Math.round(hit.fbX / scale);
        PerspectiveBoard.mappedY = (int) Math.round(hit.fbY / scale);
    }

    private static void fitExtents() {
        int frameW = Math.max(1, RubyClient.client.getWindow().getWidth());
        int frameH = Math.max(1, RubyClient.client.getWindow().getHeight());
        int[] frame = new int[4];
        if (!ClickGUI.panelFrame(frame)) {
            frame[0] = 0;
            frame[1] = 0;
            frame[2] = frameW;
            frame[3] = frameH;
        }
        PerspectiveBoard.originX = frame[0];
        PerspectiveBoard.originY = frame[1];
        PerspectiveBoard.spanW = Math.max(1, frame[2]);
        PerspectiveBoard.spanH = Math.max(1, frame[3]);
        PerspectiveBoard.u0 = PerspectiveBoard.originX / (float) frameW;
        PerspectiveBoard.u1 = (PerspectiveBoard.originX + PerspectiveBoard.spanW) / (float) frameW;
        PerspectiveBoard.v1 = 1f - PerspectiveBoard.originY / (float) frameH;
        PerspectiveBoard.v0 = 1f - (PerspectiveBoard.originY + PerspectiveBoard.spanH) / (float) frameH;
        PerspectiveBoard.halfWidth = PANEL_BLOCKS * 0.5f;
        PerspectiveBoard.halfHeight = PANEL_BLOCKS * (PerspectiveBoard.spanH / (float) PerspectiveBoard.spanW) * 0.5f;
    }

    private static void place() {
        Camera camera = PerspectiveBoard.camera();
        if (camera == null) return;
        var forward = camera.forwardVector();
        var up = camera.upVector();
        PerspectiveBoard.NORMAL.set(-forward.x(), -forward.y(), -forward.z());
        if (PerspectiveBoard.NORMAL.lengthSquared() < 1.0e-6f) PerspectiveBoard.NORMAL.set(0f, 0f, 1f);
        PerspectiveBoard.NORMAL.normalize();
        PerspectiveBoard.RIGHT.set(up.x(), up.y(), up.z()).cross(PerspectiveBoard.NORMAL);
        if (PerspectiveBoard.RIGHT.lengthSquared() < 1.0e-6f) PerspectiveBoard.RIGHT.set(1f, 0f, 0f);
        PerspectiveBoard.RIGHT.normalize();
        PerspectiveBoard.UP.set(PerspectiveBoard.NORMAL).cross(PerspectiveBoard.RIGHT).normalize();
        Vec3 at = camera.position();
        PerspectiveBoard.anchor = at.add(forward.x() * DISTANCE, forward.y() * DISTANCE, forward.z() * DISTANCE);
        PerspectiveBoard.placed = true;
        PerspectiveBoard.fitExtents();
    }

    private static boolean overPanels() {
        Hit hit = PerspectiveBoard.aimHit();
        if (hit == null) return false;
        double scale = Math.max(1, RubyClient.client.getWindow().getGuiScale());
        return ClickGUI.covers(hit.fbX / scale, hit.fbY / scale);
    }

    private static Hit aimHit() {
        Camera camera = PerspectiveBoard.camera();
        if (camera == null || PerspectiveBoard.anchor == null) return null;
        Vector3f dir = PerspectiveBoard.look(camera);
        if (dir == null) return null;
        return PerspectiveBoard.ray(camera, dir);
    }

    private static Vector3f look(Camera camera) {
        var forward = camera.forwardVector();
        Vector3f dir = new Vector3f(forward.x(), forward.y(), forward.z());
        if (dir.lengthSquared() < 1.0e-8f) return null;
        return dir.normalize();
    }

    private static Hit ray(Camera camera, Vector3f worldDir) {
        float denom = worldDir.dot(PerspectiveBoard.NORMAL);
        if (Math.abs(denom) < 1.0e-5f) return null;
        Vec3 origin = camera.position();
        float t = (float) (
                (PerspectiveBoard.anchor.x - origin.x) * PerspectiveBoard.NORMAL.x
                        + (PerspectiveBoard.anchor.y - origin.y) * PerspectiveBoard.NORMAL.y
                        + (PerspectiveBoard.anchor.z - origin.z) * PerspectiveBoard.NORMAL.z
        ) / denom;
        if (t <= 0f) return null;
        float hx = worldDir.x * t - (float) (PerspectiveBoard.anchor.x - origin.x);
        float hy = worldDir.y * t - (float) (PerspectiveBoard.anchor.y - origin.y);
        float hz = worldDir.z * t - (float) (PerspectiveBoard.anchor.z - origin.z);
        float localX = hx * PerspectiveBoard.RIGHT.x + hy * PerspectiveBoard.RIGHT.y + hz * PerspectiveBoard.RIGHT.z;
        float localY = hx * PerspectiveBoard.UP.x + hy * PerspectiveBoard.UP.y + hz * PerspectiveBoard.UP.z;
        float u = (localX / PerspectiveBoard.halfWidth + 1f) * 0.5f;
        float v = (localY / PerspectiveBoard.halfHeight + 1f) * 0.5f;
        if (u < 0f || u > 1f || v < 0f || v > 1f) return null;
        float fbX = PerspectiveBoard.originX + u * PerspectiveBoard.spanW;
        float fbY = PerspectiveBoard.originY + (1f - v) * PerspectiveBoard.spanH;
        return new Hit(fbX, fbY, localX, localY, t);
    }

    private static Camera camera() {
        Camera camera = RubyClient.client.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) return null;
        return camera;
    }

    private static void holdCamera() {
        var mouse = RubyClient.client.mouseHandler;
        if (mouse.isMouseGrabbed()) return;
        var window = RubyClient.client.getWindow();
        double x = window.getScreenWidth() / 2.0;
        double y = window.getScreenHeight() / 2.0;
        mouse.setIgnoreFirstMove();
        ((MouseHandlerAccessor) mouse).ruby$setGrabbed(true);
        InputConstants.grabMouse(window, x, y);
    }

    private static void releaseCamera() {
        var mouse = RubyClient.client.mouseHandler;
        if (RubyClient.client.gui.screen() instanceof ClickGUI && mouse.isMouseGrabbed()) {
            mouse.releaseMouse();
        }
    }

    private static void driveMovement(boolean allow) {
        Options options = RubyClient.client.options;
        if (allow) {
            options.keyUp.setDown(InputUtils.isKeyPressed(options.keyUp));
            options.keyDown.setDown(InputUtils.isKeyPressed(options.keyDown));
            options.keyLeft.setDown(InputUtils.isKeyPressed(options.keyLeft));
            options.keyRight.setDown(InputUtils.isKeyPressed(options.keyRight));
            options.keyJump.setDown(InputUtils.isKeyPressed(options.keyJump));
            options.keyShift.setDown(InputUtils.isKeyPressed(options.keyShift));
            options.keySprint.setDown(InputUtils.isKeyPressed(options.keySprint));
            PerspectiveBoard.driving = true;
            return;
        }
        if (!PerspectiveBoard.driving) return;
        options.keyUp.setDown(false);
        options.keyDown.setDown(false);
        options.keyLeft.setDown(false);
        options.keyRight.setDown(false);
        options.keyJump.setDown(false);
        options.keyShift.setDown(false);
        options.keySprint.setDown(false);
        PerspectiveBoard.driving = false;
    }

    private record Hit(double fbX, double fbY, float localX, float localY, float distance) {}

    private static void prepareTarget() {
        Minecraft mc = RubyClient.client;
        int width = Math.max(1, mc.getWindow().getWidth());
        int height = Math.max(1, mc.getWindow().getHeight());
        int sample = 3;
        while (sample > 1 && (width * (long) sample > 8192 || height * (long) sample > 8192)) sample--;
        PerspectiveBoard.sample = sample;
        int targetW = width * sample;
        int targetH = height * sample;
        if (PerspectiveBoard.target == null) {
            PerspectiveBoard.target = new TextureTarget("Ruby perspective", targetW, targetH, GpuFormat.RGBA8_UNORM, GpuFormat.D16_UNORM);
            PerspectiveBoard.clearTarget();
            return;
        }
        if (PerspectiveBoard.target.width != targetW || PerspectiveBoard.target.height != targetH) {
            PerspectiveBoard.target.resize(targetW, targetH);
            PerspectiveBoard.clearTarget();
        }
    }

    private static void clearTarget() {
        var color = PerspectiveBoard.target.getColorTexture();
        var depth = PerspectiveBoard.target.getDepthTexture();
        if (color == null) return;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        if (depth != null) encoder.clearColorAndDepthTextures(color, new Vector4f(0f, 0f, 0f, 0f), depth, 1.0);
        else encoder.clearColorTexture(color, new Vector4f(0f, 0f, 0f, 0f));
    }

    private static void ensureRenderer() {
        if (PerspectiveBoard.renderer != null) return;
        Minecraft mc = RubyClient.client;
        PerspectiveBoard.state = new GuiRenderState();
        PerspectiveBoard.renderer = new GuiRenderer(PerspectiveBoard.state, mc.gameRenderer.featureRenderDispatcher(), List.of());
    }

    private static void draw() {
        var texture = PerspectiveBoard.target.getColorTextureView();
        VertexFormat format = PIPELINE.getVertexFormatBinding(0);
        if (texture == null || format == null || PerspectiveBoard.projectionBuffer == null) return;
        if (PerspectiveBoard.buffer == null) {
            PerspectiveBoard.buffer = new StagedVertexBuffer(() -> "Ruby perspective", RenderType.SMALL_BUFFER_SIZE);
        }

        PrimitiveTopology topology = PIPELINE.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = PerspectiveBoard.buffer.appendDraw(
                format,
                topology,
                topology == PrimitiveTopology.QUADS ? PerspectiveBoard.projectionType.vertexSorting() : null
        );
        Camera camera = PerspectiveBoard.camera();
        if (camera == null || PerspectiveBoard.anchor == null) {
            PerspectiveBoard.buffer.endFrame();
            return;
        }
        Vector3f forward = PerspectiveBoard.look(camera);
        if (forward == null) {
            PerspectiveBoard.buffer.endFrame();
            return;
        }
        var upVec = camera.upVector();
        Vector3f camRight = new Vector3f(forward).cross(upVec.x(), upVec.y(), upVec.z());
        if (camRight.lengthSquared() < 1.0e-6f) camRight.set(1f, 0f, 0f);
        camRight.normalize();
        Vector3f camUp = new Vector3f(camRight).cross(forward);
        VertexConsumer builder = PerspectiveBoard.buffer.getVertexBuilder(draw);
        Vec3 cam = camera.position();
        float hw = PerspectiveBoard.halfWidth;
        float hh = PerspectiveBoard.halfHeight;
        PerspectiveBoard.vertex(builder, cam, forward, camRight, camUp, -hw, -hh, PerspectiveBoard.u0, PerspectiveBoard.v0);
        PerspectiveBoard.vertex(builder, cam, forward, camRight, camUp, hw, -hh, PerspectiveBoard.u1, PerspectiveBoard.v0);
        PerspectiveBoard.vertex(builder, cam, forward, camRight, camUp, hw, hh, PerspectiveBoard.u1, PerspectiveBoard.v1);
        PerspectiveBoard.vertex(builder, cam, forward, camRight, camUp, -hw, hh, PerspectiveBoard.u0, PerspectiveBoard.v1);
        PerspectiveBoard.buffer.upload();
        StagedVertexBuffer.ExecuteInfo info = PerspectiveBoard.buffer.getExecuteInfo(draw);
        if (info == null) {
            PerspectiveBoard.buffer.endFrame();
            return;
        }

        RenderTarget main = RubyClient.client.gameRenderer.mainRenderTarget();
        var output = main.getColorTextureView();
        var depth = main.getDepthTextureView();
        if (output == null || depth == null) {
            PerspectiveBoard.buffer.endFrame();
            return;
        }
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(IDENTITY, WHITE, ZERO, IDENTITY);
        var previousBuffer = RenderSystem.getProjectionMatrixBuffer();
        var previousType = RenderSystem.getProjectionType();
        try {
            RenderSystem.setProjectionMatrix(PerspectiveBoard.projectionBuffer, PerspectiveBoard.projectionType);
            try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Ruby perspective",
                    output,
                    Optional.empty(),
                    depth,
                    OptionalDouble.empty()
            )) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setUniform("Sampler0", texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                pass.setVertexBuffer(0, info.vertexBuffer().slice());
                pass.setIndexBuffer(info.indexBuffer(), info.indexType());
                pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
            }
        } finally {
            if (previousBuffer != null && previousType != null) {
                RenderSystem.setProjectionMatrix(previousBuffer, previousType);
            }
            PerspectiveBoard.buffer.endFrame();
        }
    }

    private static void vertex(
            VertexConsumer builder, Vec3 cam, Vector3f forward, Vector3f camRight, Vector3f camUp,
            float x, float y, float u, float v
    ) {
        float wx = (float) (PerspectiveBoard.anchor.x - cam.x) + PerspectiveBoard.RIGHT.x * x + PerspectiveBoard.UP.x * y;
        float wy = (float) (PerspectiveBoard.anchor.y - cam.y) + PerspectiveBoard.RIGHT.y * x + PerspectiveBoard.UP.y * y;
        float wz = (float) (PerspectiveBoard.anchor.z - cam.z) + PerspectiveBoard.RIGHT.z * x + PerspectiveBoard.UP.z * y;
        float vx = wx * camRight.x + wy * camRight.y + wz * camRight.z;
        float vy = wx * camUp.x + wy * camUp.y + wz * camUp.z;
        float vz = -(wx * forward.x + wy * forward.y + wz * forward.z);
        builder.addVertex(vx, vy, vz).setUv(u, v).setColor(255, 255, 255, 255);
    }
}
