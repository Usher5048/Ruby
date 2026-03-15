package ruby.systems.events.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import ruby.systems.events.Event;

public class Render3DEvent extends Event {
    private final VertexConsumerProvider consumerProvider;
    private final Matrix4f projectionMatrix;
    private final Matrix4f positionMatrix;
    private final MatrixStack matrixStack;
    private final float tickProgress;

    public Render3DEvent(
            float tickProgress,
            Matrix4f projectionMatrix, Matrix4f positionMatrix,
            MatrixStack matrixStack, VertexConsumerProvider consumerProvider
    ) {
        this.tickProgress = tickProgress;
        this.projectionMatrix = projectionMatrix;
        this.consumerProvider = consumerProvider;
        this.positionMatrix = positionMatrix;
        this.matrixStack = matrixStack;
    }

    public Matrix4f getPositionMatrix() {
        return this.positionMatrix;
    }
    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }
    public VertexConsumerProvider getConsumerProvider() {
        return this.consumerProvider;
    }
    public VertexConsumer getConsumer(RenderLayer layer) {
        return this.consumerProvider.getBuffer(layer);
    }
    public MatrixStack getMatrixStack() {
        return this.matrixStack;
    }
    public float getTickProgress() {
        return this.tickProgress;
    }
}
