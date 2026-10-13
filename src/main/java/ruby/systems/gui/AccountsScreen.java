package ruby.systems.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import ruby.RubyClient;
import ruby.systems.gui.windows.AccountsPanelContent;
import ruby.systems.gui.windows.ModuleTypeWindow;
import ruby.systems.gui.windows.WindowedScreen;

public class AccountsScreen extends WindowedScreen {
    private static final int PANEL_W = 320;
    private static final int HEADER_H = 41;
    private static final int LIFT = 20;

    private final Screen parent;
    private AccountsPanelContent panel;
    private float shown = 0f;
    private float animFrom = 0f;
    private float animTo = 1f;
    private long animStart;
    private boolean opening = false;
    private boolean closing = false;
    private boolean dismissed = false;

    public AccountsScreen(Screen parent) {
        super("Accounts");
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (!this.opening) {
            this.opening = true;
            this.closing = false;
            this.play(1f);
        }
        this.layoutPanel();
    }

    private void layoutPanel() {
        float sf = (float) RubyClient.client.getWindow().getGuiScale();
        int contentH = this.panel == null ? ROW_FALLBACK : this.panel.getHeight();
        int x = this.cardX(sf);
        int y = this.cardY(sf, contentH) + HEADER_H;
        if (this.panel == null) {
            this.panel = new AccountsPanelContent(x, y, PANEL_W);
            this.addWindow(this.panel);
        } else {
            this.panel.setPosition(x, y);
        }
    }

    private static final int ROW_FALLBACK = 31;

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        if (this.parent != null) {
            context.pose().pushMatrix();
            this.parent.extractRenderStateWithTooltipAndSubtitles(context, mouseX, mouseY, partialTick);
            context.pose().popMatrix();
        } else {
            super.extractBackground(context, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        this.advance();
        int dim = GUIStyle.get().overlayDim();
        int dimAlpha = Math.round(((dim >>> 24) & 0xFF) * this.shown);
        ClickGUI.opacity = 1f;
        context.fill(0, 0, this.width, this.height, (dimAlpha << 24) | (dim & 0x00FFFFFF));
        ClickGUI.opacity = this.shown;
        try {
            float rise = ClientSettings.get().animations() ? Motion.RISE * (1f - this.shown) : 0f;
            context.pose().pushMatrix();
            if (rise != 0f) context.pose().translate(0f, rise);
            super.extractRenderState(context, mouseX, mouseY, tickDelta);
            context.pose().popMatrix();
        } finally {
            ClickGUI.opacity = 1f;
        }
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        GUIStyle style = GUIStyle.get();
        this.layoutPanel();

        float sf = (float) RubyClient.client.getWindow().getGuiScale();
        int contentH = this.panel.getHeight();
        int totalH = HEADER_H + contentH;
        int px = this.cardX(sf);
        int py = this.cardY(sf, contentH);
        ModuleTypeWindow.drawRoundedPanel(context, px - 1, py, px + PANEL_W + 1, py + totalH,
                GUIStyle.RADIUS_PANEL, style.bgPanel(), style.borderPanel());
        style.logoFont().draw(context, "Accounts", px + 14, py + 8, style.ruby());
        style.bodyFont().draw(context, "Logged in as " + RubyClient.client.getUser().getName(),
                px + 14, py + HEADER_H - 16, style.textMuted());
    }

    private void play(float to) {
        this.animFrom = this.shown;
        this.animTo = to;
        this.animStart = System.nanoTime();
        if (!ClientSettings.get().animations()) this.shown = to;
    }

    private void advance() {
        if (!ClientSettings.get().animations()) {
            this.shown = this.closing ? 0f : 1f;
            return;
        }
        float dur = this.closing ? Motion.EXIT : Motion.ENTER;
        float t = (System.nanoTime() - this.animStart) / 1_000_000_000f / dur;
        this.shown = this.animFrom + (this.animTo - this.animFrom) * Motion.out(Math.clamp(t, 0f, 1f));
    }

    @Override
    public void onTick() {
        if (this.closing && this.shown <= 0.001f) this.dismiss();
    }

    private void requestClose() {
        if (this.closing) return;
        this.closing = true;
        this.play(0f);
    }

    private void dismiss() {
        if (this.dismissed) return;
        this.dismissed = true;
        RubyClient.client.gui.setScreen(this.parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        if (this.closing) return true;
        if (super.mouseClicked(click, doubled)) return true;
        if (this.insideCard(click.x(), click.y())) return true;
        this.requestClose();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (super.keyPressed(input)) return true;
        if (input.isEscape()) {
            this.requestClose();
            return true;
        }
        return false;
    }

    private int cardX(float sf) {
        return Math.round((this.width * sf - PANEL_W) / 2f);
    }

    private int cardY(float sf, int contentH) {
        int totalH = HEADER_H + contentH;
        return Math.round((this.height * sf - totalH) / 2f) - LIFT;
    }

    private boolean insideCard(double mouseX, double mouseY) {
        float sf = (float) RubyClient.client.getWindow().getGuiScale();
        int contentH = this.panel == null ? ROW_FALLBACK : this.panel.getHeight();
        int px = this.cardX(sf);
        int py = this.cardY(sf, contentH);
        double sx = mouseX * sf;
        double sy = mouseY * sf;
        return sx >= px && sy >= py && sx < px + PANEL_W && sy < py + HEADER_H + contentH;
    }
}
