package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import ruby.systems.config.BooleanValue;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;

/** Hold bind and Show toasts share one row. */
public class SettingPairWindow extends SettingWindow {

    private final BooleanValue right;
    private final String rightLabel;
    private final Switch leftSwitch = new Switch();
    private final Switch rightSwitch = new Switch();

    public SettingPairWindow(int x, int y, int width, BooleanValue left, BooleanValue right) {
        super(x, y, width, ROW, left);
        this.right = right;
        this.rightLabel = "Toasts";
        this.handleChildren = false;
        this.leftSwitch.snap(left.value());
        this.rightSwitch.snap(right.value());
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        if (this.getHeight() <= 0) return;
        GUIStyle style = GUIStyle.get();
        this.clip(context);
        this.paintHover(context, mouseX, mouseY, dt);

        float seconds = GuiTime.toSeconds(dt);
        boolean leftOn = (boolean) this.value.value();
        boolean rightOn = this.right.value();
        this.leftSwitch.advance(leftOn, seconds);
        this.rightSwitch.advance(rightOn, seconds);

        this.beginItem(context);
        int designW = this.designWidth();
        int mid = designW / 2;
        this.paintHalf(context, style, "Hold", this.leftSwitch, PAD, mid, "pairL:" + System.identityHashCode(this));
        this.paintHalf(context, style, this.rightLabel, this.rightSwitch, mid, designW, "pairR:" + System.identityHashCode(this));
        this.endItem(context);
        this.unclip(context);
    }

    private void paintHalf(
            GuiGraphicsExtractor context, GUIStyle style, String label, Switch sw,
            int left, int right, String id
    ) {
        int swX = right - PAD - SettingToggleWindow.trackWidth();
        int swY = (this.scaled(ROW) - SettingToggleWindow.trackHeight()) / 2;
        int textY = (this.scaled(ROW) - style.labelFont().fontHeight) / 2;
        String name = this.fit(style.labelFont(), label, swX - 8 - left);
        int color = Motion.mix(style.textMuted(), style.text(), this.hoverMix());
        style.labelFont().draw(context, name, left, textY, color);
        SettingToggleWindow.paintSwitch(
                context, style, swX, swY, sw.shown, sw.stretch, Press.sample(id), sw.to < sw.from);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (click.y() < 0 || click.y() >= this.getHeight()) return false;
        if (click.x() < this.getWidth() / 2.0) {
            Press.down("pairL:" + System.identityHashCode(this));
            ((BooleanValue) this.value).setValue(!(boolean) this.value.value());
        } else {
            Press.down("pairR:" + System.identityHashCode(this));
            this.right.setValue(!this.right.value());
        }
        return true;
    }

    private static final class Switch {
        private float shown;
        private float from;
        private float to;
        private float clock = 1f;
        private float stretch;

        private void snap(boolean on) {
            float v = on ? 1f : 0f;
            this.shown = v;
            this.from = v;
            this.to = v;
        }

        private void advance(boolean enabled, float seconds) {
            float target = enabled ? 1f : 0f;
            if (!ruby.systems.gui.ClientSettings.get().animations()) {
                this.snap(enabled);
                this.stretch = 0f;
                return;
            }
            if (target != this.to) {
                this.from = this.shown;
                this.to = target;
                this.clock = 0f;
            }
            float before = this.shown;
            this.clock = Math.min(1f, this.clock + seconds / 0.2f);
            this.shown = this.from + (this.to - this.from) * Motion.out(this.clock);
            float speed = seconds <= 0f ? 0f : Math.abs(this.shown - before) / seconds;
            this.stretch = Motion.follow(this.stretch, Math.clamp(speed * 0.09f, 0f, 1f), seconds, 0.06f);
        }
    }
}
