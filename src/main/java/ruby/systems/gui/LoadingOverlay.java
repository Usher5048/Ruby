package ruby.systems.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import ruby.RubyClient;
import ruby.systems.gui.text.FontRenderer;
import ruby.systems.gui.windows.WindowedOverlay;

import java.util.ArrayList;
import java.util.List;

// TODO: Possibly redo this to be less intrusive and use less busy waiting
public class LoadingOverlay extends WindowedOverlay {
    public interface ClientLoader {
        void load(LoadingOverlay overlay);
    }

    private static FontRenderer logFont = null;
    private static FontRenderer welcomeFont = null;

    private final Thread loadThread;
    private final List<Text> logs = new ArrayList<>();
    private final List<Integer> logRemoveTimers = new ArrayList<>();
    private final List<Long> logSlideTimers = new ArrayList<>();

    private long start = 0;
    private boolean loaded = false;
    private boolean ranLoadThread = false;

    private int welcomeOpacity = 1;
    private int opacity = 0xFF;

    public LoadingOverlay(Screen parent, ClientLoader loader) {
        super(parent);

        this.loadThread = new Thread(() -> {
            try { Thread.sleep(1000); } catch(Exception ignored) {}

            if(loader != null) loader.load(this);
            this.loaded = true;
        });
    }

    private double timingFunction(double t, double n) {
        if(t < 0.5) return Math.pow(2, n - 1) * Math.pow(t, n);
        return 1 - Math.pow(2, n - 1) * Math.pow(1 - t, n);
    }

    public void log(String str) {
        this.log(str, 0xFFFFFFFF);
    }
    public void log(String str, int color) {
        this.logs.add(Text.literal(str).withColor(color));
        this.logRemoveTimers.add(100); // 100 ticks = 5 seconds
        this.logSlideTimers.add(System.currentTimeMillis());

        try { Thread.sleep(100); } catch(Exception ignored) {}
    }

    @Override
    public void onTick() {
        if(this.start == 0) this.start = System.currentTimeMillis();
        if(System.currentTimeMillis() - this.start < 1000) return;

        if(this.welcomeOpacity < 0xFF && !this.loaded)
            this.welcomeOpacity = (int) Math.ceil(this.welcomeOpacity * 1.5);

        for(int i = this.logRemoveTimers.size() - 1; i >= 0; i--) {
            int timer = this.logRemoveTimers.get(i) - 1;
            if(timer < 0) {
                long time = System.currentTimeMillis() - this.logSlideTimers.get(i);
                if(time >= 500) {
                    this.logRemoveTimers.remove(i);
                    this.logSlideTimers.remove(i);
                    this.logs.remove(i);
                }

                continue;
            }

            this.logRemoveTimers.set(i, timer);
        }

        if(this.loaded) {
            long now = System.currentTimeMillis();
            for(long logSlideTimer : this.logSlideTimers)
                if(now - logSlideTimer <= 1000) return;

            for(int i = 0; i < this.logRemoveTimers.size(); i++) {
                if(this.logRemoveTimers.get(i) > 0) this.logRemoveTimers.set(i, 0);
            }

            if(this.logs.size() > 0) return;

            this.welcomeOpacity = (int) (this.welcomeOpacity / 1.5);
            if(this.welcomeOpacity <= 0) {
                this.welcomeOpacity = 0;
                this.opacity = (int) (this.opacity / 1.3);
            }
        }
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        if(!this.ranLoadThread && this.welcomeOpacity >= 0xFF) {
            this.loadThread.start();
            this.welcomeOpacity = 0xFF;
            this.ranLoadThread = true;
        }

        if(this.opacity <= 0) {
            RubyClient.client.setOverlay(null);
            return;
        }

        int width = RubyClient.client.getWindow().getWidth();
        int height = RubyClient.client.getWindow().getHeight();

        context.fill(0, 0, width, height, this.opacity << 24);

        if(LoadingOverlay.welcomeFont == null) {
            LoadingOverlay.welcomeFont = FontRenderer.create(
                    RubyClient.getResourceStream("fonts/Nunito.ttf"),
                    "LoadingOverlayFont", 24
            );

            if(LoadingOverlay.welcomeFont == null) return;
        }

        String text = "Welcome";
        if(RubyClient.client.getSession().getXuid().isPresent())
            text += ", " + RubyClient.client.getSession().getUsername();

        LoadingOverlay.welcomeFont.draw(
                context, text,
                width / 2 - LoadingOverlay.welcomeFont.getWidth(text) / 2,
                height / 2 - LoadingOverlay.welcomeFont.fontHeight / 2,
                (this.welcomeOpacity << 24) | 0xFFFFFF
        );

        if(LoadingOverlay.logFont == null) {
            LoadingOverlay.logFont = FontRenderer.create(
                    RubyClient.getResourceStream("fonts/JetBrainsMono.ttf"),
                    "LoadingOverlayMonoFont", 16
            );

            if(LoadingOverlay.logFont == null) return;
        }

        int y = 1;
        for(int i = 0; i < this.logs.size(); i++) {
            Text log = this.logs.get(i);
            int color = log.getStyle().getColor().getRgb();

            if(this.logRemoveTimers.get(i) == 0) {
                this.logRemoveTimers.set(i, -1);
                this.logSlideTimers.set(i, System.currentTimeMillis());
            }

            long now = System.currentTimeMillis();
            double t = (now - this.logSlideTimers.get(i)) / 500.0;
            t = this.timingFunction(Math.min(t, 1), 2.3);

            int textWidth = LoadingOverlay.logFont.getWidth(log.getString());
            double x = MathHelper.lerp(t, -textWidth, 1);

            if(this.logRemoveTimers.get(i) <= 0)
                x = MathHelper.lerp(t, 1, -textWidth);

            LoadingOverlay.logFont.draw(
                    context,
                    log.getString(),
                    (int) x, y,
                    0xFF000000 | color
            );

            y += LoadingOverlay.logFont.fontHeight;
        }
    }
}
