package ruby.helpers.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;
import ruby.RubyClient;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Objects;

import net.minecraft.world.phys.Vec3;

public class MeshBuilder {
    private final VertexFormat format;
    private final int vertexSize;
    private final int indexCount;

    private ByteBuffer vertexBuf = null;
    private long vertexBufStart;
    private long vertexBufPtr;

    private ByteBuffer indexBuf = null;
    private long indexBufPtr;

    private int vertexIndex;
    private int indexIndex;

    private boolean building = false;
    private Vec3 cameraPos;

    public MeshBuilder(RenderPipeline pipeline) {
        this(Objects.requireNonNull(pipeline.getVertexFormatBinding(0)), pipeline.getPrimitiveTopology());
    }
    public MeshBuilder(VertexFormat format, PrimitiveTopology drawMode) {
        this.format = format;
        this.vertexSize = format.getVertexSize();
        this.indexCount = drawMode.primitiveLength;
    }

    public void begin() {
        if(this.building) return;
        this.building = true;

        this.vertexBufPtr = this.vertexBufStart;
        this.vertexIndex = 0;
        this.indexIndex = 0;

        Renderer.ensureCamera();
        this.cameraPos = Renderer.cameraPos();
    }

    public MeshBuilder vec3(double x, double y, double z) {
        MemoryUtil.memPutFloat(this.vertexBufPtr + 0, (float) (x - this.cameraPos.x()));
        MemoryUtil.memPutFloat(this.vertexBufPtr + 4, (float) (y - this.cameraPos.y()));
        MemoryUtil.memPutFloat(this.vertexBufPtr + 8, (float) (z - this.cameraPos.z()));

        this.vertexBufPtr += 12;
        return this;
    }

    public MeshBuilder color(int r, int g, int b) {
        MemoryUtil.memPutByte(this.vertexBufPtr + 0, (byte) r);
        MemoryUtil.memPutByte(this.vertexBufPtr + 1, (byte) g);
        MemoryUtil.memPutByte(this.vertexBufPtr + 2, (byte) b);
        MemoryUtil.memPutByte(this.vertexBufPtr + 3, (byte) 255);

        this.vertexBufPtr += 4;
        return this;
    }

    public MeshBuilder color(int r, int g, int b, int a) {
        MemoryUtil.memPutByte(this.vertexBufPtr + 0, (byte) r);
        MemoryUtil.memPutByte(this.vertexBufPtr + 1, (byte) g);
        MemoryUtil.memPutByte(this.vertexBufPtr + 2, (byte) b);
        MemoryUtil.memPutByte(this.vertexBufPtr + 3, (byte) a);

        this.vertexBufPtr += 4;
        return this;
    }

    public MeshBuilder color(int rgba) {
        byte a = (byte) ((rgba >> 24) & 0xFF);

        MemoryUtil.memPutByte(this.vertexBufPtr + 0, (byte) ((rgba >> 16) & 0xFF));
        MemoryUtil.memPutByte(this.vertexBufPtr + 1, (byte) ((rgba >>  8) & 0xFF));
        MemoryUtil.memPutByte(this.vertexBufPtr + 2, (byte) ((rgba >>  0) & 0xFF));
        MemoryUtil.memPutByte(this.vertexBufPtr + 3, a == 0 ? (byte) 255 : a);

        this.vertexBufPtr += 4;
        return this;
    }

    public int next() {
        return this.vertexIndex++;
    }
    public void line(int p1, int p2) {
        long ptr = this.indexBufPtr + (long) this.indexIndex * Integer.BYTES;

        MemoryUtil.memPutInt(ptr + 0, p1);
        MemoryUtil.memPutInt(ptr + 4, p2);

        this.indexIndex += 2;
    }

    public void triangle(int p1, int p2, int p3) {
        long p = this.indexBufPtr + (long) this.indexIndex * Integer.BYTES;

        MemoryUtil.memPutInt(p + 0, p1);
        MemoryUtil.memPutInt(p + 4, p2);
        MemoryUtil.memPutInt(p + 8, p3);

        this.indexIndex += 3;
    }

    public void quad(int p1, int p2, int p3, int p4) {
        long p = this.indexBufPtr + (long) this.indexIndex * Integer.BYTES;

        MemoryUtil.memPutInt(p + 0, p1);
        MemoryUtil.memPutInt(p + 4, p2);
        MemoryUtil.memPutInt(p + 8, p3);

        MemoryUtil.memPutInt(p + 12, p3);
        MemoryUtil.memPutInt(p + 16, p4);
        MemoryUtil.memPutInt(p + 20, p1);

        this.indexIndex += 6;
    }

    public void end() {
        if(!this.building) return;
        this.building = false;
    }

    public void ensureQuadCapacity() {
        this.ensureCapacity(4, 6);
    }
    public void ensureTriangleCapacity() {
        this.ensureCapacity(3, 3);
    }
    public void ensureLineCapacity() {
        this.ensureCapacity(2, 2);
    }

    public void ensureCapacity(int vertexCount, int indexCount) {
        if(this.vertexBuf == null) {
            this.vertexBuf = BufferUtils.createByteBuffer(256 * 4 * this.vertexSize);
            this.vertexBufStart = MemoryUtil.memAddress0(this.vertexBuf);
            this.vertexBufPtr = this.vertexBufStart;
        }

        if(this.indexBuf == null) {
            this.indexBuf = BufferUtils.createByteBuffer(512 * 4 * this.indexCount);
            this.indexBufPtr = MemoryUtil.memAddress0(this.indexBuf);
        }

        if((this.vertexIndex + vertexCount) * this.vertexSize >= this.vertexBuf.capacity()) {
            int off = (int) (this.vertexBufPtr - this.vertexBufStart);
            int newSize = Math.max(
                    this.vertexBuf.capacity() * 2,
                    this.vertexBuf.capacity() + vertexCount * this.vertexSize
            );

            ByteBuffer newVert = BufferUtils.createByteBuffer(newSize);
            MemoryUtil.memCopy(MemoryUtil.memAddress0(this.vertexBuf), MemoryUtil.memAddress0(newVert), off);

            this.vertexBuf = newVert;
            this.vertexBufStart = MemoryUtil.memAddress0(this.vertexBuf);
            this.vertexBufPtr = this.vertexBufStart + off;
        }

        if((this.indexIndex + indexCount) * Integer.BYTES >= this.indexBuf.capacity()) {
            int newSize = Math.max(
                    this.indexBuf.capacity() * 2,
                    this.indexBuf.capacity() + indexCount * Integer.BYTES
            );

            ByteBuffer newIndex = BufferUtils.createByteBuffer(newSize);
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress0(this.indexBuf),
                    MemoryUtil.memAddress0(newIndex),
                    (long) this.indexIndex * Integer.BYTES
            );

            this.indexBuf = newIndex;
            this.indexBufPtr = MemoryUtil.memAddress0(this.indexBuf);
        }
    }

    public GpuBuffer getVertexBuffer() {
        this.vertexBuf.limit((int) (this.vertexBufPtr - this.vertexBufStart));
        return RenderSystem.getDevice().createBuffer(
                () -> "Mesh vertex buffer",
                GpuBuffer.USAGE_VERTEX,
                this.vertexBuf
        );
    }

    public GpuBuffer getIndexBuffer() {
        this.indexBuf.limit(this.indexIndex * Integer.BYTES);
        return RenderSystem.getDevice().createBuffer(
                () -> "Mesh index buffer",
                GpuBuffer.USAGE_INDEX,
                this.indexBuf
        );
    }

    public GpuBufferSlice uploadVertexBuffer(CommandEncoder encoder) {
        this.vertexBuf.limit((int) (this.vertexBufPtr - this.vertexBufStart));
        return encoder.transientMemory().uploadGpu(this.vertexBuf, this.vertexSize, GpuBuffer.USAGE_VERTEX);
    }

    public GpuBufferSlice uploadIndexBuffer(CommandEncoder encoder) {
        this.indexBuf.limit(this.indexIndex * Integer.BYTES);
        return encoder.transientMemory().uploadGpu(this.indexBuf, Integer.BYTES, GpuBuffer.USAGE_INDEX);
    }

    public int indexCount() {
        return this.indexIndex;
    }
}
