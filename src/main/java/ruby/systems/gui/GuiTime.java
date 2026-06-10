package ruby.systems.gui;

public final class GuiTime {
    public static final float TICKS_PER_SECOND = 20f;

    private GuiTime() {}

    /** Converts Minecraft render tick delta to seconds. */
    public static float toSeconds(float tickDelta) {
        return tickDelta / GuiTime.TICKS_PER_SECOND;
    }
}
