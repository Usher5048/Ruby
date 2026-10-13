package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import ruby.systems.gui.ClientSettings;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;
import ruby.systems.gui.ThemeManager;
import ruby.systems.gui.Toasts;
import ruby.systems.gui.icons.Lucide;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class ThemePanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 31;
    private static final int LAST_EXTRA = 4;

    private int openGroup = -1;
    private int openColor = -1;
    private int draggingHue = -1;
    private int draggingSv = -1;
    private float hue;
    private float saturation;
    private float brightness;
    private float contentAlpha = 1f;
    private float[] rowMix;

    private List<SettingsSectionWindow> sections;
    private int builtRevision = -1;
    private float resetMix;

    public ThemePanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    private void onSectionExpand(SettingsSectionWindow source) {
        if (!source.expanded) return;
        for (SettingsSectionWindow section : this.sections) {
            if (section != source && section.expanded) section.expanded = false;
        }
    }

    private void ensure() {
        int revision = ClientSettings.get().revision();
        if (this.sections == null || this.builtRevision != revision) {
            this.builtRevision = revision;
            this.sections = new ArrayList<>();
            this.windows().clear();
            int width = this.getWidth();
            for (ClientSettings.Section spec : ClientSettings.get().sections()) {
                SettingsSectionWindow section = new SettingsSectionWindow(spec.name(), spec.values(), width);
                this.sections.add(section);
                this.addWindow(section);
            }
            this.handleChildren = true;
        }
        for (SettingsSectionWindow section : this.sections) section.setAccordionHost(this::onSectionExpand);
    }

    @Override
    public int getHeight() {
        this.ensure();
        int h = ROW_H + LAST_EXTRA;
        for (SettingsSectionWindow section : this.sections) h += section.getHeight();
        this.height = h;
        return h;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        this.ensure();
        int y = 0;
        for (SettingsSectionWindow section : this.sections) {
            section.setContentAlpha(this.contentAlpha);
            section.setPosition(0, y);
            y += section.getHeight();
        }
        this.drawReset(context, y, mouseX, mouseY, GuiTime.toSeconds(dt));
    }

    private void drawReset(GuiGraphicsExtractor context, int y, int mouseX, int mouseY, float seconds) {
        GUIStyle style = GUIStyle.get();
        int innerR = this.getWidth() - INNER_L;
        int rowH = ROW_H + LAST_EXTRA;
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        this.resetMix = Motion.follow(this.resetMix, hovered ? 1f : 0f, seconds, Motion.HOVER);
        if (this.resetMix > 0.01f) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * this.resetMix), true);
        }

        int textColor = Motion.mix(style.textMuted(), style.text(), this.resetMix);
        int iconY = y + (ROW_H - Lucide.UI) / 2;
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        int resetColor = GUIStyle.withAlpha(textColor, this.contentAlpha);
        Press.icon(context, "rotate-ccw", PAD, iconY, resetColor, "settings-reset");
        Press.label(style.bodyFont(), context, "Reset colors", PAD + Lucide.UI + 8, textY, resetColor, "settings-reset");
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        this.ensure();
        int y = 0;
        for (SettingsSectionWindow section : this.sections) y += section.getHeight();
        if (click.y() < y || click.y() >= y + ROW_H + LAST_EXTRA) return false;
        Press.down("settings-reset");
        ThemeManager.get().resetDefaults();
        Toasts.show("rotate-ccw", "Reset colors");
        return true;
    }
}
