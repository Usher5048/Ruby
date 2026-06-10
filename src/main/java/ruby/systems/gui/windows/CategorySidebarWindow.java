package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.ProfileManager;
import ruby.systems.gui.ClickGuiSelection;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.ModuleType;
import ruby.systems.social.FriendsManager;

import java.util.List;
import java.util.function.Consumer;

public class CategorySidebarWindow extends Window {
    public static final int WIDTH = ClickGuiLayoutWindow.SIDEBAR_WIDTH;
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int HEADER_H = 41;
    private static final int ROW_H = 31;
    private static final int MISC_LABEL_H = 19;
    private static final int PANEL_RADIUS = GUIStyle.RADIUS_PANEL;
    private static final int ARROW_W = 5;
    private static final int ARROW_H = 5;

    private final List<ModuleType> categories;
    private ClickGuiSelection selection;
    private final Consumer<ClickGuiSelection> onSelect;
    private final Runnable onHeaderDrag;

    public CategorySidebarWindow(
            int x, int y,
            List<ModuleType> categories,
            ClickGuiSelection initial,
            Consumer<ClickGuiSelection> onSelect,
            Runnable onHeaderDrag
    ) {
        super(x, y, WIDTH, 400);
        this.categories = categories;
        this.selection = initial;
        this.onSelect = onSelect;
        this.onHeaderDrag = onHeaderDrag;
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    public ClickGuiSelection selection() {
        return this.selection;
    }

    @Override
    public int getHeight() {
        int h = HEADER_H + 6 + this.categories.size() * ROW_H + 9 + MISC_LABEL_H + 2 * ROW_H + 6;
        this.height = h;
        return h;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        GUIStyle style = GUIStyle.get();
        int w = WIDTH;
        int h = this.getHeight();
        int innerR = w - INNER_L;

        ModuleTypeWindow.drawRoundedPanel(context, 0, 0, w, h, PANEL_RADIUS, style.bgPanel(), style.borderPanel());

        context.enableScissor(INNER_L, INNER_L, innerR, h - INNER_L);

        context.fill(INNER_L, HEADER_H - 1, innerR, HEADER_H, style.borderSubtle());

        int logoY = (HEADER_H - style.logoFont().fontHeight) / 2;
        style.logoFont().draw(context, "Ruby", PAD, logoY, style.ruby());

        String profile = FriendsManager.capitalizeProfileName(ProfileManager.getActiveProfile());
        int profileW = style.profileFont().getWidth(profile);
        int profileY = (HEADER_H - style.profileFont().fontHeight) / 2;
        style.profileFont().draw(context, profile, w - PAD - profileW, profileY, style.textMuted());

        int y = HEADER_H + 6;

        for (ModuleType type : this.categories) {
            ClickGuiSelection catSel = new ClickGuiSelection.ModuleCategory(type);
            y = this.drawCategoryRow(context, style, innerR, y, mouseX, mouseY, type.toString(), catSel, false);
        }

        y += 4;
        context.fill(INNER_L, y, innerR, y + 1, style.borderSubtle());
        y += 4;

        style.labelFont().draw(context, "MISC", PAD, y + 4, style.textMuted());
        y += MISC_LABEL_H;

        y = this.drawCategoryRow(context, style, innerR, y, mouseX, mouseY, "Friends",
                new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.FRIENDS), false);
        this.drawCategoryRow(context, style, innerR, y, mouseX, mouseY, "Profiles",
                new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.PROFILES), true);

        context.disableScissor();
    }

    private int drawCategoryRow(
            DrawContext context, GUIStyle style, int innerR, int y,
            int mouseX, int mouseY, String label, ClickGuiSelection sel, boolean lastRow
    ) {
        boolean active = this.selection.equals(sel);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + ROW_H;

        if (active || hovered) {
            int bg = style.rubyBg();
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + ROW_H, bg, lastRow);
            if (hovered && !active) {
                ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + ROW_H, 0x08FFFFFF, lastRow);
            }
        }

        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        int textColor = active ? style.ruby() : (hovered ? style.textBright() : style.catInactive());
        style.bodyFont().draw(context, label, PAD, textY, textColor);

        int arrowColor = active ? style.ruby() : style.arrowInactive();
        int arrowX = innerR - PAD - ARROW_W;
        int arrowY = y + (ROW_H - ARROW_H) / 2;
        ModuleTypeWindow.drawChevron(context, arrowX, arrowY, arrowColor, true);

        return y + ROW_H;
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        if (click.y() < HEADER_H) {
            if (this.onHeaderDrag != null) this.onHeaderDrag.run();
            return true;
        }

        int y = HEADER_H + 6;
        for (ModuleType type : this.categories) {
            if (click.y() >= y && click.y() < y + ROW_H) {
                this.select(new ClickGuiSelection.ModuleCategory(type));
                return true;
            }
            y += ROW_H;
        }

        y += 9 + MISC_LABEL_H;
        if (click.y() >= y && click.y() < y + ROW_H) {
            this.select(new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.FRIENDS));
            return true;
        }
        y += ROW_H;
        if (click.y() >= y && click.y() < y + ROW_H) {
            this.select(new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.PROFILES));
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
