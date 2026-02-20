package ruby.systems.gui;

import com.entity.eclipse.Eclipse;
import com.entity.eclipse.utils.render.font.FontRenderer;

public record GUIStyle(
        int headerBGColor, int headerTextColor,
        int subheaderTextColor,
        int bodyBGColor, int bodyTextColor,
        int enabledBGColor, int enabledColor,
        FontRenderer headerFont, FontRenderer subHeaderFont,
        FontRenderer bodyFont, FontRenderer monospaceFont
) {
    public static GUIStyle DEFAULT = new GUIStyle(
            0xFF111111, 0xFFFFAA00,
            0xFFCCCCCC,
            0xCC222222, 0xFFCCCCCC,
            0xCC333333, 0xFFFFAA00,
            FontRenderer.create(Eclipse.getResourceStream("fonts/NunitoBold.ttf"),    "Header",    28),
            FontRenderer.create(Eclipse.getResourceStream("fonts/Nunito.ttf"),        "Subheader", 22),
            FontRenderer.create(Eclipse.getResourceStream("fonts/Nunito.ttf"),        "Body",      18),
            FontRenderer.create(Eclipse.getResourceStream("fonts/JetBrainsMono.ttf"), "Monospace", 16)
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
