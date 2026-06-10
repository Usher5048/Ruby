package ruby.systems.gui;

/**
 * Easing curves matching test.html {@code cubic-bezier(.32,.72,0,1)} transitions.
 */
public final class GuiEasing {
    private GuiEasing() {}

    public static float smooth(float t) {
        t = Math.clamp(t, 0f, 1f);
        return 1f - (float) Math.pow(1 - t, 3.2);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * smooth(t);
    }
}
