package ruby.helpers.render;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ruby.RubyClient;

import java.util.HashMap;
import java.util.Map;

public class Renderer {
    private static RenderPipeline pipeline(String path, PrimitiveTopology topology, boolean depth) {
        return RenderPipelines.register(RenderPipeline.builder()
                .withLocation(RubyClient.identifier(path))
                .withVertexShader(RubyClient.identifier("pos_color"))
                .withFragmentShader(RubyClient.identifier("pos_color"))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(topology)
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("meshData", UniformType.UNIFORM_BUFFER)
                        .build())
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(
                        depth ? CompareOp.LESS_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS,
                        false
                ))
                .withCull(false)
                .build());
    }

    private static final RenderPipeline TRIANGLES_NO_DEPTH = pipeline(
            "pipeline/triangle", PrimitiveTopology.TRIANGLES, false);
    private static final RenderPipeline TRIANGLES_DEPTH = pipeline(
            "pipeline/triangle_depth", PrimitiveTopology.TRIANGLES, true);
    private static final RenderPipeline LINES_NO_DEPTH = pipeline(
            "pipeline/lines", PrimitiveTopology.DEBUG_LINES, false);
    private static final RenderPipeline LINES_DEPTH = pipeline(
            "pipeline/lines_depth", PrimitiveTopology.DEBUG_LINES, true);

    private static Matrix4f projection;
    private static Vec3 screenCenter;
    private static Matrix4f position;
    /** Camera position the world was extracted with. Not the live camera, which is a frame ahead of the HUD. */
    private static Vec3 cameraPos;
    private static float cachedYaw = Float.NaN;
    private static float cachedPitch = Float.NaN;
    private static float cachedPartial = Float.NaN;
    private static Vec3 cachedPos;

    /**
     * The HUD is extracted before the world is drawn, so matrices captured during the world pass
     * are still the previous frame. Read the camera snapshot that was just extracted instead.
     */
    public static void ensureCamera() {
        CameraRenderState camera = RubyClient.client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        if (camera == null || camera.pos == null || camera.viewRotationMatrix == null || camera.projectionMatrix == null) return;
        if (Renderer.position != null
                && camera.yRot == Renderer.cachedYaw
                && camera.xRot == Renderer.cachedPitch
                && camera.cameraEntityPartialTicks == Renderer.cachedPartial
                && camera.pos.equals(Renderer.cachedPos)) {
            return;
        }

        Renderer.cachedYaw = camera.yRot;
        Renderer.cachedPitch = camera.xRot;
        Renderer.cachedPartial = camera.cameraEntityPartialTicks;
        Renderer.cachedPos = camera.pos;

        Matrix4f view = new Matrix4f(camera.viewRotationMatrix);
        Matrix4f proj = new Matrix4f(camera.projectionMatrix);
        PoseStack bob = new PoseStack();
        Renderer.bobHurt(camera, bob);
        if (RubyClient.client.gameRenderer.gameRenderState().optionsRenderState.bobView) {
            Renderer.bobView(camera, bob);
        }
        proj.mul(bob.last().pose());
        Renderer.capture(proj, view, camera.pos);
    }

    public static Vec3 cameraPos() {
        if (Renderer.cameraPos == null) Renderer.ensureCamera();
        return Renderer.cameraPos != null
                ? Renderer.cameraPos
                : RubyClient.client.gameRenderer.mainCamera().position();
    }

    /** Partial tick the level extractor stored for this frame's entity positions. */
    public static float partialTicks() {
        return RubyClient.client.gameRenderer.gameRenderState().levelRenderState.worldPartialTicks;
    }

    private static void bobHurt(CameraRenderState camera, PoseStack stack) {
        CameraEntityRenderState entity = camera.entityRenderState;
        if (entity == null || !entity.isLiving) return;
        if (entity.isDeadOrDying) {
            float death = Math.min(entity.deathTime, 20f);
            stack.rotateDegrees(Axis.ZP, 40f - 8000f / (death + 200f));
        }
        float hurt = entity.hurtTime;
        if (hurt < 0f) return;
        if (entity.hurtDuration != 0) hurt /= entity.hurtDuration;
        hurt = Mth.sin(hurt * hurt * hurt * hurt * (float) Math.PI);
        float dir = entity.hurtDir;
        stack.rotateDegrees(Axis.YP, -dir);
        float tilt = (float) (-hurt * 14.0 * RubyClient.client.gameRenderer.gameRenderState().optionsRenderState.damageTiltStrength);
        stack.rotateDegrees(Axis.ZP, tilt);
        stack.rotateDegrees(Axis.YP, dir);
    }

    private static void bobView(CameraRenderState camera, PoseStack stack) {
        CameraEntityRenderState entity = camera.entityRenderState;
        if (entity == null || !entity.isPlayer) return;
        float distance = entity.backwardsInterpolatedWalkDistance;
        float bob = entity.bob;
        float angle = distance * (float) Math.PI;
        stack.translate(Mth.sin(angle) * bob * 0.5f, -Math.abs(Mth.cos(angle) * bob), 0f);
        stack.rotateDegrees(Axis.ZP, Mth.sin(angle) * bob * 3f);
        stack.rotateDegrees(Axis.XP, Math.abs(Mth.cos(angle - 0.2f)) * bob * 5f);
    }

    public static void updateMatrices(Matrix4f projection, Matrix4f position) {
        Vec3 cam = Renderer.cameraPos != null
                ? Renderer.cameraPos
                : RubyClient.client.gameRenderer.mainCamera().position();
        Renderer.capture(projection, position, cam);
    }

    private static void capture(Matrix4f projection, Matrix4f view, Vec3 cam) {
        Renderer.projection = projection;
        Renderer.position = view;
        Renderer.cameraPos = cam;

        Matrix4f invProj = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(view).invert();
        Vector4f center = new Vector4f(0, 0, 0.5f, 1).mul(invProj).mul(invView);
        center.div(center.w);
        Renderer.screenCenter = new Vec3(cam.x() + center.x, cam.y() + center.y, cam.z() + center.z);
    }

    public static Matrix4f projection() {
        return Renderer.projection;
    }
    public static Matrix4f view() {
        return Renderer.position;
    }
    public static Vec3 getScreenCenter() {
        return Renderer.screenCenter;
    }
    public static Vec3 screenToWorld(Vec3 pos) {
        Matrix4f invProj = new Matrix4f(Renderer.projection).invert();
        Matrix4f invView = new Matrix4f(Renderer.position).invert();

        pos = new Vec3(
                2 * (pos.x() / RubyClient.client.getWindow().getWidth() - 0.5),
                -2 * (pos.y() / RubyClient.client.getWindow().getHeight() - 0.5),
                pos.z()
        );

        Vector4f center = new Vector4f(
                (float) pos.x(), (float) pos.y(),
                (float) pos.z(), 1
        ).mul(invProj).mul(invView);

        center = center.div(center.w);

        Vec3 cam = Renderer.cameraPos();
        return new Vec3(
                cam.x() + center.x,
                cam.y() + center.y,
                cam.z() + center.z
        );
    }

    public static Vec2 worldToScreen(Vec3 pos) {
        Renderer.ensureCamera();
        Vec3 cam = Renderer.cameraPos();
        Vector4f vec = new Vector4f(
                (float) (pos.x() - cam.x()),
                (float) (pos.y() - cam.y()),
                (float) (pos.z() - cam.z()),
                1
        );

        Renderer.position.transform(vec);
        Renderer.projection.transform(vec);

        if(vec.w <= 0) return null;

        float ndcX = vec.x / vec.w;
        float ndcY = vec.y / vec.w;

        int screenX = (int) (     (ndcX * 0.5f + 0.5f)  * RubyClient.client.getWindow().getWidth());
        int screenY = (int) ((1 - (ndcY * 0.5f + 0.5f)) * RubyClient.client.getWindow().getHeight());

        return new Vec2(screenX, screenY);
    }

    public enum Mode {
        STROKE_ALWAYS_ON_TOP,
        STROKE_RESPECT_DEPTH,
        FILL_ALWAYS_ON_TOP,
        FILL_RESPECT_DEPTH
    }

    private record PipelineMesh(RenderPipeline pipeline, MeshBuilder mesh) {}

    private static final Map<Mode, PipelineMesh> builders = new HashMap<>();
    static {
        Renderer.builders.put(Mode.STROKE_RESPECT_DEPTH, new PipelineMesh(
                Renderer.LINES_DEPTH, new MeshBuilder(Renderer.LINES_DEPTH)));

        Renderer.builders.put(Mode.FILL_RESPECT_DEPTH, new PipelineMesh(
                Renderer.TRIANGLES_DEPTH, new MeshBuilder(Renderer.TRIANGLES_DEPTH)));

        Renderer.builders.put(Mode.STROKE_ALWAYS_ON_TOP, new PipelineMesh(
                Renderer.LINES_NO_DEPTH, new MeshBuilder(Renderer.LINES_NO_DEPTH)));

        Renderer.builders.put(Mode.FILL_ALWAYS_ON_TOP, new PipelineMesh(
                Renderer.TRIANGLES_NO_DEPTH, new MeshBuilder(Renderer.TRIANGLES_NO_DEPTH)));
    }

    public static void begin() {
        for(PipelineMesh builderPair : Renderer.builders.values())
            builderPair.mesh().begin();
    }

    public static void end(PoseStack stack) {
        for(PipelineMesh builderPair : Renderer.builders.values()) {
            MeshRenderer.begin()
                    .attachments(RubyClient.client.gameRenderer.mainRenderTarget())
                    .pipeline(builderPair.pipeline())
                    .mesh(builderPair.mesh(), stack)
                    .end();
        }
    }

    private static Mode mode = Mode.STROKE_ALWAYS_ON_TOP;
    public static void setMode(Mode mode) {
        if(mode == null) return;
        Renderer.mode = mode;
    }

    private static int color;
    public static void color(int hex) {
        Renderer.color = hex;
    }
    public static void color(int r, int g, int b, int a) {
        Renderer.color = (r << 16) | (g << 8) | b | (a << 24);
    }
    public static void color(int r, int g, int b) {
        Renderer.color = (r << 16) | (g << 8) | b;
    }

    public static void line(Vec3 p0, Vec3 p1) {
        Renderer.line(
                p0.x(), p0.y(), p0.z(),
                p1.x(), p1.y(), p1.z()
        );
    }

    public static void line(double x0, double y0, double z0, double x1, double y1, double z1) {
        Renderer.builders.get(Renderer.mode).mesh().ensureLineCapacity();
        Renderer.builders.get(Renderer.mode).mesh().line(
                Renderer.builders.get(Renderer.mode).mesh().vec3(x0, y0, z0).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).mesh().vec3(x1, y1, z1).color(Renderer.color).next()
        );
    }

    public static void triangle(Vec3 p0, Vec3 p1, Vec3 p2) {
        Renderer.triangle(
                p0.x(), p0.y(), p0.z(),
                p1.x(), p1.y(), p1.z(),
                p2.x(), p2.y(), p2.z()
        );
    }

    public static void triangle(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2
    ) {
        Renderer.builders.get(Renderer.mode).mesh().ensureTriangleCapacity();
        Renderer.builders.get(Renderer.mode).mesh().triangle(
                Renderer.builders.get(Renderer.mode).mesh().vec3(x0, y0, z0).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).mesh().vec3(x1, y1, z1).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).mesh().vec3(x2, y2, z2).color(Renderer.color).next()
        );
    }

    public static void quad(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        Renderer.quad(
                p0.x(), p0.y(), p0.z(),
                p1.x(), p1.y(), p1.z(),
                p2.x(), p2.y(), p2.z(),
                p3.x(), p3.y(), p3.z()
        );
    }

    public static void quad(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3
    ) {
        Renderer.builders.get(Renderer.mode).mesh().ensureQuadCapacity();
        Renderer.builders.get(Renderer.mode).mesh().quad(
                Renderer.builders.get(Renderer.mode).mesh().vec3(x0, y0, z0).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).mesh().vec3(x1, y1, z1).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).mesh().vec3(x2, y2, z2).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).mesh().vec3(x3, y3, z3).color(Renderer.color).next()
        );
    }

    public static void cuboid(Vec3 pos, Vec3 size) {
        Renderer.cuboid(
                pos.x(), pos.y(), pos.z(),
                size.x(), size.y(), size.z()
        );
    }

    public static void cuboid(double x, double y, double z, double width, double height, double length) {
        double x0 = x - width  / 2.0; double x1 = x + width  / 2.0;
        double y0 = y - height / 2.0; double y1 = y + height / 2.0;
        double z0 = z - length / 2.0; double z1 = z + length / 2.0;

        if(Renderer.mode.ordinal() < Mode.values().length / 2) {
            // bottom square
            Renderer.line(x0, y0, z0, x1, y0, z0);
            Renderer.line(x1, y0, z0, x1, y0, z1);
            Renderer.line(x1, y0, z1, x0, y0, z1);
            Renderer.line(x0, y0, z1, x0, y0, z0);

            // vertical lines
            Renderer.line(x0, y0, z0, x0, y1, z0);
            Renderer.line(x0, y0, z1, x0, y1, z1);
            Renderer.line(x1, y0, z0, x1, y1, z0);
            Renderer.line(x1, y0, z1, x1, y1, z1);

            // top square
            Renderer.line(x0, y1, z0, x1, y1, z0);
            Renderer.line(x1, y1, z0, x1, y1, z1);
            Renderer.line(x1, y1, z1, x0, y1, z1);
            Renderer.line(x0, y1, z1, x0, y1, z0);
        } else {
            // top square
            Renderer.quad(
                    x0, y1, z0,
                    x0, y1, z1,
                    x1, y1, z1,
                    x1, y1, z0
            );

            // bottom square
            Renderer.quad(
                    x0, y0, z0,
                    x1, y0, z0,
                    x1, y0, z1,
                    x0, y0, z1
            );

            // east square
            Renderer.quad(
                    x1, y0, z0,
                    x1, y1, z0,
                    x1, y1, z1,
                    x1, y0, z1
            );

            // west square
            Renderer.quad(
                    x0, y0, z0,
                    x0, y0, z1,
                    x0, y1, z1,
                    x0, y1, z0
            );

            // south square
            Renderer.quad(
                    x0, y0, z1,
                    x1, y0, z1,
                    x1, y1, z1,
                    x0, y1, z1
            );

            // north square
            Renderer.quad(
                    x0, y0, z0,
                    x0, y1, z0,
                    x1, y1, z0,
                    x1, y0, z0
            );
        }
    }

    public static void block(int x, int y, int z) {
        Renderer.cuboid(x + 0.5, y + 0.5, z + 0.5, 1, 1, 1);
    }

    public static void block(BlockState state, BlockPos pos) {
        VoxelShape shape = state.getShape(RubyClient.client.level, pos);
        for(AABB box : shape.toAabbs()) {
            double width  = box.maxX - box.minX;
            double height = box.maxY - box.minY;
            double length = box.maxZ - box.minZ;

            Renderer.cuboid(
                    pos.getX() + 0.5 * width  + box.minX,
                    pos.getY() + 0.5 * height + box.minY,
                    pos.getZ() + 0.5 * length + box.minZ,
                    width, height, length
            );
        }
    }

    public static void face(BlockPos pos, Direction dir) {
        double x0 = pos.getX(); double x1 = pos.getX() + 1;
        double y0 = pos.getY(); double y1 = pos.getY() + 1;
        double z0 = pos.getZ(); double z1 = pos.getZ() + 1;

        if(Renderer.mode.ordinal() < Mode.values().length / 2) {
            switch(dir) {
                case DOWN -> {
                    Renderer.line(x0, y0, z0, x1, y0, z0);
                    Renderer.line(x1, y0, z0, x1, y0, z1);
                    Renderer.line(x1, y0, z1, x0, y0, z1);
                    Renderer.line(x0, y0, z1, x0, y0, z0);
                }

                case UP -> {
                    Renderer.line(x0, y1, z0, x1, y1, z0);
                    Renderer.line(x1, y1, z0, x1, y1, z1);
                    Renderer.line(x1, y1, z1, x0, y1, z1);
                    Renderer.line(x0, y1, z1, x0, y1, z0);
                }

                case NORTH -> {
                    Renderer.line(x0, y0, z0, x0, y1, z0);
                    Renderer.line(x1, y0, z0, x1, y1, z0);
                    Renderer.line(x0, y0, z0, x1, y0, z0);
                    Renderer.line(x0, y1, z0, x1, y1, z0);
                }

                case SOUTH -> {
                    Renderer.line(x0, y0, z1, x0, y1, z1);
                    Renderer.line(x1, y0, z1, x1, y1, z1);
                    Renderer.line(x0, y0, z1, x1, y0, z1);
                    Renderer.line(x0, y1, z1, x1, y1, z1);
                }

                case EAST -> {
                    Renderer.line(x1, y0, z0, x1, y0, z1);
                    Renderer.line(x1, y1, z0, x1, y1, z1);
                    Renderer.line(x1, y0, z0, x1, y1, z0);
                    Renderer.line(x1, y0, z1, x1, y1, z1);
                }

                case WEST -> {
                    Renderer.line(x0, y0, z0, x0, y0, z1);
                    Renderer.line(x0, y1, z0, x0, y1, z1);
                    Renderer.line(x0, y0, z0, x0, y1, z0);
                    Renderer.line(x0, y0, z1, x0, y1, z1);
                }
            }
        } else {
            switch(dir) {
                case UP -> Renderer.quad(
                        x0, y1, z0,
                        x0, y1, z1,
                        x1, y1, z1,
                        x1, y1, z0
                );

                case DOWN -> Renderer.quad(
                        x0, y0, z0,
                        x1, y0, z0,
                        x1, y0, z1,
                        x0, y0, z1
                );

                case EAST -> Renderer.quad(
                        x1, y0, z0,
                        x1, y1, z0,
                        x1, y1, z1,
                        x1, y0, z1
                );

                case WEST -> Renderer.quad(
                        x0, y0, z0,
                        x0, y0, z1,
                        x0, y1, z1,
                        x0, y1, z0
                );

                case SOUTH -> Renderer.quad(
                        x0, y0, z1,
                        x1, y0, z1,
                        x1, y1, z1,
                        x0, y1, z1
                );

                case NORTH -> Renderer.quad(
                        x0, y0, z0,
                        x0, y1, z0,
                        x1, y1, z0,
                        x1, y0, z0
                );
            }
        }
    }
}
