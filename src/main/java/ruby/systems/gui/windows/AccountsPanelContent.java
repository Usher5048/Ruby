package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ruby.systems.accounts.Account;
import ruby.systems.accounts.AccountsManager;
import ruby.systems.accounts.MicrosoftLogin;
import ruby.systems.accounts.types.CrackedAccount;
import ruby.systems.accounts.types.MicrosoftAccount;
import ruby.systems.accounts.types.SessionAccount;
import ruby.systems.gui.GUIStyle;
import ruby.systems.social.PlayerHeadCache;

import java.util.List;

public class AccountsPanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 44;
    private static final int ADD_BTN_ROW_H = 44;
    private static final int TYPE_ROW_H = 36;
    private static final int INPUT_ROW_H = 40;
    private static final int SESSION_INPUT_ROW_H = 70;
    private static final int HEAD_SIZE = 24;
    private static final int ACTION_BTN_W = 48;
    private static final int REMOVE_INSET = 18;
    private static final int ACTION_GAP = 4;
    private static final int INPUT_GAP = 6;
    private static final int CREATE_BTN_W = 52;

    private enum AddMode { NONE, PICK_TYPE, CRACKED, SESSION }

    private AddMode addMode = AddMode.NONE;
    private String inputPrimary = "";
    private String inputSecondary = "";
    private int cursorBlink = 0;
    private boolean focusPrimary = true;
    private float contentAlpha = 1f;
    private String statusMessage = "";

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    public void resetState() {
        this.resetAddState();
        this.statusMessage = "";
    }

    public AccountsPanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    @Override
    public int getHeight() {
        int h = switch (this.addMode) {
            case NONE -> ADD_BTN_ROW_H;
            case PICK_TYPE -> ADD_BTN_ROW_H + TYPE_ROW_H * 3;
            case CRACKED -> INPUT_ROW_H;
            case SESSION -> SESSION_INPUT_ROW_H;
        };
        h += AccountsManager.getAccounts().size() * ROW_H;
        if (AccountsManager.getAccounts().isEmpty() && this.addMode == AddMode.NONE) h += 32;
        if (!this.statusMessage.isEmpty()) h += 20;
        this.height = h;
        return h;
    }

    @Override
    public void onTick() {
        this.cursorBlink++;
        if (this.cursorBlink > 20) this.cursorBlink = 0;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY, float dt) {
        this.drawContent(context, mouseX, mouseY);
    }

    private void drawContent(DrawContext context, int mouseX, int mouseY) {
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();
        int innerR = w - INNER_L;
        int y = 0;
        List<Account> accounts = AccountsManager.getAccounts();

        y = switch (this.addMode) {
            case NONE -> this.drawAddButton(context, style, innerR, y, mouseX, mouseY);
            case PICK_TYPE -> this.drawTypePicker(context, style, innerR, y, mouseX, mouseY);
            case CRACKED -> this.drawSingleInput(context, style, innerR, y, mouseX, mouseY,
                    "Username...", this.inputPrimary, true);
            case SESSION -> this.drawDualInput(context, style, innerR, y, mouseX, mouseY);
        };

        if (!this.statusMessage.isEmpty()) {
            style.bodyFont().draw(context, this.statusMessage, PAD, y + 4,
                    GUIStyle.withAlpha(style.textMuted(), this.contentAlpha));
            y += 20;
        }

        if (accounts.isEmpty() && this.addMode == AddMode.NONE) {
            ModuleTypeWindow.fillBottomRoundedRect(context, INNER_L, y, innerR, y + 32, GUIStyle.RADIUS_ROW,
                    GUIStyle.withAlpha(style.bgPanel(), this.contentAlpha));
            style.bodyFont().draw(context, "No accounts saved", PAD, y + 8,
                    GUIStyle.withAlpha(style.textMuted(), this.contentAlpha));
            y += 32;
        }

        for (int i = 0; i < accounts.size(); i++) {
            y = this.drawAccountRow(context, style, innerR, y, mouseX, mouseY, accounts.get(i), i == accounts.size() - 1);
        }
    }

    private int drawAddButton(DrawContext context, GUIStyle style, int innerR, int y, int mx, int my) {
        int btnR = innerR - PAD;
        int by = y + 10;
        int bh = 24;
        boolean hovered = mx >= PAD && mx < btnR && my >= by && my < by + bh;
        ModuleTypeWindow.drawRoundedBadge(context, PAD, by, btnR, by + bh,
                hovered ? style.rubyBg() : 0x00000000, style.border());
        this.drawTextInRect(style.bodyFont(), context, "+ Add Account", PAD, by, btnR, by + bh,
                GUIStyle.withAlpha(hovered ? style.textBright() : style.ruby(), this.contentAlpha));
        context.fill(INNER_L, y + ADD_BTN_ROW_H - 1, innerR, y + ADD_BTN_ROW_H, style.borderSubtle());
        return y + ADD_BTN_ROW_H;
    }

    private int drawTypePicker(DrawContext context, GUIStyle style, int innerR, int y, int mx, int my) {
        y = this.drawAddButton(context, style, innerR, y, mx, my);
        String[] labels = {"Cracked", "Microsoft", "Session"};
        for (int i = 0; i < labels.length; i++) {
            boolean hovered = mx >= INNER_L && my >= y && mx < innerR && my < y + TYPE_ROW_H;
            if (hovered) {
                ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + TYPE_ROW_H,
                        GUIStyle.withAlpha(style.bgHover(), this.contentAlpha), i == labels.length - 1);
            }
            int textY = y + (TYPE_ROW_H - style.bodyFont().fontHeight) / 2;
            style.bodyFont().draw(context, labels[i], PAD, textY,
                    GUIStyle.withAlpha(hovered ? style.textBright() : style.text(), this.contentAlpha));
            if (i < labels.length - 1) {
                context.fill(INNER_L, y + TYPE_ROW_H - 1, innerR, y + TYPE_ROW_H, style.borderSubtle());
            }
            y += TYPE_ROW_H;
        }
        return y;
    }

    private int drawSingleInput(DrawContext context, GUIStyle style, int innerR, int y, int mx, int my,
                                String placeholder, String value, boolean primary) {
        return this.drawInputRow(context, style, innerR, y, mx, my, placeholder, value, primary, false);
    }

    private int drawDualInput(DrawContext context, GUIStyle style, int innerR, int y, int mx, int my) {
        int rowY = y + 8;
        int inputH = 24;
        int contentR = innerR - PAD;
        int btnX = contentR - CREATE_BTN_W;

        ModuleTypeWindow.drawRoundedBadge(context, PAD, rowY, btnX - INPUT_GAP, rowY + inputH,
                style.bgBase(), style.border());
        this.drawInputText(context, style, PAD + 8, rowY, inputH, "Label...", this.inputPrimary, this.focusPrimary);

        int row2 = rowY + inputH + 6;
        ModuleTypeWindow.drawRoundedBadge(context, PAD, row2, btnX - INPUT_GAP, row2 + inputH,
                style.bgBase(), style.border());
        this.drawInputText(context, style, PAD + 8, row2, inputH, "Access token...", this.inputSecondary, !this.focusPrimary);

        boolean addHovered = mx >= btnX && mx < contentR && my >= rowY && my < row2 + inputH;
        ModuleTypeWindow.fillSmoothRoundedRect(context, btnX, rowY, contentR, row2 + inputH, GUIStyle.RADIUS_BADGE,
                GUIStyle.withAlpha(addHovered ? style.rubyHover() : style.ruby(), this.contentAlpha));
        this.drawTextInRect(style.bodyFont(), context, "Add", btnX, rowY, contentR, row2 + inputH, 0xFFE8E4E5);

        context.fill(INNER_L, y + SESSION_INPUT_ROW_H - 1, innerR, y + SESSION_INPUT_ROW_H, style.borderSubtle());
        return y + SESSION_INPUT_ROW_H;
    }

    private int drawInputRow(DrawContext context, GUIStyle style, int innerR, int y, int mx, int my,
                             String placeholder, String value, boolean primary, boolean unused) {
        int rowY = y + 8;
        int inputH = 24;
        int contentR = innerR - PAD;
        int btnX = contentR - CREATE_BTN_W;
        int inputR = btnX - INPUT_GAP;

        ModuleTypeWindow.drawRoundedBadge(context, PAD, rowY, inputR, rowY + inputH, style.bgBase(), style.border());
        this.drawInputText(context, style, PAD + 8, rowY, inputH, placeholder, value, primary);

        boolean addHovered = mx >= btnX && mx < contentR && my >= rowY && my < rowY + inputH;
        ModuleTypeWindow.fillSmoothRoundedRect(context, btnX, rowY, contentR, rowY + inputH, GUIStyle.RADIUS_BADGE,
                GUIStyle.withAlpha(addHovered ? style.rubyHover() : style.ruby(), this.contentAlpha));
        this.drawTextInRect(style.bodyFont(), context, "Add", btnX, rowY, contentR, rowY + inputH, 0xFFE8E4E5);

        context.fill(INNER_L, y + INPUT_ROW_H - 1, innerR, y + INPUT_ROW_H, style.borderSubtle());
        return y + INPUT_ROW_H;
    }

    private void drawInputText(DrawContext context, GUIStyle style, int x, int rowY, int inputH,
                               String placeholder, String value, boolean focused) {
        String display = value.isEmpty() ? placeholder : value;
        int col = value.isEmpty() ? style.textMuted() : style.text();
        int textY = rowY + (inputH - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, display, x, textY, GUIStyle.withAlpha(col, this.contentAlpha));
        if (focused && this.cursorBlink < 10) {
            int cx = x + style.bodyFont().getWidth(value);
            context.fill(cx, textY, cx + 1, textY + style.bodyFont().fontHeight,
                    GUIStyle.withAlpha(style.text(), this.contentAlpha));
        }
    }

    private int drawAccountRow(DrawContext context, GUIStyle style, int innerR, int y, int mx, int my,
                               Account account, boolean last) {
        boolean hovered = mx >= INNER_L && my >= y && mx < innerR && my < y + ROW_H;
        if (hovered) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + ROW_H,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha), last);
        }

        String username = account.getUsername();
        this.drawPlayerHead(context, style, username, PAD, y + (ROW_H - HEAD_SIZE) / 2);

        int textY = y + 10;
        style.bodyFont().draw(context, username, PAD + HEAD_SIZE + 10, textY,
                GUIStyle.withAlpha(style.text(), this.contentAlpha));
        style.labelFont().draw(context, account.getType().label(), PAD + HEAD_SIZE + 10, textY + 14,
                GUIStyle.withAlpha(style.textMuted(), this.contentAlpha));

        boolean active = AccountsManager.isActive(account);
        int removeX = innerR - REMOVE_INSET;
        int actionRight = removeX - ACTION_GAP;
        int actionX = actionRight - ACTION_BTN_W;
        int actionY = y + (ROW_H - 24) / 2;
        if (active) {
            String activeLabel = "Active";
            int activeW = style.bodyFont().getWidth(activeLabel);
            style.bodyFont().draw(context, activeLabel, actionRight - activeW,
                    y + (ROW_H - style.bodyFont().fontHeight) / 2,
                    GUIStyle.withAlpha(style.ruby(), this.contentAlpha));
        } else {
            boolean loginHovered = mx >= actionX && mx < actionX + ACTION_BTN_W
                    && my >= actionY && my < actionY + 24;
            ModuleTypeWindow.drawRoundedBadge(context, actionX, actionY, actionX + ACTION_BTN_W, actionY + 24,
                    loginHovered ? style.rubyBg() : 0x00000000, style.border());
            this.drawTextInRect(style.bodyFont(), context, "Login", actionX, actionY,
                    actionX + ACTION_BTN_W, actionY + 24,
                    GUIStyle.withAlpha(loginHovered ? style.textBright() : style.ruby(), this.contentAlpha));
        }

        boolean removeHovered = mx >= innerR - 28 && mx < innerR - 8
                && my >= y + 8 && my < y + ROW_H - 8;
        int removeY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, "\u2715", removeX, removeY,
                GUIStyle.withAlpha(removeHovered ? 0xFFC25555 : style.textMuted(), this.contentAlpha));

        if (!last) {
            context.fill(INNER_L, y + ROW_H - 1, innerR, y + ROW_H, style.borderSubtle());
        }
        return y + ROW_H;
    }

    private void drawPlayerHead(DrawContext context, GUIStyle style, String username, int x, int y) {
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

    private void tryLogin(Account account) {
        this.statusMessage = "Logging in...";
        Util.getIoWorkerExecutor().execute(() -> {
            boolean ok = account.fetchInfo() && account.login();
            Util.getMainWorkerExecutor().execute(() -> {
                if (ok) {
                    AccountsManager.markLastUsed(account);
//                    AccountsManager.save();
                    this.statusMessage = "Logged in as " + account.getUsername();
                } else {
                    this.statusMessage = "Login failed";
                }
            });
        });
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        int w = this.getWidth();
        int innerR = w - INNER_L;
        int y = 0;
        List<Account> accounts = AccountsManager.getAccounts();
        int contentR = innerR - PAD;

        switch (this.addMode) {
            case NONE -> {
                if (click.x() >= PAD && click.x() < contentR && click.y() >= y + 10 && click.y() < y + 34) {
                    this.addMode = AddMode.PICK_TYPE;
                    return true;
                }
                y += ADD_BTN_ROW_H;
            }
            case PICK_TYPE -> {
                y += ADD_BTN_ROW_H;
                if (click.y() >= y && click.y() < y + TYPE_ROW_H) {
                    this.resetAddState();
                    this.addMode = AddMode.CRACKED;
                    return true;
                }
                y += TYPE_ROW_H;
                if (click.y() >= y && click.y() < y + TYPE_ROW_H) {
                    this.statusMessage = "Check your browser...";
                    MicrosoftLogin.requestRefreshToken(token -> Util.getMainWorkerExecutor().execute(() -> {
                        if (token == null) {
                            this.statusMessage = "Microsoft login cancelled";
                            return;
                        }
                        MicrosoftAccount account = new MicrosoftAccount(token);
                        if (account.fetchInfo()) {
                            AccountsManager.add(account);
                            this.statusMessage = "Added " + account.getUsername();
                            this.resetAddState();
                        } else {
                            this.statusMessage = "Microsoft login failed";
                        }
                    }));
                    this.resetAddState();
                    return true;
                }
                y += TYPE_ROW_H;
                if (click.y() >= y && click.y() < y + TYPE_ROW_H) {
                    this.resetAddState();
                    this.addMode = AddMode.SESSION;
                    return true;
                }
                y += TYPE_ROW_H;
            }
            case CRACKED -> {
                int rowY = y + 8;
                int btnX = contentR - CREATE_BTN_W;
                if (click.x() >= btnX && click.x() < contentR && click.y() >= rowY && click.y() < rowY + 24) {
                    if (!this.inputPrimary.isBlank()) {
                        AccountsManager.add(new CrackedAccount(this.inputPrimary.trim()));
                        this.statusMessage = "Added " + this.inputPrimary.trim();
                    }
                    this.resetAddState();
                    return true;
                }
                int inputR = btnX - INPUT_GAP;
                if (click.x() >= PAD && click.x() < inputR && click.y() >= rowY && click.y() < rowY + 24) {
                    this.focusPrimary = true;
                    return true;
                }
                y += INPUT_ROW_H;
            }
            case SESSION -> {
                int rowY = y + 8;
                int btnX = contentR - CREATE_BTN_W;
                if (click.x() >= btnX && click.x() < contentR && click.y() >= rowY && click.y() < rowY + SESSION_INPUT_ROW_H - 8) {
                    if (!this.inputPrimary.isBlank() && !this.inputSecondary.isBlank()) {
                        SessionAccount account = new SessionAccount(this.inputPrimary.trim(), this.inputSecondary.trim());
                        if (account.fetchInfo()) {
                            AccountsManager.add(account);
                            this.statusMessage = "Added " + account.getUsername();
                        } else {
                            this.statusMessage = "Invalid session token";
                        }
                    }
                    this.resetAddState();
                    return true;
                }
                int inputR = btnX - INPUT_GAP;
                if (click.x() >= PAD && click.x() < inputR) {
                    this.focusPrimary = click.y() < rowY + 30;
                    return true;
                }
                y += SESSION_INPUT_ROW_H;
            }
        }

        if (!this.statusMessage.isEmpty()) y += 20;
        if (accounts.isEmpty() && this.addMode == AddMode.NONE) y += 32;

        for (Account account : accounts) {
            if (click.y() >= y && click.y() < y + ROW_H) {
                int removeX = innerR - REMOVE_INSET;
                int actionRight = removeX - ACTION_GAP;
                int actionX = actionRight - ACTION_BTN_W;
                int actionY = y + (ROW_H - 24) / 2;
                if (click.x() >= innerR - 28) {
                    AccountsManager.remove(account);
                    this.statusMessage = "Removed account";
                    return true;
                }
                if (!AccountsManager.isActive(account)
                        && click.x() >= actionX && click.x() < actionX + ACTION_BTN_W
                        && click.y() >= actionY && click.y() < actionY + 24) {
                    this.tryLogin(account);
                    return true;
                }
                return true;
            }
            y += ROW_H;
        }

        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        if (this.addMode != AddMode.CRACKED && this.addMode != AddMode.SESSION) return false;

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.resetAddState();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.focusPrimary && !this.inputPrimary.isEmpty()) {
                this.inputPrimary = this.inputPrimary.substring(0, this.inputPrimary.length() - 1);
            } else if (!this.focusPrimary && !this.inputSecondary.isEmpty()) {
                this.inputSecondary = this.inputSecondary.substring(0, this.inputSecondary.length() - 1);
            }
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER) {
            if (this.addMode == AddMode.CRACKED && !this.inputPrimary.isBlank()) {
                AccountsManager.add(new CrackedAccount(this.inputPrimary.trim()));
                this.statusMessage = "Added " + this.inputPrimary.trim();
                this.resetAddState();
            } else if (this.addMode == AddMode.SESSION && !this.inputPrimary.isBlank() && !this.inputSecondary.isBlank()) {
                SessionAccount account = new SessionAccount(this.inputPrimary.trim(), this.inputSecondary.trim());
                if (account.fetchInfo()) {
                    AccountsManager.add(account);
                    this.statusMessage = "Added " + account.getUsername();
                } else {
                    this.statusMessage = "Invalid session token";
                }
                this.resetAddState();
            }
            return true;
        }
        if (this.addMode == AddMode.SESSION && input.key() == GLFW.GLFW_KEY_TAB) {
            this.focusPrimary = !this.focusPrimary;
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
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
