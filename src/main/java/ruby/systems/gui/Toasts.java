package ruby.systems.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ruby.RubyClient;
import ruby.systems.gui.icons.Lucide;
import ruby.systems.gui.text.FontRenderer;
import ruby.systems.gui.windows.ModuleTypeWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Top-right notices in the click GUI's type and motion. */
public final class Toasts {
    private static final float ENTER = Motion.ENTER;
    private static final float HOLD = 2.2f;
    private static final float EXIT = Motion.EXIT;
    private static final int MARGIN = 12;
    private static final int GAP = 6;
    private static final int ROW_H = 31;
    private static final int MAX = 30;
    private static final int MAX_TEXT = 280;

    private static final List<Entry> TOASTS = new ArrayList<>();
    private static long lastNanos;

    private Toasts() {}

    /** One-line toast, the same shape the click GUI buttons use. */
    public static void show(String icon, String text) {
        Toasts.toast(text).icon(icon).show();
    }

    /** Starts a toast. Call {@link Toast#show()} when it is ready. */
    public static Toast toast(String title) {
        return new Toast(title);
    }

    public static void render(GuiGraphicsExtractor context) {
        synchronized (TOASTS) {
            if (TOASTS.isEmpty()) return;

            long now = System.nanoTime();
            float dt = Toasts.lastNanos == 0L ? 0.016f : (now - Toasts.lastNanos) / 1_000_000_000f;
            Toasts.lastNanos = now;
            dt = Math.clamp(dt, 0f, 0.05f);

            int scale = Math.max(1, RubyClient.client.getWindow().getGuiScale());
            var pose = context.pose();
            pose.pushMatrix();
            pose.scale(1f / scale, 1f / scale);

            GUIStyle style = GUIStyle.get();
            int screenW = RubyClient.client.getWindow().getWidth();
            int y = MARGIN;
            for (int i = 0; i < TOASTS.size(); ) {
                Entry toast = TOASTS.get(i);
                toast.life += dt;
                float span = ENTER + toast.hold + EXIT;
                if (toast.life >= span) {
                    TOASTS.remove(i);
                    continue;
                }

                float shown;
                float slide;
                if (toast.life < ENTER) {
                    shown = Motion.out(toast.life / ENTER);
                    slide = (1f - shown) * 8f;
                } else if (toast.life < ENTER + toast.hold) {
                    shown = 1f;
                    slide = 0f;
                } else {
                    float u = (toast.life - ENTER - toast.hold) / EXIT;
                    shown = 1f - Motion.in(u);
                    slide = u * 6f;
                }
                if (shown > 0.01f) {
                    y = Toasts.draw(context, style, toast, screenW, y, shown, slide);
                }
                i++;
            }
            if (TOASTS.isEmpty()) Toasts.lastNanos = 0L;

            pose.popMatrix();
        }
    }

    private static int draw(GuiGraphicsExtractor context, GUIStyle style, Entry toast,
                            int screenW, int y, float shown, float slide) {
        boolean detailed = toast.detail != null && !toast.detail.isBlank();
        FontRenderer body = style.bodyFont();
        FontRenderer label = style.labelFont();
        int textX = 12 + Lucide.UI + 8;
        int maxText = Math.min(MAX_TEXT, Math.max(40, screenW - MARGIN * 2 - textX - 12));
        String title = Toasts.fit(body, toast.title, maxText);
        String detail = detailed ? Toasts.fit(label, toast.detail, maxText) : null;
        int textW = body.getWidth(title);
        if (detail != null) textW = Math.max(textW, label.getWidth(detail));

        int boxW = textX + textW + 12;
        int boxH = detailed ? 8 + body.fontHeight + 2 + label.fontHeight + 8 : ROW_H;
        int x = ClientSettings.get().toastLeft()
                ? MARGIN - Math.round(slide)
                : screenW - boxW - MARGIN + Math.round(slide);
        int bg = GUIStyle.withAlpha(style.bgPanel(), shown);
        int edge = GUIStyle.withAlpha(style.borderPanel(), shown);
        ModuleTypeWindow.drawRoundedPanel(context, x, y, x + boxW, y + boxH, GUIStyle.RADIUS_PANEL, bg, edge);

        int iconColor = toast.accent == 0 ? style.textMuted() : toast.accent;
        Lucide.draw(context, toast.icon, x + 12, y + (boxH - Lucide.UI) / 2, Lucide.UI,
                GUIStyle.withAlpha(iconColor, shown));

        if (!detailed) {
            int textY = y + (ROW_H - body.fontHeight) / 2;
            body.draw(context, title, x + textX, textY, GUIStyle.withAlpha(style.text(), shown));
        } else {
            int block = body.fontHeight + 2 + label.fontHeight;
            int top = y + (boxH - block) / 2;
            body.draw(context, title, x + textX, top, GUIStyle.withAlpha(style.text(), shown));
            label.draw(context, detail, x + textX, top + body.fontHeight + 2,
                    GUIStyle.withAlpha(style.textMuted(), shown));
        }
        return y + boxH + GAP;
    }

    private static String fit(FontRenderer font, String text, int maxW) {
        if (text == null || text.isEmpty()) return "";
        if (font.getWidth(text) <= maxW) return text;
        String ellipsis = "…";
        int end = text.length();
        while (end > 1 && font.getWidth(text.substring(0, end) + ellipsis) > maxW) end--;
        return text.substring(0, end) + ellipsis;
    }

    private static void push(Entry entry) {
        synchronized (TOASTS) {
            TOASTS.add(0, entry);
            int cap = Math.min(MAX, ClientSettings.get().maxToasts());
            while (TOASTS.size() > cap) TOASTS.remove(TOASTS.size() - 1);
        }
    }

    /**
     * A toast before it is shown. Set an icon, a second line, how long it stays,
     * and an accent, then call {@link #show()}.
     */
    public static final class Toast {
        private final String title;
        private String detail;
        private String icon = "circle-dot";
        private int accent;
        private float hold = -1f;
        private String source;
        private BooleanSupplier allow;

        private Toast(String title) {
            this.title = title == null ? "" : title;
        }

        /** Lucide icon name, such as {@code "check"} or {@code "user"}. */
        public Toast icon(String icon) {
            if (icon != null && !icon.isBlank()) this.icon = icon;
            return this;
        }

        /** Second line under the title. */
        public Toast detail(String detail) {
            this.detail = detail;
            return this;
        }

        /** How long the toast stays fully visible, in seconds. */
        public Toast seconds(float hold) {
            this.hold = Math.clamp(hold, 0.6f, 12f);
            return this;
        }

        /** Icon tint. The default is the muted text color. */
        public Toast accent(int argb) {
            this.accent = argb;
            return this;
        }

        /** Shown as the detail line when {@link #detail(String)} was not set. */
        public Toast source(String source) {
            this.source = source;
            return this;
        }

        /** Skips the toast when this returns false. */
        public Toast when(BooleanSupplier allow) {
            this.allow = allow;
            return this;
        }

        public void show() {
            if (this.title.isBlank()) return;
            if (!ClientSettings.get().toasts()) return;
            if (this.source != null && !ClientSettings.get().moduleToasts()) return;
            if (this.allow != null && !this.allow.getAsBoolean()) return;
            String detail = this.detail;
            if ((detail == null || detail.isBlank()) && this.source != null && !this.source.isBlank()) {
                detail = this.source;
            }
            float hold = this.hold < 0f ? ClientSettings.get().holdSeconds() : this.hold;
            Toasts.push(new Entry(this.icon, this.title, detail, this.accent, hold));
        }
    }

    private static final class Entry {
        final String icon;
        final String title;
        final String detail;
        final int accent;
        final float hold;
        float life;

        Entry(String icon, String title, String detail, int accent, float hold) {
            this.icon = icon;
            this.title = title;
            this.detail = detail;
            this.accent = accent;
            this.hold = hold;
        }
    }
}
