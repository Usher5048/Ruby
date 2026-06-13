package ruby.helpers.render;

import net.minecraft.client.render.VertexConsumer;

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
    public VertexConsumer vertex(float x, float y, float z) {
        return delegate.vertex(x, y, z);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return delegate.color(red, green, blue, scaleAlpha(alpha));
    }

    @Override
    public VertexConsumer color(int argb) {
        int a = (argb >>> 24) & 0xFF;
        return delegate.color((scaleAlpha(a) << 24) | (argb & 0x00FFFFFF));
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return delegate.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return delegate.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return delegate.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return delegate.normal(x, y, z);
    }

    @Override
    public VertexConsumer lineWidth(float width) {
        return delegate.lineWidth(width);
    }

    private int scaleAlpha(int original) {
        return Math.min(255, (original * alpha) / 255);
    }
}
