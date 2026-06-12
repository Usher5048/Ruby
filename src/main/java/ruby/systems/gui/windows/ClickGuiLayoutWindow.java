package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
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

    private static final int SEARCH_MAX_LEN = 48;

    private final CategorySidebarWindow sidebar;
    private final ModulePanelWindow modulePanel;

    private String searchQuery = "";
    private ClickGuiSelection preSearchSelection;

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
                this::onSidebarSelect,
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
        this.searchQuery = "";
        this.preSearchSelection = null;
        this.sidebar.setSearchActive(false);
        if (this.modulePanel.selection() instanceof ClickGuiSelection.Search) {
            this.modulePanel.setSelectionImmediate(this.sidebar.selection());
        }
        this.modulePanel.prepareForOpen();
    }

    public boolean isSearching() {
        return !this.searchQuery.isEmpty();
    }

    public String searchQuery() {
        return this.searchQuery;
    }

    public boolean handleSearchChar(CharInput input) {
        char c = (char) input.codepoint();
        if (c < 32 || c >= 127) return false;
        if (this.searchQuery.length() >= SEARCH_MAX_LEN) return true;

        if (this.searchQuery.isEmpty()) {
            this.preSearchSelection = this.sidebar.selection();
            this.sidebar.setSearchActive(true);
        }

        this.searchQuery += c;
        this.applySearch();
        return true;
    }

    public boolean handleSearchKey(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.searchQuery.isEmpty()) return false;
            this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
            if (this.searchQuery.isEmpty()) {
                this.clearSearch(true);
            } else {
                this.applySearch();
            }
            return true;
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE && !this.searchQuery.isEmpty()) {
            this.clearSearch(true);
            return true;
        }

        return false;
    }

    private void onSidebarSelect(ClickGuiSelection sel) {
        this.clearSearch(false);
        this.modulePanel.requestSelection(sel);
    }

    private void applySearch() {
        this.modulePanel.updateSearch(this.searchQuery);
    }

    private void clearSearch(boolean restoreSelection) {
        this.searchQuery = "";
        this.sidebar.setSearchActive(false);

        if (restoreSelection && this.preSearchSelection != null) {
            this.sidebar.setSelection(this.preSearchSelection);
            this.modulePanel.setSelectionImmediate(this.preSearchSelection);
        }

        this.preSearchSelection = null;
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
        if (!this.isSearching()) {
            this.modulePanel.syncSelection(this.sidebar.selection());
        }
    }
}
