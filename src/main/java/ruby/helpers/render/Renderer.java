package ruby.helpers.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.shape.VoxelShape;
import ruby.RubyClient;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

public class Renderer {
    private static final RenderPipeline TRIANGLES_NO_DEPTH = RenderPipeline.builder()
            .withLocation(RubyClient.identifier("pipeline/triangle"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(RubyClient.identifier("pos_color"))
            .withFragmentShader(RubyClient.identifier("pos_color"))
            .withUniform("meshData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private static final RenderPipeline TRIANGLES_DEPTH = RenderPipeline.builder()
            .withLocation(RubyClient.identifier("pipeline/triangle_depth"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
            .withVertexShader(RubyClient.identifier("pos_color"))
            .withFragmentShader(RubyClient.identifier("pos_color"))
            .withUniform("meshData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private static final RenderPipeline LINES_NO_DEPTH = RenderPipeline.builder()
            .withLocation(RubyClient.identifier("pipeline/lines"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withVertexShader(RubyClient.identifier("pos_color"))
            .withFragmentShader(RubyClient.identifier("pos_color"))
            .withUniform("meshData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private static final RenderPipeline LINES_DEPTH = RenderPipeline.builder()
            .withLocation(RubyClient.identifier("pipeline/lines_depth"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withVertexShader(RubyClient.identifier("pos_color"))
            .withFragmentShader(RubyClient.identifier("pos_color"))
            .withUniform("meshData", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private static Matrix4f projection;
    private static Vec3d screenCenter;
    private static Matrix4f position;
    public static void updateMatrices(Matrix4f projection, Matrix4f position) {
        Renderer.projection = projection;
        Renderer.position = position;

        Matrix4f invProj = new Matrix4f(Renderer.projection).invert();
        Matrix4f invView = new Matrix4f(Renderer.position).invert();

        Vector4f center = new Vector4f(0, 0, 0.5f, 1).mul(invProj).mul(invView);
        center.div(center.w);

        Vec3d cam = RubyClient.client.gameRenderer.getCamera().getCameraPos();
        Renderer.screenCenter = new Vec3d(
                cam.getX() + center.x,
                cam.getY() + center.y,
                cam.getZ() + center.z
        );
    }

    public static Matrix4f projection() {
        return Renderer.projection;
    }

    public static Matrix4f view() {
        return Renderer.position;
    }
    public static Vec3d getScreenCenter() {
        return Renderer.screenCenter;
    }
    public static Vec3d screenToWorld(Vec3d pos) {
        Matrix4f invProj = new Matrix4f(Renderer.projection).invert();
        Matrix4f invView = new Matrix4f(Renderer.position).invert();

        pos = new Vec3d(
                2 * (pos.getX() / RubyClient.client.getWindow().getWidth() - 0.5),
                -2 * (pos.getY() / RubyClient.client.getWindow().getHeight() - 0.5),
                pos.getZ()
        );

        Vector4f center = new Vector4f(
                (float) pos.getX(), (float) pos.getY(),
                (float) pos.getZ(), 1
        ).mul(invProj).mul(invView);

        center = center.div(center.w);

        Vec3d cam = RubyClient.client.gameRenderer.getCamera().getCameraPos();
        return new Vec3d(
                cam.getX() + center.x,
                cam.getY() + center.y,
                cam.getZ() + center.z
        );
    }

    public static Vec2f worldToScreen(Vec3d pos) {
        Vec3d cam = RubyClient.client.gameRenderer.getCamera().getCameraPos();
        Vector4f vec = new Vector4f(
                (float) (pos.getX() - cam.getX()),
                (float) (pos.getY() - cam.getY()),
                (float) (pos.getZ() - cam.getZ()),
                1
        );

        Renderer.position.transform(vec);
        Renderer.projection.transform(vec);

        if(vec.w <= 0) return null;

        float ndcX = vec.x / vec.w;
        float ndcY = vec.y / vec.w;

        int screenX = (int) (     (ndcX * 0.5f + 0.5f)  * RubyClient.client.getWindow().getWidth());
        int screenY = (int) ((1 - (ndcY * 0.5f + 0.5f)) * RubyClient.client.getWindow().getHeight());

        return new Vec2f(screenX, screenY);
    }

    public enum Mode {
        STROKE_ALWAYS_ON_TOP,
        STROKE_RESPECT_DEPTH,
        FILL_ALWAYS_ON_TOP,
        FILL_RESPECT_DEPTH
    }

    private static final Map<Mode, Pair<RenderPipeline, MeshBuilder>> builders = new HashMap<>();
    static {
        Renderer.builders.put(Mode.STROKE_RESPECT_DEPTH, new Pair<>(
                Renderer.LINES_DEPTH, new MeshBuilder(Renderer.LINES_DEPTH)));

        Renderer.builders.put(Mode.FILL_RESPECT_DEPTH, new Pair<>(
                Renderer.TRIANGLES_DEPTH, new MeshBuilder(Renderer.TRIANGLES_DEPTH)));

        Renderer.builders.put(Mode.STROKE_ALWAYS_ON_TOP, new Pair<>(
                Renderer.LINES_NO_DEPTH, new MeshBuilder(Renderer.LINES_NO_DEPTH)));

        Renderer.builders.put(Mode.FILL_ALWAYS_ON_TOP, new Pair<>(
                Renderer.TRIANGLES_NO_DEPTH, new MeshBuilder(Renderer.TRIANGLES_NO_DEPTH)));
    }

    public static void begin() {
        for(Pair<RenderPipeline, MeshBuilder> builderPair : Renderer.builders.values())
            builderPair.getRight().begin();
    }

    public static void end(MatrixStack stack) {
        for(Pair<RenderPipeline, MeshBuilder> builderPair : Renderer.builders.values()) {
            MeshRenderer.begin()
                    .attachments(RubyClient.client.getFramebuffer())
                    .pipeline(builderPair.getLeft())
                    .mesh(builderPair.getRight(), stack)
                    .end();
        }
    }

    private static Mode mode;
    public static void setMode(Mode mode) {
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

    public static void line(Vec3d p0, Vec3d p1) {
        Renderer.line(
                p0.getX(), p0.getY(), p0.getZ(),
                p1.getX(), p1.getY(), p1.getZ()
        );
    }

    public static void line(double x0, double y0, double z0, double x1, double y1, double z1) {
        Renderer.builders.get(Renderer.mode).getRight().ensureLineCapacity();
        Renderer.builders.get(Renderer.mode).getRight().line(
                Renderer.builders.get(Renderer.mode).getRight().vec3(x0, y0, z0).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).getRight().vec3(x1, y1, z1).color(Renderer.color).next()
        );
    }

    public static void triangle(Vec3d p0, Vec3d p1, Vec3d p2) {
        Renderer.triangle(
                p0.getX(), p0.getY(), p0.getZ(),
                p1.getX(), p1.getY(), p1.getZ(),
                p2.getX(), p2.getY(), p2.getZ()
        );
    }

    public static void triangle(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2
    ) {
        Renderer.builders.get(Renderer.mode).getRight().ensureTriangleCapacity();
        Renderer.builders.get(Renderer.mode).getRight().triangle(
                Renderer.builders.get(Renderer.mode).getRight().vec3(x0, y0, z0).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).getRight().vec3(x1, y1, z1).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).getRight().vec3(x2, y2, z2).color(Renderer.color).next()
        );
    }

    public static void quad(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3) {
        Renderer.quad(
                p0.getX(), p0.getY(), p0.getZ(),
                p1.getX(), p1.getY(), p1.getZ(),
                p2.getX(), p2.getY(), p2.getZ(),
                p3.getX(), p3.getY(), p3.getZ()
        );
    }

    public static void quad(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3
    ) {
        Renderer.builders.get(Renderer.mode).getRight().ensureQuadCapacity();
        Renderer.builders.get(Renderer.mode).getRight().quad(
                Renderer.builders.get(Renderer.mode).getRight().vec3(x0, y0, z0).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).getRight().vec3(x1, y1, z1).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).getRight().vec3(x2, y2, z2).color(Renderer.color).next(),
                Renderer.builders.get(Renderer.mode).getRight().vec3(x3, y3, z3).color(Renderer.color).next()
        );
    }

    public static void cuboid(Vec3d pos, Vec3d size) {
        Renderer.cuboid(
                pos.getX(), pos.getY(), pos.getZ(),
                size.getX(), size.getY(), size.getZ()
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
        VoxelShape shape = state.getOutlineShape(RubyClient.client.world, pos);
        for(Box box : shape.getBoundingBoxes()) {
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
