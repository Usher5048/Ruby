package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorType;
import org.lwjgl.sdl.SDLMouse;
import ruby.RubyClient;
import ruby.systems.gui.ClickGuiSearch;
import ruby.systems.gui.ClickGuiSelection;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.Press;
import ruby.systems.gui.icons.Lucide;
import ruby.systems.gui.Motion;
import ruby.systems.gui.GuiTime;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public class ModulePanelWindow extends Window {
    public static final int WIDTH = ClickGuiLayoutWindow.MODULE_PANEL_WIDTH;
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int HEADER_H = 34;
    private static final int PANEL_RADIUS = GUIStyle.RADIUS_PANEL;
    private static final float CAT_PAUSE_SEC = 0.04f;
    private static final int BLUR_H = 22;
    /** Overflow this small grows the panel instead of showing a scrollbar. */
    private static final float SCROLLBAR_MIN = 10f;
    private static final int HANDLE = 12;

    private enum SwitchPhase { IDLE, FADE_OUT, PAUSE, FADE_IN }

    private ClickGuiSelection selection;
    private ClickGuiSelection displayedSelection;
    private ClickGuiSelection pendingSelection;

    private SwitchPhase switchPhase = SwitchPhase.IDLE;
    private float switchTimer = 0f;
    private float itemStaggerTimer = 0f;
    private boolean itemStaggerActive = false;
    private float panelAlpha = 1f;
    private float panelOffsetY = 0f;
    private float panelScale = 1f;

    private boolean panelCollapsed = false;
    private float collapseProgress = 1f;
    private float collapseArrowRotation = 90f;

    private final Map<ModuleType, List<ModuleWindow>> moduleWindows = new EnumMap<>(ModuleType.class);
    private final FriendsPanelContent friendsPanel;
    private final ProfilesPanelContent profilesPanel;
    private final ThemePanelContent themePanel;

    private int bodyHeight = 0;
    private int maxPanelHeight = Integer.MAX_VALUE;
    /** Sidebar height. The panel cap never drops below this. */
    private int floorPanelHeight = HEADER_H;
    /** Largest height the window will allow without pushing the handle off screen. */
    private int screenHeightLimit = Integer.MAX_VALUE;
    /** Negative until a drag or a profile stores a height. */
    private int chosenMaxHeight = -1;
    private int appliedSavedHeight = Integer.MIN_VALUE;
    private float scroll = 0f;
    private float scrollTarget = 0f;
    private int titleSeparatorY = HEADER_H - 1;
    private boolean scrollbarDrag = false;
    private float dragGrab = 0f;
    private String cachedSearchQuery;
    private List<ModuleWindow> cachedSearchResults = List.of();
    private boolean bodyCached;
    private int bodyStamp;
    private boolean heightDrag = false;
    private double heightDragOriginY = 0;
    private int heightDragOriginMax = 0;

    private static CursorType resizeNwse;

    private static CursorType resizeCursor() {
        CursorType cursor = resizeNwse;
        if (cursor != null) return cursor;
        cursor = CursorType.createStandardCursor(
                SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE,
                "resize_nwse",
                CursorType.DEFAULT
        );
        resizeNwse = cursor;
        return cursor;
    }

    public ModulePanelWindow(int x, int y, ClickGuiSelection initial) {
        super(x, y, WIDTH, 400);
        this.selection = initial;
        this.displayedSelection = initial;
        this.draggableBounds = new int[] {0, 0, 0, 0};

        for (ModuleType type : ModuleType.values()) {
            List<Module> modules = Modules.getByType(type);
            if (modules.isEmpty()) continue;

            List<ModuleWindow> windows = new ArrayList<>();
            for (Module module : modules) {
                ModuleWindow win = new ModuleWindow(0, 0, module);
                win.setAccordionHost(this::onModuleExpandRequest);
                windows.add(win);
            }
            this.moduleWindows.put(type, windows);
        }

        this.friendsPanel = new FriendsPanelContent(0, HEADER_H, WIDTH);
        this.profilesPanel = new ProfilesPanelContent(0, HEADER_H, WIDTH);
        this.themePanel = new ThemePanelContent(0, HEADER_H, WIDTH);
        this.rebuildChildren();
    }

    /**
     * Sidebar height is the floor. A taller height chosen by dragging the corner handle is kept.
     * {@code screenLimit} is the tallest cap that still leaves the handle on screen.
     */
    public void setPanelHeightFloor(int floor, int screenLimit) {
        this.floorPanelHeight = Math.max(HEADER_H, floor);
        this.screenHeightLimit = Math.max(this.floorPanelHeight, screenLimit);
        int saved = ruby.systems.config.ConfigManager.panelHeight();
        if (saved != this.appliedSavedHeight) {
            this.appliedSavedHeight = saved;
            if (saved >= 0) this.chosenMaxHeight = saved;
        }
        int chosen = this.chosenMaxHeight < 0
                ? this.floorPanelHeight
                : Math.max(this.floorPanelHeight, this.chosenMaxHeight);
        this.maxPanelHeight = Math.min(chosen, this.screenHeightLimit);
    }

    public boolean isScrollbarDragging() {
        return this.scrollbarDrag;
    }

    public void releaseScrollbar() {
        this.scrollbarDrag = false;
    }

    public boolean isHeightDragging() {
        return this.heightDrag;
    }

    public void releaseHeightDrag() {
        this.heightDrag = false;
    }

    public void dragHeight(double localY) {
        if (!this.heightDrag) return;
        if (!this.isModuleList()) {
            this.heightDrag = false;
            return;
        }
        int next = this.heightDragOriginMax + (int) Math.round(localY - this.heightDragOriginY);
        next = Math.max(this.floorPanelHeight, next);
        next = Math.min(next, this.screenHeightLimit);
        this.chosenMaxHeight = next;
        this.maxPanelHeight = next;
        this.appliedSavedHeight = next;
        ruby.systems.config.ConfigManager.setPanelHeight(next);
    }

    public ClickGuiSelection selection() {
        return this.selection;
    }

    public void setSelectionImmediate(ClickGuiSelection sel) {
        this.selection = sel;
        this.displayedSelection = sel;
        this.pendingSelection = null;
        this.switchPhase = SwitchPhase.IDLE;
        this.switchTimer = 0f;
        this.itemStaggerActive = false;
        this.panelAlpha = 1f;
        this.panelOffsetY = 0f;
        this.panelScale = 1f;
        this.rebuildChildren();
    }

    public void updateSearch(String query) {
        ClickGuiSelection.Search sel = new ClickGuiSelection.Search(query);
        this.selection = sel;
        this.displayedSelection = sel;
        this.pendingSelection = null;
        this.switchPhase = SwitchPhase.IDLE;
        this.switchTimer = 0f;
        this.itemStaggerTimer = 0f;
        this.itemStaggerActive = false;
        this.panelAlpha = 1f;
        this.panelOffsetY = 0f;
        this.panelScale = 1f;
        this.rebuildChildren();
    }

    public void requestSelection(ClickGuiSelection sel) {
        if (this.selection.equals(sel)) return;

        if (sel instanceof ClickGuiSelection.Search(String query)) {
            this.updateSearch(query);
            return;
        }

        this.selection = sel;
        this.pendingSelection = sel;
        switch (this.switchPhase) {
            case IDLE, FADE_IN -> {
                this.switchPhase = SwitchPhase.FADE_OUT;
                this.switchTimer = 0f;
                this.itemStaggerActive = false;
            }
            case FADE_OUT, PAUSE -> {}
        }
    }

    public void syncSelection(ClickGuiSelection sidebarSelection) {
        if (!this.selection.equals(sidebarSelection)) {
            this.requestSelection(sidebarSelection);
        }
    }

    public void prepareForOpen() {
        this.switchPhase = SwitchPhase.IDLE;
        this.switchTimer = 0f;
        this.itemStaggerTimer = 0f;
        this.itemStaggerActive = false;
        this.panelAlpha = 1f;
        this.panelOffsetY = 0f;
        this.panelScale = 1f;
        this.pendingSelection = null;
        this.displayedSelection = this.selection;
        this.rebuildChildren();
    }

    private void onModuleExpandRequest(ModuleWindow source) {
        if (!source.expanded) return;

        List<ModuleWindow> scope;
        if (this.displayedSelection instanceof ClickGuiSelection.Search search) {
            scope = this.searchResults(search.query());
        } else if (this.displayedSelection instanceof ClickGuiSelection.ModuleCategory cat) {
            scope = this.moduleWindows.get(cat.type());
            if (scope == null) return;
        } else {
            return;
        }

        for (ModuleWindow win : scope) {
            if (win != source && win.expanded) {
                win.expanded = false;
            }
        }
    }

    private List<ModuleWindow> searchResults(String query) {
        if (query.equals(this.cachedSearchQuery)) return this.cachedSearchResults;
        List<ModuleWindow> results = new ArrayList<>();
        for (List<ModuleWindow> list : this.moduleWindows.values()) {
            for (int i = 0, n = list.size(); i < n; i++) {
                ModuleWindow win = list.get(i);
                if (ClickGuiSearch.matches(win.module(), query)) results.add(win);
            }
        }
        this.cachedSearchQuery = query;
        this.cachedSearchResults = results;
        return results;
    }

    private List<ModuleWindow> displayedModules() {
        if (this.displayedSelection instanceof ClickGuiSelection.ModuleCategory cat) {
            return this.moduleWindows.get(cat.type());
        }
        if (this.displayedSelection instanceof ClickGuiSelection.Search search) {
            return this.searchResults(search.query());
        }
        return null;
    }

    private Window specialPanel() {
        if (!(this.displayedSelection instanceof ClickGuiSelection.Special special)) {
            return this.friendsPanel;
        }
        return switch (special.view()) {
            case FRIENDS -> this.friendsPanel;
            case PROFILES -> this.profilesPanel;
            case THEME -> this.themePanel;
        };
    }

    private void rebuildChildren() {
        this.scroll = 0f;
        this.scrollTarget = 0f;
        this.scrollbarDrag = false;
        this.windows().clear();
        if (this.displayedSelection instanceof ClickGuiSelection.ModuleCategory cat) {
            List<ModuleWindow> list = this.moduleWindows.get(cat.type());
            if (list != null) {
                for (ModuleWindow win : list) {
                    this.addWindow(win);
                }
            }
        } else if (this.displayedSelection instanceof ClickGuiSelection.Search search) {
            for (ModuleWindow win : this.searchResults(search.query())) {
                this.addWindow(win);
            }
        } else if (this.displayedSelection instanceof ClickGuiSelection.Special special) {
            Window panel = switch (special.view()) {
                case FRIENDS -> this.friendsPanel;
                case PROFILES -> this.profilesPanel;
                case THEME -> this.themePanel;
            };
            this.addWindow(panel);
            if (special.view() == ClickGuiSelection.SpecialView.PROFILES) this.profilesPanel.refreshClipboard();
        }
    }

    private String headerTitle() {
        return switch (this.displayedSelection) {
            case ClickGuiSelection.ModuleCategory cat -> cat.type().toString();
            case ClickGuiSelection.Search search -> search.query();
            case ClickGuiSelection.Special special -> switch (special.view()) {
                case FRIENDS -> "Friends";
                case PROFILES -> "Profiles";
                case THEME -> "Settings";
            };
        };
    }

    @Override
    public int getHeight() {
        this.bodyHeight = this.computeBodyHeight();
        return this.visibleHeight();
    }

    /** Header plus the rows that exist now. Empty space under a collapsed list is excluded. */
    public int solidHeight() {
        int visible = this.getHeight();
        int content = HEADER_H + Math.max(0, this.bodyHeight);
        return Math.min(visible, content);
    }

    private int visibleHeight() {
        int full = HEADER_H + (int) (this.bodyHeight * Motion.standard(this.collapseProgress));
        int cap = this.maxPanelHeight < HEADER_H ? Integer.MAX_VALUE : this.maxPanelHeight;
        int capped = Math.min(Math.max(full, HEADER_H), cap);
        int overflow = full - capped;
        if (overflow > 0 && overflow <= SCROLLBAR_MIN) {
            capped = Math.min(full, this.screenHeightLimit);
        }
        this.height = capped;
        return capped;
    }

    private int computeBodyHeight() {
        if (this.displayedSelection instanceof ClickGuiSelection.Special special) {
            this.bodyCached = false;
            return switch (special.view()) {
                case FRIENDS -> this.friendsPanel.getHeight() + 2;
                case PROFILES -> this.profilesPanel.getHeight() + 2;
                case THEME -> this.themePanel.getHeight();
            };
        }
        int stamp = this.moduleBodyStamp();
        if (this.bodyCached && stamp == this.bodyStamp) return this.bodyHeight;
        this.bodyStamp = stamp;
        this.bodyCached = true;
        this.bodyHeight = this.measureModuleBody();
        return this.bodyHeight;
    }

    private int moduleBodyStamp() {
        int stamp = this.displayedSelection.hashCode();
        List<ModuleWindow> list = this.displayedModules();
        if (list == null) return stamp;
        int last = list.size() - 1;
        for (int i = 0; i < list.size(); i++) {
            ModuleWindow win = list.get(i);
            win.setLastInList(i == last);
            stamp = stamp * 31 + win.layoutStamp();
        }
        return stamp;
    }

    private int measureModuleBody() {
        List<ModuleWindow> list = this.displayedModules();
        if (list == null || list.isEmpty()) return 0;
        int h = 0;
        for (int i = 0, n = list.size(); i < n; i++) {
            h += list.get(i).getHeight();
        }
        return h;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        boolean animating = this.switchPhase != SwitchPhase.IDLE
                || Math.abs(this.panelOffsetY) > 0.01f
                || Math.abs(this.panelScale - 1f) > 0.001f
                || this.panelAlpha < 0.999f;

        if (!animating) {
            super.extractRenderState(context, mouseX, mouseY, dt);
            return;
        }

        context.pose().pushMatrix();
        context.pose().translate(0, this.panelOffsetY);
        float cx = WIDTH / 2f;
        float cy = HEADER_H / 2f;
        context.pose().translate(cx, cy);
        context.pose().scale(this.panelScale, this.panelScale);
        context.pose().translate(-cx, -cy);
        super.extractRenderState(context, mouseX, mouseY, dt);
        context.pose().popMatrix();
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        float deltaSec = GuiTime.toSeconds(dt);
        float collapseDir = this.panelCollapsed ? -1f : 1f;
        this.collapseProgress = Math.clamp(this.collapseProgress + collapseDir * deltaSec / Motion.EXPAND, 0f, 1f);

        this.updateSwitchAnimation(deltaSec);

        GUIStyle style = GUIStyle.get();
        int w = WIDTH;
        int innerR = w - INNER_L;
        this.bodyHeight = this.computeBodyHeight();
        int h = this.visibleHeight();
        this.tickScroll(deltaSec, h);
        boolean roundBottom = this.collapseProgress > 0.98f && !this.panelCollapsed;

        int panelBg = GUIStyle.withAlpha(style.bgPanel(), this.panelAlpha);
        int panelBorder = GUIStyle.withAlpha(style.borderPanel(), this.panelAlpha);
        ModuleTypeWindow.drawRoundedPanel(context, 0, 0, w, Math.max(h, HEADER_H), PANEL_RADIUS, panelBg, panelBorder, roundBottom);

        context.enableScissor(INNER_L, INNER_L, innerR, Math.max(h, HEADER_H) - INNER_L);

        boolean showBody = this.collapseProgress > 0.01f && this.panelAlpha > 0.01f
                && this.switchPhase != SwitchPhase.PAUSE;
        List<ModuleWindow> modules = showBody ? this.displayedModules() : null;
        if (modules != null) {
            this.layoutModuleList(modules, HEADER_H - Math.round(this.scroll));
        }

        int sepColor = GUIStyle.withAlpha(style.borderSubtle(), this.panelAlpha);
        int separatorY = modules != null ? this.titleSeparatorY : HEADER_H - 1;
        context.fill(INNER_L, separatorY, innerR, separatorY + 1, sepColor);

        int titleY = (HEADER_H - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, this.headerTitle(), PAD, titleY,
                GUIStyle.withAlpha(style.textBright(), this.panelAlpha));

        float arrowTarget = this.panelCollapsed ? -90f : 90f;
        this.collapseArrowRotation = Motion.follow(this.collapseArrowRotation, arrowTarget, deltaSec, Motion.FAST);

        boolean collapseHovered = mouseX >= w - 30 && mouseX < w - 6 && mouseY >= 6 && mouseY < HEADER_H - 6;
        int collapseColor = GUIStyle.withAlpha(collapseHovered ? 0xFF7A7577 : style.textMuted(), this.panelAlpha);
        float collapsePress = Press.sample("panel-collapse");
        int collapseTint = Motion.mix(collapseColor, Motion.mix(collapseColor, 0xFFFFFFFF, 0.55f), collapsePress);
        Lucide.drawCentered(context, "chevron-right", w - PAD - 2, HEADER_H / 2, Lucide.UI, collapseTint, this.collapseArrowRotation, collapsePress);

        if (showBody) {
            if (modules == null) this.layoutBodyChildren();
            this.enableCutout(INNER_L, HEADER_H, innerR, h - INNER_L);
            this.handleChildren = true;
        } else {
            this.handleChildren = false;
        }

        context.disableScissor();
    }

    private void layoutBodyChildren() {
        int y = HEADER_H - Math.round(this.scroll);
        List<ModuleWindow> modules = this.displayedModules();
        if (modules != null) {
            this.layoutModuleList(modules, y);
            return;
        }
        Window panel = this.specialPanel();
        float eased = this.staggerEased(0);
        panel.setPosition(0, HEADER_H - Math.round(this.scroll) + Math.round(Motion.RISE * (1f - eased)));
        float staggerAlpha = eased;
        if (panel instanceof FriendsPanelContent f) {
            f.setContentAlpha(this.panelAlpha * staggerAlpha);
        }
        if (panel instanceof ProfilesPanelContent p) {
            p.setContentAlpha(this.panelAlpha * staggerAlpha);
        }
        if (panel instanceof ThemePanelContent t) {
            t.setContentAlpha(this.panelAlpha * staggerAlpha);
        }
    }

    private boolean isStaggering() {
        return this.switchPhase == SwitchPhase.FADE_IN || this.itemStaggerActive;
    }

    private float staggerTime() {
        return this.switchPhase == SwitchPhase.FADE_IN ? this.switchTimer : this.itemStaggerTimer;
    }

    private float staggerEased(int index) {
        if (!this.isStaggering()) return 1f;
        float t = (this.staggerTime() - index * Motion.STAGGER) / Motion.ENTER;
        return Motion.out(t);
    }

    private void layoutModuleList(List<ModuleWindow> list, int y) {
        this.titleSeparatorY = HEADER_H - 1;
        if (list == null || list.isEmpty()) return;

        int total = list.size();
        int viewportTop = HEADER_H;
        // Child rows are scissored in GUI pixels (cut / scale), then the pose scale
        // maps that exclusive edge back to physical pixels. A raw panel-height test
        // counts a following row that only crosses that edge by a pixel, so the
        // module above it still draws its rule inside the viewport.
        int scale = Math.max(1, RubyClient.client.getWindow().getGuiScale());
        int cutBottom = this.height - INNER_L;
        int viewportBottom = (cutBottom / scale) * scale;
        int[] tops = new int[total];
        int[] heights = new int[total];
        int cursor = y;
        for (int i = 0; i < total; i++) {
            tops[i] = cursor + Math.round(Motion.RISE * (1f - this.staggerEased(i)));
            heights[i] = list.get(i).getHeight();
            cursor += heights[i];
        }

        int topIdx = -1;
        int bottomIdx = -1;
        for (int i = 0; i < total; i++) {
            int bottom = tops[i] + heights[i];
            if (bottom > viewportTop && tops[i] < viewportBottom) {
                if (topIdx < 0) topIdx = i;
            }
            if (visibleSpan(tops[i], bottom, viewportTop, viewportBottom) > 1) {
                bottomIdx = i;
            }
        }

        if (topIdx >= 0) {
            int edge = tops[topIdx];
            this.titleSeparatorY = edge > viewportTop ? edge - 1 : viewportTop - 1;
            if (topIdx < total - 1) {
                int ruleY = edge + list.get(topIdx).getHeaderHeight() - 1;
                if (ruleY >= viewportTop && ruleY <= this.titleSeparatorY + 1) {
                    this.titleSeparatorY = ruleY;
                }
            }
        }

        for (int i = 0; i < total; i++) {
            ModuleWindow win = list.get(i);
            win.setPosition(0, tops[i]);
            boolean last = i == total - 1;
            win.setLastInList(last);
            win.setBodyClip(this.height - tops[i]);
            win.setContentAlpha(this.panelAlpha * this.staggerEased(i));
            int headerH = win.getHeaderHeight();
            int ruleY = tops[i] + headerH - 1;
            boolean lineInView = ruleY >= viewportTop && ruleY < viewportBottom;
            int bodySpan = visibleSpan(tops[i] + headerH, tops[i] + heights[i], viewportTop, viewportBottom);
            // Keep the rule only when another row, or this row's settings, is actually
            // visible beneath it. The bottom visible module's own edge stays clear.
            boolean contentBelow = bodySpan > 1 || (bottomIdx >= 0 && bottomIdx > i);
            win.setDrawBottomSeparator(!last && i != bottomIdx && lineInView && contentBelow && ruleY > this.titleSeparatorY);
        }
    }

    /** Pixels of [top, bottom) inside the scissored list body. */
    private static int visibleSpan(int top, int bottom, int viewportTop, int viewportBottom) {
        return Math.max(0, Math.min(bottom, viewportBottom) - Math.max(top, viewportTop));
    }

    private int maxStaggerItems() {
        List<ModuleWindow> list = this.displayedModules();
        return list == null ? 1 : list.size();
    }

    private void updateSwitchAnimation(float deltaSec) {
        if (this.itemStaggerActive) {
            this.itemStaggerTimer += deltaSec;
            float maxDelay = Math.max(0, this.maxStaggerItems() - 1) * Motion.STAGGER + Motion.ENTER;
            if (this.itemStaggerTimer >= maxDelay) {
                this.itemStaggerActive = false;
            }
        }

        if (this.switchPhase == SwitchPhase.IDLE) {
            this.panelAlpha = 1f;
            this.panelOffsetY = 0f;
            this.panelScale = 1f;
            return;
        }

        this.switchTimer += deltaSec;

        switch (this.switchPhase) {
            case FADE_OUT -> {
                float t = Math.min(1f, this.switchTimer / Motion.EXIT);
                float eased = Motion.out(t);
                this.panelAlpha = 1f - eased;
                this.panelOffsetY = Motion.RISE * eased;
                this.panelScale = 1f - (1f - Motion.SCALE_FROM) * eased;
                if (t >= 1f) {
                    this.switchPhase = SwitchPhase.PAUSE;
                    this.switchTimer = 0f;
                    this.panelAlpha = 0f;
                }
            }
            case PAUSE -> {
                this.panelAlpha = 0f;
                if (this.switchTimer >= CAT_PAUSE_SEC) {
                    this.displayedSelection = this.pendingSelection;
                    this.rebuildChildren();
                    this.switchPhase = SwitchPhase.FADE_IN;
                    this.switchTimer = 0f;
                    this.itemStaggerTimer = 0f;
                    this.itemStaggerActive = true;
                    this.panelOffsetY = -Motion.RISE;
                    this.panelScale = Motion.SCALE_FROM;
                }
            }
            case FADE_IN -> {
                float panelT = Math.min(1f, this.switchTimer / Motion.ENTER);
                float panelEased = Motion.out(panelT);
                this.panelAlpha = panelEased;
                this.panelOffsetY = -Motion.RISE * (1f - panelEased);
                this.panelScale = Motion.SCALE_FROM + (1f - Motion.SCALE_FROM) * panelEased;
                if (panelT >= 1f) {
                    this.panelAlpha = 1f;
                    this.panelOffsetY = 0f;
                    this.panelScale = 1f;
                    if (!this.selection.equals(this.displayedSelection)) {
                        this.pendingSelection = this.selection;
                        this.switchPhase = SwitchPhase.FADE_OUT;
                        this.switchTimer = 0f;
                        this.itemStaggerActive = false;
                    } else {
                        this.switchPhase = SwitchPhase.IDLE;
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public void onRenderForeground(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        if (this.panelAlpha < 0.02f || this.collapseProgress < 0.85f) return;

        if (this.displayedSelection instanceof ClickGuiSelection.Special special
                && special.view() == ClickGuiSelection.SpecialView.PROFILES) {
            if (ruby.systems.gui.ClientSettings.get().hints()) this.profilesPanel.drawHint(context);
        }

        Scrollbar bar = this.scrollbarGeometry();
        if (this.isModuleList() && bar != null) this.drawBottomBlur(context);

        if (bar != null) {
            boolean hot = this.scrollbarDrag
                    || (mouseX >= bar.x1() - 4 && mouseX <= bar.x2() + 3
                    && mouseY >= bar.thumbY() && mouseY < bar.thumbY() + bar.thumbH());
            int color = GUIStyle.withAlpha(this.thumbColor(hot), this.panelAlpha);
            ModuleTypeWindow.fillSmoothRoundedRect(context, bar.x1(), bar.thumbY(), bar.x2(), bar.thumbY() + bar.thumbH(), 1, color);
        }

        if (!this.handleShown(bar)) return;
        this.drawCornerHandle(context);
        if (this.heightDrag || this.hitHandle(mouseX, mouseY)) {
            context.requestCursor(resizeCursor());
        }
    }

    private boolean handleShown(Scrollbar bar) {
        if (!this.isModuleList()) return false;
        if (this.panelAlpha < 0.02f || this.collapseProgress < 0.85f) return false;
        if (this.height - 5 - HANDLE < HEADER_H) return false;
        return this.heightDrag || bar != null || this.chosenMaxHeight > this.floorPanelHeight;
    }

    @Override
    protected boolean blockChildHover(int localX, int localY) {
        Scrollbar bar = this.scrollbarGeometry();
        if (this.handleShown(bar) && this.hitHandle(localX, localY)) return true;
        if (bar == null) return false;
        return this.scrollbarDrag
                || (localX >= bar.x1() - 4 && localX <= bar.x2() + 3
                && localY >= bar.trackTop() && localY <= bar.trackBottom());
    }

    private boolean hitHandle(double localX, double localY) {
        if (this.height - 5 - HANDLE < HEADER_H) return false;
        int x = WIDTH - 8 - HANDLE;
        int y = this.height - 5 - HANDLE;
        return localX >= x && localX < x + HANDLE && localY >= y && localY < y + HANDLE;
    }

    private boolean isModuleList() {
        return this.displayedSelection instanceof ClickGuiSelection.ModuleCategory
                || this.displayedSelection instanceof ClickGuiSelection.Search;
    }

    /** Soft veil at the clipped bottom edge. The game blur pass is full-screen, so this stays local. */
    private void drawBottomBlur(GuiGraphicsExtractor context) {
        int h = this.height;
        int top = Math.max(HEADER_H, h - BLUR_H);
        int bottom = h - INNER_L;
        if (bottom - top < 4) return;

        int rgb = GUIStyle.get().bgPanel() & 0x00FFFFFF;
        int x1 = INNER_L;
        int x2 = WIDTH - INNER_L;
        int radius = Math.max(0, PANEL_RADIUS - 1);
        int span = bottom - top;

        for (int y = top; y < bottom; y++) {
            float t = (y - top + 1) / (float) span;
            t = t * t * (3f - 2f * t);
            int alpha = (int) (t * 170f * this.panelAlpha);
            if (alpha <= 0) continue;

            int inset = 0;
            int fromBottom = bottom - 1 - y;
            if (fromBottom < radius) {
                inset = ModuleTypeWindow.cornerInset(radius, fromBottom);
            }
            context.fill(x1 + inset, y, x2 - inset, y + 1, (alpha << 24) | rgb);
        }
    }

    private void drawCornerHandle(GuiGraphicsExtractor context) {
        int x = WIDTH - 8 - HANDLE;
        int y = this.height - 5 - HANDLE;
        if (y < HEADER_H) return;
        int color = GUIStyle.withAlpha(GUIStyle.get().textMuted(), this.panelAlpha);
        Lucide.draw(context, "handle-bottom-right", x, y, HANDLE, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int scale = RubyClient.client.getWindow().getGuiScale();
        double localX = Math.round(click.x() * scale) - this.getX();
        double localY = Math.round(click.y() * scale) - this.getY();
        if (this.tryHandlePress(localX, localY, click.button())) return true;
        if (this.tryScrollbarPress(localX, localY, click.button())) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT
                && (this.heightDrag || this.scrollbarDrag)) {
            int scale = RubyClient.client.getWindow().getGuiScale();
            double localY = Math.round(click.y() * scale) - this.getY();
            if (this.heightDrag) this.dragHeight(localY);
            else this.dragScrollbar(localY);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (this.heightDrag || this.scrollbarDrag) {
            boolean scrollbar = this.scrollbarDrag;
            this.heightDrag = false;
            this.scrollbarDrag = false;
            if (scrollbar && this.isModuleList()) this.scrollTarget = snapScroll(this.scroll, this.maxScroll());
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.heightDrag) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean onMouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (!this.scrollbarDrag) return false;
        this.dragScrollbar(click.y());
        return true;
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        if (!this.scrollbarDrag) return false;
        this.scrollbarDrag = false;
        if (this.isModuleList()) this.scrollTarget = snapScroll(this.scroll, this.maxScroll());
        return true;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.heightDrag) return true;
        if (this.scrollbarGeometry() == null) return false;
        if (!this.isModuleList()) {
            this.scrollTarget -= (float) vertical * 32f;
            return true;
        }
        int steps = Math.max(1, Math.round(Math.abs((float) vertical)));
        float dir = vertical > 0d ? -1f : 1f;
        float target = this.scrollTarget;
        float max = this.maxScroll();
        for (int i = 0; i < steps; i++) {
            target = snapScroll(target + dir * SCROLL_STEP, max);
        }
        this.scrollTarget = target;
        return true;
    }

    private boolean tryHandlePress(double localX, double localY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        Scrollbar bar = this.scrollbarGeometry();
        if (!this.handleShown(bar) || !this.hitHandle(localX, localY)) return false;
        this.heightDrag = true;
        this.heightDragOriginY = localY;
        this.heightDragOriginMax = this.maxPanelHeight;
        return true;
    }

    @Override
    protected boolean isChildInteractive(Window child, double localX, double localY) {
        return localY >= HEADER_H && localY < this.height;
    }

    public void dragScrollbar(double localY) {
        Scrollbar bar = this.scrollbarGeometry();
        if (bar == null) {
            this.scrollbarDrag = false;
            return;
        }
        float travel = bar.trackH() - bar.thumbH();
        float thumbTop = (float) localY - this.dragGrab;
        float frac = travel <= 0f ? 0f : (thumbTop - bar.trackTop()) / travel;
        this.scrollTarget = Math.clamp(frac, 0f, 1f) * bar.maxScroll();
        this.scroll = this.scrollTarget;
    }

    private boolean tryScrollbarPress(double localX, double localY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        Scrollbar bar = this.scrollbarGeometry();
        if (bar == null) return false;
        if (localX < bar.x1() - 4 || localX > bar.x2() + 3) return false;
        if (localY < bar.trackTop() || localY > bar.trackBottom()) return false;

        this.scrollbarDrag = true;
        if (localY >= bar.thumbY() && localY < bar.thumbY() + bar.thumbH()) {
            this.dragGrab = (float) (localY - bar.thumbY());
        } else {
            this.dragGrab = bar.thumbH() / 2f;
            this.dragScrollbar(localY);
        }
        return true;
    }

    private static final int SCROLL_STEP = 31;

    private float maxScroll() {
        return Math.max(0f, this.bodyHeight - Math.max(0, this.visibleHeight() - HEADER_H));
    }

    /** Nearest row boundary. The leftover tail of the list is its own stop. */
    private static float snapScroll(float value, float max) {
        if (max <= 0f) return 0f;
        value = Math.clamp(value, 0f, max);
        float lastFull = (float) (Math.floor(max / SCROLL_STEP) * SCROLL_STEP);
        float tail = max - lastFull;
        if (tail > 0.5f && value > lastFull && value >= lastFull + tail * 0.5f) return max;
        float snapped = Math.round(value / SCROLL_STEP) * (float) SCROLL_STEP;
        if (snapped > lastFull) snapped = max;
        return Math.clamp(snapped, 0f, max);
    }

    private void tickScroll(float deltaSec, int visibleH) {
        float max = Math.max(0f, this.bodyHeight - Math.max(0, visibleH - HEADER_H));
        this.scrollTarget = Math.clamp(this.scrollTarget, 0f, max);
        if (!this.scrollbarDrag) {
            this.scroll = Motion.follow(this.scroll, this.scrollTarget, deltaSec, Motion.SCROLL);
        }
        this.scroll = Math.clamp(this.scroll, 0f, max);
    }

    private int thumbColor(boolean hot) {
        int color = GUIStyle.get().scrollbar();
        if ((color >>> 24) == 0) color = 0xFF4A4648;
        if (!hot) return color;
        int a = (color >>> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 48);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 48);
        int b = Math.min(255, (color & 0xFF) + 48);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private Scrollbar scrollbarGeometry() {
        int visibleBody = Math.max(0, this.height - HEADER_H);
        if (visibleBody <= 8 || this.collapseProgress < 0.85f) return null;
        float maxScroll = this.bodyHeight - visibleBody;
        if (maxScroll <= SCROLLBAR_MIN) return null;

        int trackTop = HEADER_H + 8;
        int trackBottom = this.height - 10;
        int trackH = trackBottom - trackTop;
        if (trackH < 16) return null;

        int thumbH = Math.max(16, Math.round(trackH * (visibleBody / (float) this.bodyHeight)));
        thumbH = Math.min(thumbH, trackH);
        int thumbY = trackTop + Math.round((this.scroll / maxScroll) * (trackH - thumbH));
        return new Scrollbar(WIDTH - 8, WIDTH - 5, trackTop, trackBottom, thumbY, thumbH, maxScroll);
    }

    private record Scrollbar(int x1, int x2, int trackTop, int trackBottom, int thumbY, int thumbH, float maxScroll) {
        int trackH() {
            return this.trackBottom - this.trackTop;
        }
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (click.y() >= 6 && click.y() < HEADER_H - 6 && click.x() >= WIDTH - 30) {
            Press.down("panel-collapse");
            this.panelCollapsed = !this.panelCollapsed;
            return true;
        }
        if (this.switchPhase != SwitchPhase.IDLE) return true;
        return false;
    }
}
