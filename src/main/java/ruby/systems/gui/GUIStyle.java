package ruby.systems.gui;

import ruby.RubyClient;
import ruby.systems.gui.text.FontRenderer;

import java.awt.*;

public record GUIStyle(
        int ruby,
        int rubyHover,
        int rubyBg,
        int rubyActive,
        int bgPanel,
        int bgBase,
        int bgHover,
        int bgElevated,
        int text,
        int textBright,
        int textMuted,
        int catInactive,
        int arrowInactive,
        int borderPanel,
        int borderSubtle,
        int border,
        int trackOff,
        int overlayDim,
        FontRenderer logoFont,
        FontRenderer subHeaderFont,
        FontRenderer bodyFont,
        FontRenderer labelFont,
        FontRenderer badgeFont,
        FontRenderer profileFont,
        FontRenderer monospaceFont
) {
    public static final int RUBY = 0xFFA82938;
    public static final int RUBY_HOVER = 0xFFC23344;

    public static final int RADIUS_PANEL = 8;
    public static final int RADIUS_ROW = 6;
    public static final int RADIUS_BADGE = 3;
    public static final int RADIUS_PILL = 7;

    public static GUIStyle DEFAULT = createDefault();

    private static GUIStyle createDefault() {
        FontRenderer logo = FontRenderer.create(
                RubyClient.getResourceStream("fonts/DMSans-SemiBold.ttf"), "logo", 17, Font.BOLD);
        FontRenderer sub = FontRenderer.create(
                RubyClient.getResourceStream("fonts/DMSans-SemiBold.ttf"), "subheader", 13, Font.BOLD);
        FontRenderer body = FontRenderer.create(
                RubyClient.getResourceStream("fonts/DMSans-Medium.ttf"), "body", 13, Font.PLAIN);
        FontRenderer label = FontRenderer.create(
                RubyClient.getResourceStream("fonts/DMSans-SemiBold.ttf"), "label", 10, Font.BOLD);
        FontRenderer badge = FontRenderer.create(
                RubyClient.getResourceStream("fonts/DMSans-Medium.ttf"), "badge", 10, Font.PLAIN);
        FontRenderer profile = FontRenderer.create(
                RubyClient.getResourceStream("fonts/DMSans-Medium.ttf"), "profile", 12, Font.PLAIN);
        FontRenderer mono = FontRenderer.create(
                RubyClient.getResourceStream("fonts/JetBrainsMono.ttf"), "monospace", 12, Font.PLAIN);

        if (logo == null || sub == null || body == null || label == null || badge == null || profile == null || mono == null) {
            throw new IllegalStateException("Failed to load GUI fonts from /assets/ruby/fonts/");
        }

        return new GUIStyle(
                RUBY, RUBY_HOVER,
                0xFF0F0C0D, 0xFF141012,
                0xFF090809, 0xFF050405, 0xFF121012, 0xFF0E0C0D,
                0xFFB8B4B5, 0xFFD4D0D1, 0xFF5A5557, 0xFF8A8688, 0xFF333333,
                0x06FFFFFF, 0xFF121012, 0xFF181416,
                0xFF1A1617,
                0x88000000,
                logo, sub, body, label, badge, profile, mono
        );
    }

    private static GUIStyle current = GUIStyle.DEFAULT;

    public static GUIStyle get() {
        return GUIStyle.current;
    }

    public static GUIStyle set(GUIStyle style) {
        GUIStyle last = GUIStyle.current;
        GUIStyle.current = style;
        return last;
    }

    public static int withAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
