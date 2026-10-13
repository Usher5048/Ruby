package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ruby.systems.config.ProfileManager;
import ruby.systems.gui.ClickGuiSelection;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;
import ruby.systems.gui.icons.Lucide;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.input.MouseButtonEvent;

public class CategorySidebarWindow extends Window {
    public static final int WIDTH = ClickGuiLayoutWindow.SIDEBAR_WIDTH;
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    public static final int HEADER_H = 41;
    private static final int ROW_H = 31;
    private static final int THEME_EXTRA = 3;
    private static final int MISC_LABEL_H = 19;
    private static final int PANEL_RADIUS = GUIStyle.RADIUS_PANEL;

    private final List<ModuleType> categories;
    private final ClickGuiSelection[] categorySels;
    private final ClickGuiSelection friendsSel = new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.FRIENDS);
    private final ClickGuiSelection profilesSel = new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.PROFILES);
    private final ClickGuiSelection themeSel = new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.THEME);
    private String profileRaw;
    private String profileLabel = "";
    private ClickGuiSelection selection;
    private boolean searchActive;
    private final Consumer<ClickGuiSelection> onSelect;
    private final Runnable onHeaderDrag;
    private float[] rowMix = new float[0];

    public CategorySidebarWindow(
            int x, int y,
            List<ModuleType> categories,
            ClickGuiSelection initial,
            Consumer<ClickGuiSelection> onSelect,
            Runnable onHeaderDrag
    ) {
        super(x, y, WIDTH, 400);
        this.categories = categories;
        this.categorySels = new ClickGuiSelection[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            this.categorySels[i] = new ClickGuiSelection.ModuleCategory(categories.get(i));
        }
        this.selection = initial;
        this.onSelect = onSelect;
        this.onHeaderDrag = onHeaderDrag;
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    public ClickGuiSelection selection() {
        return this.selection;
    }

    public void setSelection(ClickGuiSelection selection) {
        this.selection = selection;
    }

    public void setSearchActive(boolean searchActive) {
        this.searchActive = searchActive;
    }

    @Override
    public int getHeight() {
        int h = HEADER_H + this.categories.size() * ROW_H + 1 + MISC_LABEL_H + 3 * ROW_H + THEME_EXTRA;
        this.height = h;
        return h;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        GUIStyle style = GUIStyle.get();
        int w = WIDTH;
        int h = this.getHeight();
        int innerR = w - INNER_L;

        ModuleTypeWindow.drawRoundedPanel(context, 0, 0, w, h, PANEL_RADIUS, style.bgPanel(), style.borderPanel());

        context.enableScissor(INNER_L, INNER_L, innerR, h - INNER_L);

        context.fill(INNER_L, HEADER_H - 1, innerR, HEADER_H, style.borderSubtle());

        int logoY = (HEADER_H - style.logoFont().fontHeight) / 2;
        style.logoFont().draw(context, "Ruby", PAD, logoY, style.ruby());

        String rawProfile = ProfileManager.activeProfile();
        if (rawProfile == null) rawProfile = "";
        if (!rawProfile.equals(this.profileRaw)) {
            this.profileRaw = rawProfile;
            this.profileLabel = FriendsManager.capitalizeProfileName(rawProfile);
        }
        String profile = this.profileLabel;
        int profileW = style.profileFont().getWidth(profile);
        int profileY = (HEADER_H - style.profileFont().fontHeight) / 2;
        style.profileFont().draw(context, profile, w - PAD - profileW, profileY, style.textMuted());

        int y = HEADER_H;

        int row = 0;
        for (int i = 0; i < this.categories.size(); i++) {
            ModuleType type = this.categories.get(i);
            y = this.drawCategoryRow(context, style, innerR, y, mouseX, mouseY, type.toString(), this.categorySels[i], false, ROW_H, row++, dt);
        }

        context.fill(INNER_L, y, innerR, y + 1, style.borderSubtle());
        y += 1;

        style.labelFont().draw(context, "Client", PAD, y + 4, style.textMuted());
        y += MISC_LABEL_H;

        y = this.drawCategoryRow(context, style, innerR, y, mouseX, mouseY, "Friends", this.friendsSel, false, ROW_H, row++, dt);
        y = this.drawCategoryRow(context, style, innerR, y, mouseX, mouseY, "Profiles", this.profilesSel, false, ROW_H, row++, dt);
        this.drawCategoryRow(context, style, innerR, y, mouseX, mouseY, "Settings", this.themeSel, true, ROW_H + THEME_EXTRA, row, dt);

        context.disableScissor();
    }

    private int drawCategoryRow(
            GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
            int mouseX, int mouseY, String label, ClickGuiSelection sel, boolean lastRow, int rowH,
            int index, float dt
    ) {
        boolean active = !this.searchActive && this.selection.equals(sel);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        float target = active ? 1f : hovered ? 0.85f : 0f;
        float mix = this.rowMix(index, target, dt);

        if (mix > 0.01f) {
            int bg = Motion.mix(0, style.rubyBg(), mix);
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH, bg, lastRow);
        }

        int textY = y + (rowH - style.bodyFont().fontHeight) / 2;
        int textColor = Motion.mix(style.catInactive(), style.ruby(), mix);

        int arrowColor = Motion.mix(style.arrowInactive(), style.ruby(), mix);
        int arrowX = innerR - PAD - Lucide.UI;
        int arrowY = y + (rowH - Lucide.UI) / 2;
        Press.icon(context, "chevron-right", arrowX, arrowY, arrowColor, "nav:" + label);
        Press.label(style.bodyFont(), context, label, PAD, textY, textColor, "nav:" + label);

        return y + rowH;
    }

    private float rowMix(int index, float target, float dt) {
        if (this.rowMix == null || this.rowMix.length <= index) {
            float[] next = new float[index + 1];
            if (this.rowMix != null) System.arraycopy(this.rowMix, 0, next, 0, this.rowMix.length);
            next[index] = target;
            this.rowMix = next;
            return target;
        }
        this.rowMix[index] = Motion.follow(this.rowMix[index], target, GuiTime.toSeconds(dt), Motion.HOVER);
        return this.rowMix[index];
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        if (click.y() < HEADER_H) {
            if (this.onHeaderDrag != null) this.onHeaderDrag.run();
            return true;
        }

        int y = HEADER_H;
        for (int i = 0; i < this.categories.size(); i++) {
            if (click.y() >= y && click.y() < y + ROW_H) {
                Press.down("nav:" + this.categories.get(i).toString());
                this.select(this.categorySels[i]);
                return true;
            }
            y += ROW_H;
        }

        y += 1 + MISC_LABEL_H;
        if (click.y() >= y && click.y() < y + ROW_H) {
            Press.down("nav:Friends");
            this.select(this.friendsSel);
            return true;
        }
        y += ROW_H;
        if (click.y() >= y && click.y() < y + ROW_H) {
            Press.down("nav:Profiles");
            this.select(this.profilesSel);
            return true;
        }
        y += ROW_H;
        if (click.y() >= y && click.y() < y + ROW_H + THEME_EXTRA) {
            Press.down("nav:Settings");
            this.select(this.themeSel);
            return true;
        }

        return false;
    }

    private void select(ClickGuiSelection sel) {
        if (this.selection.equals(sel)) return;
        this.selection = sel;
        this.onSelect.accept(sel);
    }
}
