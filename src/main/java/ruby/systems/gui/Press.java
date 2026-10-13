package ruby.systems.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import ruby.systems.gui.icons.Lucide;
import ruby.systems.gui.text.FontRenderer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A short press. The control starts slightly small and eases back out immediately. */
public final class Press {
    private static final float DURATION = 0.11f;
    private static final Map<String, Anim> ANIMS = new ConcurrentHashMap<>();

    private Press() {}

    public static void down(String id) {
        Anim anim = ANIMS.computeIfAbsent(id, key -> new Anim());
        anim.start = System.nanoTime();
    }

    public static void icon(GuiGraphicsExtractor context, String name, int x, int y, int color, String id) {
        float press = Press.sample(id);
        int tint = Motion.mix(color, Press.bright(color), press);
        Lucide.draw(context, name, x, y, Lucide.UI, tint, press);
    }

    public static void label(FontRenderer font, GuiGraphicsExtractor context, String text, int x, int y, int color, String id) {
        float press = Press.sample(id);
        int tint = Motion.mix(color, Press.bright(color), press);
        if (press < 0.02f) {
            font.draw(context, text, x, y, tint);
            return;
        }
        float scale = 1f - 0.1f * press;
        int w = font.getWidth(text);
        int h = font.fontHeight;
        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x + w / 2f, y + h / 2f);
        pose.scale(scale, scale);
        font.draw(context, text, -w / 2, -h / 2, tint);
        pose.popMatrix();
    }

    public static float sample(String id) {
        Anim anim = ANIMS.get(id);
        if (anim == null) return 0f;
        float t = (System.nanoTime() - anim.start) / 1_000_000_000f / DURATION;
        if (t >= 1f) {
            ANIMS.remove(id);
            return 0f;
        }
        return 1f - Motion.out(t);
    }

    private static int bright(int color) {
        return Motion.mix(color, 0xFFFFFFFF, 0.55f);
    }

    private static final class Anim {
        long start;
    }
}
