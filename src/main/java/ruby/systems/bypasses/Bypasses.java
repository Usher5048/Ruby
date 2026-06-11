package ruby.systems.bypasses;

public class Bypasses {
    public static Bypass NONE = new Bypass();
    public static Bypass VANILLA = new VanillaBypass();
    public static Bypass VULCAN = null;
    public static Bypass GRIM = new GrimBypass();

    private static Bypass current = Bypasses.GRIM;
    public static Bypass get() {
        return Bypasses.current;
    }
    public static Bypass set(Bypass style) {
        Bypass last = Bypasses.current;
        Bypasses.current = style;
        return last;
    }
}
