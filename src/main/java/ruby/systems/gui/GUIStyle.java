package ruby.systems.gui;

import ruby.RubyClient;
import ruby.systems.gui.text.FontRenderer;

public record GUIStyle(
        int headerBGColor, int headerTextColor,
        int subheaderTextColor,
        int bodyBGColor, int bodyTextColor,
        int enabledBGColor, int enabledColor,
        int disabledBGColor, int enabledBgColor,
        FontRenderer headerFont, FontRenderer subHeaderFont,
        FontRenderer bodyFont, FontRenderer monospaceFont
) {
    public static GUIStyle DEFAULT = new GUIStyle(
            0xFF111111, 0xFFFFAA00,
            0xFFCCCCCC,
            0xCC222222, 0xFFCCCCCC,
            0xCC333333, 0xFFFFAA00,
            0xff181818, 0xff131313,
                FontRenderer.create(RubyClient.getResourceStream("fonts/NunitoBold.ttf"),    "header",    28),
                FontRenderer.create(RubyClient.getResourceStream("fonts/Nunito.ttf"),        "subheader", 22),
                FontRenderer.create(RubyClient.getResourceStream("fonts/Nunito.ttf"),        "body",      18),
                FontRenderer.create(RubyClient.getResourceStream("fonts/JetBrainsMono.ttf"), "monospace", 16)
    );

    private static GUIStyle current = GUIStyle.DEFAULT;
    public static GUIStyle get() {
        return GUIStyle.current;
    }
    public static GUIStyle set(GUIStyle style) {
        GUIStyle last = GUIStyle.current;
        GUIStyle.current = style;
        return last;
    }
}
