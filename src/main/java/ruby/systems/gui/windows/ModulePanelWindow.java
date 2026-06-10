package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ruby.systems.gui.ClickGuiSelection;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiEasing;
import ruby.systems.gui.GuiTime;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Modules;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModulePanelWindow extends Window {
    public static final int WIDTH = ClickGuiLayoutWindow.MODULE_PANEL_WIDTH;
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int HEADER_H = 34;
    private static final int PANEL_RADIUS = GUIStyle.RADIUS_PANEL;
    private static final float CAT_OUT_SEC = 0.24f;
    private static final float CAT_PAUSE_SEC = 0.08f;
    private static final float CAT_IN_SEC = 0.24f;
    private static final float COLLAPSE_SEC = 0.2f;
    private static final float ITEM_IN_SEC = 0.32f;
    private static final float ITEM_STAGGER_SEC = 0.032f;

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

    private int bodyHeight = 0;

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
        this.rebuildChildren();
    }

    public void requestSelection(ClickGuiSelection sel) {
        if (this.selection.equals(sel)) return;
        this.selection = sel;
        this.pendingSelection = sel;
        if (this.switchPhase == SwitchPhase.IDLE) {
            this.switchPhase = SwitchPhase.FADE_OUT;
            this.switchTimer = 0f;
            this.itemStaggerActive = false;
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
        if (source.expanded) {
            for (List<ModuleWindow> list : this.moduleWindows.values()) {
                for (ModuleWindow win : list) {
                    if (win != source && win.expanded) {
                        win.expanded = false;
                    }
                }
            }
        }
    }

    private void rebuildChildren() {
        this.windows().clear();
        if (this.displayedSelection instanceof ClickGuiSelection.ModuleCategory cat) {
            List<ModuleWindow> list = this.moduleWindows.get(cat.type());
            if (list != null) {
                for (ModuleWindow win : list) {
                    this.addWindow(win);
                }
            }
        } else if (this.displayedSelection instanceof ClickGuiSelection.Special special) {
            if (special.view() == ClickGuiSelection.SpecialView.FRIENDS) {
                this.addWindow(this.friendsPanel);
            } else {
                this.addWindow(this.profilesPanel);
            }
        }
    }

    private String headerTitle() {
        return switch (this.displayedSelection) {
            case ClickGuiSelection.ModuleCategory cat -> cat.type().toString();
            case ClickGuiSelection.Special special -> switch (special.view()) {
                case FRIENDS -> "Friends";
                case PROFILES -> "Profiles";
            };
        };
    }

    @Override
    public int getHeight() {
        int fullBody = this.computeBodyHeight();
        this.bodyHeight = fullBody;
        int full = HEADER_H + (int) (fullBody * this.collapseProgress);
        this.height = full;
        return full;
    }

    private int computeBodyHeight() {
        if (this.displayedSelection instanceof ClickGuiSelection.ModuleCategory cat) {
            List<ModuleWindow> list = this.moduleWindows.get(cat.type());
            if (list == null || list.isEmpty()) return 0;
            int h = 0;
            for (ModuleWindow win : list) {
                h += win.getHeight();
            }
            return h;
        }
        if (this.displayedSelection instanceof ClickGuiSelection.Special special) {
            return switch (special.view()) {
                case FRIENDS -> this.friendsPanel.getHeight() + 2;
                case PROFILES -> this.profilesPanel.getHeight() + 2;
            };
        }
        return 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float dt) {
        boolean animating = this.switchPhase != SwitchPhase.IDLE
                || Math.abs(this.panelOffsetY) > 0.01f
                || Math.abs(this.panelScale - 1f) > 0.001f
                || this.panelAlpha < 0.999f;

        if (!animating) {
            super.render(context, mouseX, mouseY, dt);
            return;
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, this.panelOffsetY);
        float cx = WIDTH / 2f;
        float cy = HEADER_H / 2f;
        context.getMatrices().translate(cx, cy);
        context.getMatrices().scale(this.panelScale, this.panelScale);
        context.getMatrices().translate(-cx, -cy);
        super.render(context, mouseX, mouseY, dt);
        context.getMatrices().popMatrix();
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        float deltaSec = GuiTime.toSeconds(dt);
        float targetCollapse = this.panelCollapsed ? 0f : 1f;
        float collapseStep = deltaSec / COLLAPSE_SEC;
        if (targetCollapse > this.collapseProgress) {
            this.collapseProgress = Math.min(targetCollapse, this.collapseProgress + collapseStep);
        } else {
            this.collapseProgress = Math.max(targetCollapse, this.collapseProgress - collapseStep);
        }

        this.updateSwitchAnimation(deltaSec);

        GUIStyle style = GUIStyle.get();
        int w = WIDTH;
        int innerR = w - INNER_L;
        int h = HEADER_H + (int) (this.bodyHeight * this.collapseProgress);
        boolean roundBottom = this.collapseProgress > 0.98f && !this.panelCollapsed;

        int panelBg = GUIStyle.withAlpha(style.bgPanel(), this.panelAlpha);
        int panelBorder = GUIStyle.withAlpha(style.borderPanel(), this.panelAlpha);
        ModuleTypeWindow.drawRoundedPanel(context, 0, 0, w, Math.max(h, HEADER_H), PANEL_RADIUS, panelBg, panelBorder, roundBottom);

        context.enableScissor(INNER_L, INNER_L, innerR, Math.max(h, HEADER_H) - INNER_L);

        int sepColor = GUIStyle.withAlpha(style.borderSubtle(), this.panelAlpha);
        context.fill(INNER_L, HEADER_H - 1, innerR, HEADER_H, sepColor);

        int titleY = (HEADER_H - style.subHeaderFont().fontHeight) / 2;
        style.subHeaderFont().draw(context, this.headerTitle(), PAD, titleY,
                GUIStyle.withAlpha(style.textBright(), this.panelAlpha));

        float arrowTarget = this.panelCollapsed ? -90f : 90f;
        this.collapseArrowRotation += (arrowTarget - this.collapseArrowRotation) * Math.min(1f, deltaSec * 10f);
        if (Math.abs(this.collapseArrowRotation - arrowTarget) < 0.5f) {
            this.collapseArrowRotation = arrowTarget;
        }

        boolean collapseHovered = mouseX >= w - 30 && mouseX < w - 6 && mouseY >= 6 && mouseY < HEADER_H - 6;
        int collapseColor = GUIStyle.withAlpha(collapseHovered ? 0xFF7A7577 : style.textMuted(), this.panelAlpha);
        ModuleTypeWindow.drawCollapseArrow(context, w - PAD - 2, HEADER_H / 2, collapseColor, this.collapseArrowRotation);

        boolean showBody = this.collapseProgress > 0.01f && this.panelAlpha > 0.01f
                && this.switchPhase != SwitchPhase.PAUSE;
        if (showBody) {
            this.layoutBodyChildren();
            this.enableCutout(INNER_L, HEADER_H, innerR, h - INNER_L);
            this.handleChildren = true;
        } else {
            this.handleChildren = false;
        }

        context.disableScissor();
    }

    private void layoutBodyChildren() {
        int y = HEADER_H;
        if (this.displayedSelection instanceof ClickGuiSelection.ModuleCategory cat) {
            List<ModuleWindow> list = this.moduleWindows.get(cat.type());
            if (list == null) return;
            int idx = 0;
            int total = list.size();
            for (ModuleWindow win : list) {
                int staggerY = Math.round(this.getStaggerOffsetY(idx));
                win.setPosition(0, y + staggerY);
                win.setLastInList(idx == total - 1);
                win.setContentAlpha(this.panelAlpha * this.getStaggerAlpha(idx));
                y += win.getHeight();
                idx++;
            }
        } else {
            Window panel = this.displayedSelection instanceof ClickGuiSelection.Special s
                    && s.view() == ClickGuiSelection.SpecialView.FRIENDS
                    ? this.friendsPanel : this.profilesPanel;
            int staggerY = Math.round(this.getStaggerOffsetY(0));
            panel.setPosition(0, HEADER_H + staggerY);
            float staggerAlpha = this.getStaggerAlpha(0);
            if (panel instanceof FriendsPanelContent f) {
                f.setContentAlpha(this.panelAlpha * staggerAlpha);
            }
            if (panel instanceof ProfilesPanelContent p) {
                p.setContentAlpha(this.panelAlpha * staggerAlpha);
            }
        }
    }

    private boolean isStaggering() {
        return this.switchPhase == SwitchPhase.FADE_IN || this.itemStaggerActive;
    }

    private float staggerTime() {
        return this.switchPhase == SwitchPhase.FADE_IN ? this.switchTimer : this.itemStaggerTimer;
    }

    private float getStaggerAlpha(int index) {
        if (!this.isStaggering()) return 1f;
        float delay = index * ITEM_STAGGER_SEC;
        float t = Math.max(0f, (this.staggerTime() - delay) / ITEM_IN_SEC);
        return Math.min(1f, GuiEasing.smooth(t));
    }

    private float getStaggerOffsetY(int index) {
        if (!this.isStaggering()) return 0f;
        float delay = index * ITEM_STAGGER_SEC;
        float t = Math.max(0f, (this.staggerTime() - delay) / ITEM_IN_SEC);
        return 6f * (1f - GuiEasing.smooth(Math.min(1f, t)));
    }

    private int maxStaggerItems() {
        if (this.displayedSelection instanceof ClickGuiSelection.ModuleCategory cat) {
            List<ModuleWindow> list = this.moduleWindows.get(cat.type());
            return list == null ? 0 : list.size();
        }
        return 1;
    }

    private void updateSwitchAnimation(float deltaSec) {
        if (this.itemStaggerActive) {
            this.itemStaggerTimer += deltaSec;
            float maxDelay = Math.max(0, this.maxStaggerItems() - 1) * ITEM_STAGGER_SEC + ITEM_IN_SEC;
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
                float t = Math.min(1f, this.switchTimer / CAT_OUT_SEC);
                float eased = GuiEasing.smooth(t);
                this.panelAlpha = 1f - eased;
                this.panelOffsetY = 6f * eased;
                this.panelScale = 1f - 0.015f * eased;
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
                    this.panelOffsetY = -6f;
                    this.panelScale = 0.985f;
                }
            }
            case FADE_IN -> {
                float panelT = Math.min(1f, this.switchTimer / CAT_IN_SEC);
                float panelEased = GuiEasing.smooth(panelT);
                this.panelAlpha = panelEased;
                this.panelOffsetY = -6f * (1f - panelEased);
                this.panelScale = 0.985f + 0.015f * panelEased;
                if (panelT >= 1f) {
                    this.switchPhase = SwitchPhase.IDLE;
                    this.panelAlpha = 1f;
                    this.panelOffsetY = 0f;
                    this.panelScale = 1f;
                    if (this.pendingSelection != null && !this.pendingSelection.equals(this.selection)) {
                        this.pendingSelection = this.selection;
                        this.switchPhase = SwitchPhase.FADE_OUT;
                        this.switchTimer = 0f;
                        this.itemStaggerActive = false;
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (click.y() >= 6 && click.y() < HEADER_H - 6 && click.x() >= WIDTH - 30) {
            this.panelCollapsed = !this.panelCollapsed;
            return true;
        }
        if (this.switchPhase != SwitchPhase.IDLE) return true;
        return false;
    }
}
