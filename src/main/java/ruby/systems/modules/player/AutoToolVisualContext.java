package ruby.systems.modules.player;

public final class AutoToolVisualContext {
    private static int depth;

    private AutoToolVisualContext() {
    }

    public static void enter() {
        depth++;
    }

    public static void exit() {
        if (depth > 0) depth--;
    }

    public static boolean isActive() {
        return depth > 0;
    }
}
