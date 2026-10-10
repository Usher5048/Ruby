package ruby.helpers.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Multiplies vertex alpha for Xray semi-transparent blocks.
 */
public final class XrayAlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int alpha;

    public XrayAlphaVertexConsumer(VertexConsumer delegate, int alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return delegate.setColor(red, green, blue, scaleAlpha(alpha));
    }

    @Override
    public VertexConsumer setColor(int argb) {
        int a = (argb >>> 24) & 0xFF;
        return delegate.setColor((scaleAlpha(a) << 24) | (argb & 0x00FFFFFF));
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        return delegate.setUv3(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return delegate.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return delegate.setLineWidth(width);
    }

    private int scaleAlpha(int original) {
        return Math.min(255, (original * alpha) / 255);
    }
}
