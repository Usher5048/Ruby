package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;
import ruby.RubyClient;
import ruby.systems.accounts.Account;
import ruby.systems.accounts.AccountsManager;
import ruby.systems.accounts.MicrosoftLogin;
import ruby.systems.accounts.types.CrackedAccount;
import ruby.systems.accounts.types.MicrosoftAccount;
import ruby.systems.accounts.types.SessionAccount;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;
import ruby.systems.gui.Toasts;
import ruby.systems.gui.icons.Lucide;
import ruby.systems.social.PlayerHeadCache;

import java.util.List;

public class AccountsPanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 31;
    private static final int LAST_EXTRA = 4;
    private static final int HEAD_SIZE = 16;

    private enum AddMode { NONE, PICK, CRACKED, SESSION }

    private static final String[] TYPE_LABELS = { "Cracked", "Microsoft", "Session" };
    private static final String[] TYPE_ICONS = { "user", "users", "clipboard" };

    private AddMode addMode = AddMode.NONE;
    private String inputPrimary = "";
    private String inputSecondary = "";
    private int cursorBlink = 0;
    private boolean focusPrimary = true;
    private float contentAlpha = 1f;
    private float addMix;
    private final float[] typeMix = new float[3];
    private float[] rowMix;

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    public AccountsPanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    private int extraRows() {
        return switch (this.addMode) {
            case PICK -> 3;
            case CRACKED -> 1;
            case SESSION -> 2;
            case NONE -> AccountsManager.getAccounts().isEmpty() ? 1 : 0;
        };
    }

    private int bodyRows() {
        return 1 + this.extraRows() + AccountsManager.getAccounts().size();
    }

    @Override
    public int getHeight() {
        int h = this.bodyRows() * ROW_H + LAST_EXTRA;
        this.height = h;
        return h;
    }

    @Override
    public void onTick() {
        this.cursorBlink++;
        if (this.cursorBlink > 20) this.cursorBlink = 0;
    }

    @Override
    public void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float dt) {
        this.drawContent(context, mouseX, mouseY, GuiTime.toSeconds(dt));
    }

    private void drawContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float seconds) {
        GUIStyle style = GUIStyle.get();
        int innerR = this.getWidth() - INNER_L;
        List<Account> accounts = AccountsManager.getAccounts();
        int rows = this.bodyRows();
        int y = 0;
        int row = 0;

        y = this.drawAddRow(context, style, innerR, y, mouseX, mouseY, row == rows - 1, seconds);
        row++;

        switch (this.addMode) {
            case PICK -> {
                for (int i = 0; i < TYPE_LABELS.length; i++) {
                    y = this.drawTypeRow(context, style, innerR, y, mouseX, mouseY, i, row == rows - 1, seconds);
                    row++;
                }
            }
            case CRACKED -> {
                y = this.drawInputRow(context, style, innerR, y, mouseX, mouseY,
                        "Username", this.inputPrimary, true, "acct-create", row == rows - 1);
                row++;
            }
            case SESSION -> {
                y = this.drawInputRow(context, style, innerR, y, mouseX, mouseY,
                        "Label", this.inputPrimary, this.focusPrimary, null, false);
                row++;
                y = this.drawInputRow(context, style, innerR, y, mouseX, mouseY,
                        "Access token", this.inputSecondary, !this.focusPrimary, "acct-create", row == rows - 1);
                row++;
            }
            case NONE -> {
                if (accounts.isEmpty()) {
                    y = this.drawEmptyRow(context, style, innerR, y, row == rows - 1);
                    row++;
                }
            }
        }

        for (int i = 0; i < accounts.size(); i++) {
            y = this.drawAccount(context, style, innerR, y, mouseX, mouseY, accounts.get(i), i,
                    row == rows - 1, seconds);
            row++;
        }
    }

    private int rowHeight(boolean last) {
        return last ? ROW_H + LAST_EXTRA : ROW_H;
    }

    private int drawAddRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                           int mouseX, int mouseY, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        boolean open = this.addMode != AddMode.NONE;
        this.addMix = Motion.follow(this.addMix, hovered || open ? 1f : 0f, seconds, Motion.HOVER);
        if (this.addMix > 0.01f) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * this.addMix), last);
        }

        int iconY = y + (ROW_H - Lucide.UI) / 2;
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        int iconColor = Motion.mix(style.textMuted(), style.ruby(), this.addMix);
        int textColor = Motion.mix(style.text(), style.textBright(), this.addMix);
        Press.icon(context, "plus", PAD, iconY, GUIStyle.withAlpha(iconColor, this.contentAlpha), "acct-add");
        Press.label(style.bodyFont(), context, "Add account", PAD + Lucide.UI + 8, textY,
                GUIStyle.withAlpha(textColor, this.contentAlpha), "acct-add");
        this.separator(context, style, innerR, y, rowH, last);
        return y + rowH;
    }

    private int drawTypeRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                            int mouseX, int mouseY, int index, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        this.typeMix[index] = Motion.follow(this.typeMix[index], hovered ? 1f : 0f, seconds, Motion.HOVER);
        if (this.typeMix[index] > 0.01f) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * this.typeMix[index]), last);
        }

        int iconY = y + (ROW_H - Lucide.UI) / 2;
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        String id = "acct-type:" + index;
        int color = Motion.mix(style.text(), style.textBright(), this.typeMix[index]);
        Press.icon(context, TYPE_ICONS[index], PAD, iconY, GUIStyle.withAlpha(color, this.contentAlpha), id);
        Press.label(style.bodyFont(), context, TYPE_LABELS[index], PAD + Lucide.UI + 8, textY,
                GUIStyle.withAlpha(color, this.contentAlpha), id);
        this.separator(context, style, innerR, y, rowH, last);
        return y + rowH;
    }

    private int drawInputRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                             int mouseX, int mouseY, String placeholder, String value,
                             boolean focused, String actionId, boolean last) {
        int rowH = this.rowHeight(last);
        context.fill(INNER_L, y, innerR, y + ROW_H, GUIStyle.withAlpha(style.bgBase(), this.contentAlpha));

        String display = value.isEmpty() ? placeholder : value;
        int col = value.isEmpty() ? style.textMuted() : style.text();
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        int textR = innerR - PAD;
        if (actionId != null) {
            int actionW = style.bodyFont().getWidth("Add");
            int actionX = innerR - PAD - actionW;
            textR = actionX - 8;
            boolean actionHovered = mouseX >= actionX - 6 && mouseX < innerR - 6
                    && mouseY >= y && mouseY < y + rowH;
            Press.label(style.bodyFont(), context, "Add", actionX, textY,
                    GUIStyle.withAlpha(actionHovered ? style.rubyHover() : style.ruby(), this.contentAlpha), actionId);
        }

        style.bodyFont().draw(context, display, PAD, textY, GUIStyle.withAlpha(col, this.contentAlpha));
        if (focused && this.cursorBlink < 10) {
            int cx = Math.min(PAD + style.bodyFont().getWidth(value), textR);
            context.fill(cx, textY, cx + 1, textY + style.bodyFont().fontHeight,
                    GUIStyle.withAlpha(style.text(), this.contentAlpha));
        }

        this.separator(context, style, innerR, y, rowH, last);
        return y + rowH;
    }

    private int drawEmptyRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y, boolean last) {
        int rowH = this.rowHeight(last);
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, "No accounts yet", PAD, textY,
                GUIStyle.withAlpha(style.textMuted(), this.contentAlpha));
        this.separator(context, style, innerR, y, rowH, last);
        return y + rowH;
    }

    private int drawAccount(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                            int mouseX, int mouseY, Account account, int index, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        float mix = this.rowMix(index, hovered ? 1f : 0f, seconds);
        if (mix > 0.01f) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * mix), last);
        }

        String username = account.getUsername();
        boolean active = AccountsManager.isActive(account);
        int headY = y + (ROW_H - HEAD_SIZE) / 2;
        this.drawPlayerHead(context, style, username, PAD, headY);

        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        int nameX = PAD + HEAD_SIZE + 8;
        int nameColor = active ? style.ruby() : style.text();
        style.bodyFont().draw(context, username, nameX, textY, GUIStyle.withAlpha(nameColor, this.contentAlpha));

        int iconX = innerR - PAD - Lucide.UI;
        int iconY = y + (ROW_H - Lucide.UI) / 2;
        String removeId = "acct-x:" + username;
        boolean removeHovered = mouseX >= iconX - 6 && mouseX < innerR - 6
                && mouseY >= y && mouseY < y + ROW_H;
        Press.icon(context, "x", iconX, iconY,
                GUIStyle.withAlpha(removeHovered ? 0xFFC25555 : style.textMuted(), this.contentAlpha), removeId);

        if (!active) {
            int loginX = iconX - 8 - Lucide.UI;
            String loginId = "acct-login:" + username;
            boolean loginHovered = mouseX >= loginX - 6 && mouseX < loginX + Lucide.UI + 6
                    && mouseY >= y && mouseY < y + ROW_H;
            Press.icon(context, "forward", loginX, iconY,
                    GUIStyle.withAlpha(loginHovered ? style.ruby() : style.textMuted(), this.contentAlpha), loginId);
        } else {
            int checkX = iconX - 8 - Lucide.UI;
            Lucide.draw(context, "check", checkX, iconY, Lucide.UI,
                    GUIStyle.withAlpha(style.ruby(), this.contentAlpha));
        }

        this.separator(context, style, innerR, y, rowH, last);
        return y + rowH;
    }

    private float rowMix(int index, float target, float seconds) {
        if (this.rowMix == null || this.rowMix.length <= index) {
            float[] next = new float[index + 1];
            if (this.rowMix != null) System.arraycopy(this.rowMix, 0, next, 0, this.rowMix.length);
            next[index] = target;
            this.rowMix = next;
            return target;
        }
        this.rowMix[index] = Motion.follow(this.rowMix[index], target, seconds, Motion.HOVER);
        return this.rowMix[index];
    }

    private void separator(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y, int rowH, boolean last) {
        if (last) return;
        context.fill(INNER_L, y + rowH - 1, innerR, y + rowH,
                GUIStyle.withAlpha(style.borderSubtle(), this.contentAlpha));
    }

    private void drawPlayerHead(GuiGraphicsExtractor context, GUIStyle style, String username, int x, int y) {
        if (PlayerHeadCache.getSkin(username) != null) {
            PlayerHeadCache.drawHead(context, username, x, y, HEAD_SIZE);
        } else {
            ModuleTypeWindow.fillSmoothRoundedRect(context, x, y, x + HEAD_SIZE, y + HEAD_SIZE, GUIStyle.RADIUS_BADGE,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha));
        }
    }

    private void resetAddState() {
        this.addMode = AddMode.NONE;
        this.inputPrimary = "";
        this.inputSecondary = "";
        this.focusPrimary = true;
    }

    private void beginMicrosoft() {
        MicrosoftLogin.requestRefreshToken(token -> {
            if (token == null) {
                RubyClient.client.execute(() -> Toasts.show("x", "Microsoft login failed"));
                return;
            }
            Util.ioPool().execute(() -> {
                MicrosoftAccount account = new MicrosoftAccount(token);
                boolean ok = account.fetchInfo();
                RubyClient.client.execute(() -> {
                    if (!ok) {
                        Toasts.show("x", "Microsoft login failed");
                        return;
                    }
                    AccountsManager.add(account);
                    ConfigManager.saveState();
                    Toasts.show("check", "Added " + account.getUsername());
                });
            });
        });
    }

    private void tryLogin(Account account) {
        Toasts.show("forward", "Logging in");
        Util.ioPool().execute(() -> {
            boolean prepared = account.fetchInfo();
            RubyClient.client.execute(() -> {
                if (prepared && account.login()) {
                    AccountsManager.markLastUsed(account);
                    ConfigManager.saveState();
                    Toasts.show("check", "Logged in as " + account.getUsername());
                } else {
                    Toasts.show("x", "Login failed");
                }
            });
        });
    }

    private void addSession() {
        if (this.inputPrimary.isBlank() || this.inputSecondary.isBlank()) return;
        String label = this.inputPrimary.trim();
        String token = this.inputSecondary.trim();
        Toasts.show("clipboard", "Checking session");
        Util.ioPool().execute(() -> {
            SessionAccount account = new SessionAccount(label, token);
            boolean ok = account.fetchInfo();
            RubyClient.client.execute(() -> {
                if (!ok) {
                    Toasts.show("x", "Invalid session");
                    return;
                }
                AccountsManager.add(account);
                ConfigManager.saveState();
                Toasts.show("check", "Added " + account.getUsername());
            });
        });
    }

    private boolean hitAdd(int contentR, double x, double y) {
        return x >= INNER_L && x < contentR && y >= 0 && y < ROW_H;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        int innerR = this.getWidth() - INNER_L;
        int y = 0;
        List<Account> accounts = AccountsManager.getAccounts();

        if (this.hitAdd(innerR, click.x(), click.y())) {
            Press.down("acct-add");
            if (this.addMode == AddMode.NONE) this.addMode = AddMode.PICK;
            else this.resetAddState();
            return true;
        }
        y += ROW_H;

        switch (this.addMode) {
            case PICK -> {
                for (int i = 0; i < TYPE_LABELS.length; i++) {
                    if (click.y() >= y && click.y() < y + ROW_H) {
                        Press.down("acct-type:" + i);
                        this.resetAddState();
                        if (i == 0) this.addMode = AddMode.CRACKED;
                        else if (i == 1) this.beginMicrosoft();
                        else this.addMode = AddMode.SESSION;
                        return true;
                    }
                    y += ROW_H;
                }
            }
            case CRACKED -> {
                if (this.hitAction(innerR, y, click.x(), click.y())) {
                    Press.down("acct-create");
                    if (!this.inputPrimary.isBlank()) {
                        String name = this.inputPrimary.trim();
                        AccountsManager.add(new CrackedAccount(name));
                        ConfigManager.saveState();
                        Toasts.show("check", "Added " + name);
                    }
                    this.resetAddState();
                    return true;
                }
                if (click.y() >= y && click.y() < y + ROW_H) {
                    this.focusPrimary = true;
                    return true;
                }
                y += ROW_H;
            }
            case SESSION -> {
                if (click.y() >= y && click.y() < y + ROW_H) {
                    this.focusPrimary = true;
                    return true;
                }
                y += ROW_H;
                if (this.hitAction(innerR, y, click.x(), click.y())) {
                    Press.down("acct-create");
                    this.addSession();
                    this.resetAddState();
                    return true;
                }
                if (click.y() >= y && click.y() < y + ROW_H) {
                    this.focusPrimary = false;
                    return true;
                }
                y += ROW_H;
            }
            case NONE -> {
                if (accounts.isEmpty()) y += ROW_H;
            }
        }

        for (Account account : accounts) {
            if (click.y() >= y && click.y() < y + ROW_H) {
                String username = account.getUsername();
                int iconX = innerR - PAD - Lucide.UI;
                if (click.x() >= iconX - 6) {
                    Press.down("acct-x:" + username);
                    AccountsManager.remove(account);
                    ConfigManager.saveState();
                    Toasts.show("x", "Removed " + username);
                    return true;
                }
                if (!AccountsManager.isActive(account)) {
                    int loginX = iconX - 8 - Lucide.UI;
                    if (click.x() >= loginX - 6 && click.x() < loginX + Lucide.UI + 6) {
                        Press.down("acct-login:" + username);
                        this.tryLogin(account);
                        return true;
                    }
                }
                return true;
            }
            y += ROW_H;
        }

        return false;
    }

    private boolean hitAction(int innerR, int y, double x, double yClick) {
        int actionW = GUIStyle.get().bodyFont().getWidth("Add");
        int actionX = innerR - PAD - actionW;
        return x >= actionX - 6 && x < innerR - 6 && yClick >= y && yClick < y + ROW_H;
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        if (this.addMode != AddMode.CRACKED && this.addMode != AddMode.SESSION) return false;

        if (input.key() == InputConstants.KEY_ESCAPE) {
            this.resetAddState();
            return true;
        }
        if (input.key() == InputConstants.KEY_BACKSPACE) {
            if (this.focusPrimary && !this.inputPrimary.isEmpty()) {
                this.inputPrimary = this.inputPrimary.substring(0, this.inputPrimary.length() - 1);
            } else if (!this.focusPrimary && !this.inputSecondary.isEmpty()) {
                this.inputSecondary = this.inputSecondary.substring(0, this.inputSecondary.length() - 1);
            }
            return true;
        }
        if (input.key() == InputConstants.KEY_RETURN) {
            Press.down("acct-create");
            if (this.addMode == AddMode.CRACKED && !this.inputPrimary.isBlank()) {
                String name = this.inputPrimary.trim();
                AccountsManager.add(new CrackedAccount(name));
                ConfigManager.saveState();
                Toasts.show("check", "Added " + name);
                this.resetAddState();
            } else if (this.addMode == AddMode.SESSION) {
                this.addSession();
                this.resetAddState();
            }
            return true;
        }
        if (this.addMode == AddMode.SESSION && input.key() == InputConstants.KEY_TAB) {
            this.focusPrimary = !this.focusPrimary;
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        if (this.addMode != AddMode.CRACKED && this.addMode != AddMode.SESSION) return false;
        char c = (char) input.codepoint();
        if (c < 32 || c >= 127) return false;

        if (this.addMode == AddMode.CRACKED) {
            if (this.inputPrimary.length() < 16) this.inputPrimary += c;
            return true;
        }

        if (this.focusPrimary) {
            if (this.inputPrimary.length() < 24) this.inputPrimary += c;
        } else if (this.inputSecondary.length() < 2048) {
            this.inputSecondary += c;
        }
        return true;
    }
}
