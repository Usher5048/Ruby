package ruby.helpers.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

public class MeshRenderer {
    private static final MeshRenderer INSTANCE = new MeshRenderer();

    private GpuTextureView colorAttachment;
    private GpuTextureView depthAttachment;
    private Color clearColor;
    private RenderPipeline pipeline;
    private MeshBuilder mesh;
    private Matrix4f matrix;
    private final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final HashMap<String, Pair<GpuTextureView, GpuSampler>> samplers = new HashMap<>();

    private MeshRenderer() {}
    public static MeshRenderer begin() {
        return MeshRenderer.INSTANCE;
    }

    public MeshRenderer attachments(GpuTextureView color, GpuTextureView depth) {
        this.colorAttachment = color;
        this.depthAttachment = depth;
        return this;
    }

    public MeshRenderer attachments(Framebuffer framebuffer) {
        this.colorAttachment = framebuffer.getColorAttachmentView();
        this.depthAttachment = framebuffer.getDepthAttachmentView();
        return this;
    }

    public MeshRenderer clearColor(Color color) {
        this.clearColor = color;
        return this;
    }

    public MeshRenderer pipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh) {
        this.mesh = mesh;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, Matrix4f matrix) {
        this.matrix = matrix;
        this.mesh = mesh;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, MatrixStack matrices) {
        this.matrix = matrices.peek().getPositionMatrix();
        this.mesh = mesh;
        return this;
    }

    public MeshRenderer transform(Matrix4f matrix) {
        this.matrix = matrix;
        return this;
    }

    public MeshRenderer transform(MatrixStack matrices) {
        this.matrix = matrices.peek().getPositionMatrix();
        return this;
    }

    public MeshRenderer uniform(String name, GpuBufferSlice slice) {
        this.uniforms.put(name, slice);
        return this;
    }

    public MeshRenderer sampler(String name, GpuTextureView view, GpuSampler sampler) {
        this.samplers.put(name, new Pair<>(view, sampler));
        return this;
    }

    private RenderPass makePass(Supplier<String> ls, GpuTextureView ca, OptionalInt cc) {
        return RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(ls, ca, cc);
    }

    private RenderPass makePass(Supplier<String> ls, GpuTextureView ca, OptionalInt cc, GpuTextureView da) {
        return RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(ls, ca, cc, da, OptionalDouble.empty());
    }

    public void end() {
        if(this.mesh == null) return;
        this.mesh.end();

        int indexCount = this.mesh.indexCount();
        if(indexCount <= 0) return;

        RenderSystem.getModelViewStack().pushMatrix();
        if(this.matrix != null) RenderSystem.getModelViewStack().mul(this.matrix);

        OptionalInt clearColor = this.clearColor != null ? OptionalInt.of(ColorHelper.getArgb(
                this.clearColor.getAlpha(),
                this.clearColor.getRed(),
                this.clearColor.getGreen(),
                this.clearColor.getBlue()
        )) : OptionalInt.empty();

        GpuBufferSlice slice = MeshUniformBuilder.write(
                Renderer.projection(),
                RenderSystem.getModelViewStack()
        );

        GpuBuffer vertexBuf = this.mesh.getVertexBuffer();
        GpuBuffer indexBuf = this.mesh.getIndexBuffer();

        RenderPass pass = (this.depthAttachment != null && this.pipeline.wantsDepthTexture()) ?
                this.makePass(() -> "Ruby Render Pass", this.colorAttachment, clearColor, this.depthAttachment) :
                this.makePass(() -> "Ruby Render Pass", this.colorAttachment, clearColor);

        pass.setPipeline(this.pipeline);
        pass.setUniform("meshData", slice);

        for(String key : this.uniforms.keySet()) pass.setUniform(key, this.uniforms.get(key));
        for(String key : this.samplers.keySet())
            pass.bindTexture(key, this.samplers.get(key).getLeft(), this.samplers.get(key).getRight());

        pass.setVertexBuffer(0, vertexBuf);
        pass.setIndexBuffer(indexBuf, VertexFormat.IndexType.INT);
        pass.drawIndexed(0, 0, indexCount, 1);

        pass.close();
        RenderSystem.getModelViewStack().popMatrix();

        this.colorAttachment = null;
        this.depthAttachment = null;
        this.clearColor = null;
        this.pipeline = null;
        this.mesh = null;
        this.matrix = null;
        this.uniforms.clear();
        this.samplers.clear();
    }
}
