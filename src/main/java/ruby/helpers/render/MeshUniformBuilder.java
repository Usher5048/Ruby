package ruby.helpers.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class MeshUniformBuilder {
    public static final int SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .get();

    private static final Matrices MATRICES = new Matrices();
    private static final DynamicGpuDataStorageMapped<Matrices> STORAGE = new DynamicGpuDataStorageMapped<>(
            "Ruby Mesh UBO",
            SIZE,
            GpuBuffer.USAGE_UNIFORM,
            16
    );

    public static void flipFrame() {
        STORAGE.endFrame();
    }

    public static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
        MATRICES.proj = proj;
        MATRICES.modelView = modelView;
        return STORAGE.writeData(MATRICES);
    }

    private static final class Matrices implements DynamicGpuDataStorage.DynamicGpuData {
        private Matrix4f proj;
        private Matrix4f modelView;

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putMat4f(this.proj)
                    .putMat4f(this.modelView);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
