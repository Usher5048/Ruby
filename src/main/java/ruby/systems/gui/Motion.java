package ruby.systems.gui;

/**
 * Motion language for the click GUI.
 *
 * Entrances ease out and travel a few pixels. Exits are the same curve on a shorter
 * duration. Height changes are symmetric. Hovers, toggles, and scrolling follow a
 * target instead of running on their own clocks. Nothing bounces or overshoots.
 */
public final class Motion {
    /** Highlight and toggle color. */
    public static final float HOVER = 0.10f;
    /** Chevron, focus, small state changes. */
    public static final float FAST = 0.14f;
    /** Leave a view. */
    public static final float EXIT = 0.16f;
    /** Enter a view, including the GUI itself. */
    public static final float ENTER = 0.26f;
    /** Expand or collapse a row or panel. */
    public static final float EXPAND = 0.22f;
    /** Delay between list rows on enter. */
    public static final float STAGGER = 0.028f;
    /** Wheel and scrollbar settle. */
    public static final float SCROLL = 0.07f;
    /** How far an entrance rises, in pixels. */
    public static final float RISE = 6f;
    /** Scale at the start of an entrance. */
    public static final float SCALE_FROM = 0.98f;

    private Motion() {}

    /** Fast start, soft landing. Entrances and most fades. */
    public static float out(float t) {
        t = Math.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Soft start, fast landing. Used when a value itself is played backward. */
    public static float in(float t) {
        t = Math.clamp(t, 0f, 1f);
        return t * t * t;
    }

    /** Symmetric. Expand, collapse, and other height changes. */
    public static float standard(float t) {
        t = Math.clamp(t, 0f, 1f);
        if (t < 0.5f) return 4f * t * t * t;
        float u = -2f * t + 2f;
        return 1f - (u * u * u) / 2f;
    }

    /**
     * Moves {@code current} toward {@code target}. {@code response} is seconds, so the
     * speed does not depend on frame rate.
     */
    public static float follow(float current, float target, float dt, float response) {
        if (!ClientSettings.get().animations() || response <= 0f) return target;
        float t = 1f - (float) Math.exp(-dt / response);
        float next = current + (target - current) * t;
        if (Math.abs(next - target) < 0.001f) return target;
        return next;
    }

    public static int mix(int from, int to, float t) {
        t = Math.clamp(t, 0f, 1f);
        int a = mixChannel(from >>> 24, to >>> 24, t);
        int r = mixChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = mixChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = mixChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int mixChannel(int from, int to, float t) {
        return Math.clamp(Math.round(from + (to - from) * t), 0, 255);
    }
}
