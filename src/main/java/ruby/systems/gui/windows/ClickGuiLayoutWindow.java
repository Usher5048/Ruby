package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.ClickGuiSelection;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiLayoutWindow extends Window {
    public static final int SIDEBAR_WIDTH = 220;
    public static final int MODULE_PANEL_WIDTH = 230;
    public static final int PANEL_GAP = 2;

    private final CategorySidebarWindow sidebar;
    private final ModulePanelWindow modulePanel;

    public ClickGuiLayoutWindow() {
        super(0, 0, SIDEBAR_WIDTH + PANEL_GAP + MODULE_PANEL_WIDTH, 400);
        this.draggableBounds = new int[] {0, 0, 0, 0};

        int[] pos = ConfigManager.getClickGuiPosition();
        this.setPosition(pos[0], pos[1]);

        List<ModuleType> categories = new ArrayList<>();
        for (ModuleType type : ModuleType.values()) {
            if (!Modules.getByType(type).isEmpty()) {
                categories.add(type);
            }
        }

        ClickGuiSelection initial = categories.isEmpty()
                ? new ClickGuiSelection.Special(ClickGuiSelection.SpecialView.FRIENDS)
                : new ClickGuiSelection.ModuleCategory(categories.getFirst());

        this.modulePanel = new ModulePanelWindow(
                SIDEBAR_WIDTH + PANEL_GAP, 0, initial
        );
        this.sidebar = new CategorySidebarWindow(
                0, 0, categories, initial,
                this.modulePanel::requestSelection,
                () -> this.setDragging(true)
        );

        this.addWindow(this.sidebar);
        this.addWindow(this.modulePanel);
    }

    public CategorySidebarWindow sidebar() {
        return this.sidebar;
    }

    public ModulePanelWindow modulePanel() {
        return this.modulePanel;
    }

    public void prepareForOpen() {
        this.modulePanel.prepareForOpen();
    }

    public void persistPosition() {
        ConfigManager.setClickGuiPosition(this.getX(), this.getY());
    }

    @Override
    public boolean onMouseUp(Click click) {
        if (this.isDragging()) this.persistPosition();
        return false;
    }

    @Override
    public int getHeight() {
        int h = Math.max(this.sidebar.getHeight(), this.modulePanel.getHeight());
        this.height = h;
        return h;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        this.modulePanel.syncSelection(this.sidebar.selection());
    }
}
