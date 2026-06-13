package ruby.helpers.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.gl.DynamicUniformStorage;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class MeshUniformBuilder {
    public static final int size = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .get();

    private static final Data data = new Data();
    private static final DynamicUniformStorage<Data> storage = new DynamicUniformStorage<>(
            "Ruby Mesh Uniforms",
            MeshUniformBuilder.size, 16
    );

    public static void flipFrame() {
        MeshUniformBuilder.storage.clear();
    }
    public static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
        MeshUniformBuilder.data.proj = proj;
        MeshUniformBuilder.data.modelView = modelView;

        return MeshUniformBuilder.storage.write(MeshUniformBuilder.data);
    }

    private static final class Data implements DynamicUniformStorage.Uploadable {
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
