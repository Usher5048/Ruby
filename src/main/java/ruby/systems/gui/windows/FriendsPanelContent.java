package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;
import ruby.systems.gui.Toasts;
import ruby.systems.gui.icons.Lucide;
import ruby.systems.social.FriendsManager;
import ruby.systems.social.PlayerHeadCache;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class FriendsPanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 31;
    private static final int LAST_EXTRA = 4;
    private static final int HEAD_SIZE = 16;

    private boolean showAddInput = false;
    private String inputText = "";
    private int cursorBlink = 0;
    private float contentAlpha = 1f;
    private float addMix;
    private float[] rowMix;

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    public FriendsPanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    private int bodyRows(int friendCount) {
        int rows = 1;
        if (this.showAddInput) rows++;
        else if (friendCount == 0) rows++;
        return rows + friendCount;
    }

    @Override
    public int getHeight() {
        int h = this.bodyRows(FriendsManager.getFriends().size()) * ROW_H + LAST_EXTRA;
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
        List<String> friends = FriendsManager.getFriends();
        int rows = this.bodyRows(friends.size());
        int y = 0;
        int row = 0;

        y = this.drawAddRow(context, style, innerR, y, mouseX, mouseY, row == rows - 1, seconds);
        row++;

        if (this.showAddInput) {
            y = this.drawInputRow(context, style, innerR, y, mouseX, mouseY, row == rows - 1, seconds);
            row++;
        } else if (friends.isEmpty()) {
            y = this.drawEmptyRow(context, style, innerR, y, row == rows - 1);
            row++;
        }

        for (int i = 0; i < friends.size(); i++) {
            y = this.drawFriend(context, style, innerR, y, mouseX, mouseY, friends.get(i), i,
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
        float target = hovered || this.showAddInput ? 1f : 0f;
        this.addMix = Motion.follow(this.addMix, target, seconds, Motion.HOVER);
        if (this.addMix > 0.01f) {
            int bg = GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * this.addMix);
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH, bg, last);
        }

        int iconColor = Motion.mix(style.textMuted(), style.ruby(), this.addMix);
        int textColor = Motion.mix(style.text(), style.textBright(), this.addMix);
        int iconY = y + (ROW_H - Lucide.UI) / 2;
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        Press.icon(context, "plus", PAD, iconY, GUIStyle.withAlpha(iconColor, this.contentAlpha), "friends-new");
        Press.label(style.bodyFont(), context, "Add friend", PAD + Lucide.UI + 8, textY,
                GUIStyle.withAlpha(textColor, this.contentAlpha), "friends-new");

        if (!last) {
            context.fill(INNER_L, y + rowH - 1, innerR, y + rowH,
                    GUIStyle.withAlpha(style.borderSubtle(), this.contentAlpha));
        }
        return y + rowH;
    }

    private int drawInputRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                             int mouseX, int mouseY, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        context.fill(INNER_L, y, innerR, y + ROW_H, GUIStyle.withAlpha(style.bgBase(), this.contentAlpha));

        String display = this.inputText.isEmpty() ? "Username" : this.inputText;
        int col = this.inputText.isEmpty() ? style.textMuted() : style.text();
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, display, PAD, textY, GUIStyle.withAlpha(col, this.contentAlpha));

        if (this.cursorBlink < 10) {
            int cx = PAD + style.bodyFont().getWidth(this.inputText);
            context.fill(cx, textY, cx + 1, textY + style.bodyFont().fontHeight,
                    GUIStyle.withAlpha(style.text(), this.contentAlpha));
        }

        String action = "Add";
        int actionW = style.bodyFont().getWidth(action);
        int actionX = innerR - PAD - actionW;
        boolean actionHovered = mouseX >= actionX - 6 && mouseX < innerR - 6
                && mouseY >= y && mouseY < y + ROW_H;
        Press.label(style.bodyFont(), context, action, actionX, textY,
                GUIStyle.withAlpha(actionHovered ? style.rubyHover() : style.ruby(), this.contentAlpha), "friends-add");

        if (!last) {
            context.fill(INNER_L, y + rowH - 1, innerR, y + rowH,
                    GUIStyle.withAlpha(style.borderSubtle(), this.contentAlpha));
        }
        return y + rowH;
    }

    private int drawEmptyRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y, boolean last) {
        int rowH = this.rowHeight(last);
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, "No friends yet", PAD, textY,
                GUIStyle.withAlpha(style.textMuted(), this.contentAlpha));
        if (!last) {
            context.fill(INNER_L, y + rowH - 1, innerR, y + rowH,
                    GUIStyle.withAlpha(style.borderSubtle(), this.contentAlpha));
        }
        return y + rowH;
    }

    private int drawFriend(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                           int mouseX, int mouseY, String name, int index, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        float mix = this.rowMix(index, hovered ? 1f : 0f, seconds);
        if (mix > 0.01f) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * mix), last);
        }

        int headY = y + (ROW_H - HEAD_SIZE) / 2;
        this.drawPlayerHead(context, style, name, PAD, headY);
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, name, PAD + HEAD_SIZE + 8, textY,
                GUIStyle.withAlpha(style.text(), this.contentAlpha));

        int iconX = innerR - PAD - Lucide.UI;
        int iconY = y + (ROW_H - Lucide.UI) / 2;
        boolean removeHovered = mouseX >= iconX - 6 && mouseX < innerR - 6
                && mouseY >= y && mouseY < y + ROW_H;
        Press.icon(context, "x", iconX, iconY,
                GUIStyle.withAlpha(removeHovered ? 0xFFC25555 : style.textMuted(), this.contentAlpha), "friends-x:" + name);

        if (!last) {
            context.fill(INNER_L, y + rowH - 1, innerR, y + rowH,
                    GUIStyle.withAlpha(style.borderSubtle(), this.contentAlpha));
        }
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

    private void drawPlayerHead(GuiGraphicsExtractor context, GUIStyle style, String username, int x, int y) {
        if (PlayerHeadCache.getSkin(username) != null) {
            PlayerHeadCache.drawHead(context, username, x, y, HEAD_SIZE);
        } else {
            ModuleTypeWindow.fillSmoothRoundedRect(context, x, y, x + HEAD_SIZE, y + HEAD_SIZE, GUIStyle.RADIUS_BADGE,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha));
        }
    }

    private void submitFriend() {
        String name = this.inputText.trim();
        if (FriendsManager.addFriend(this.inputText)) {
            ConfigManager.saveState();
            Toasts.show("plus", "Added " + name);
        }
        this.inputText = "";
        this.showAddInput = false;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        int innerR = this.getWidth() - INNER_L;
        List<String> friends = FriendsManager.getFriends();
        int rows = this.bodyRows(friends.size());
        int y = 0;
        int row = 0;

        int addH = this.rowHeight(row == rows - 1);
        if (click.y() >= y && click.y() < y + addH) {
            Press.down("friends-new");
            this.showAddInput = !this.showAddInput;
            if (this.showAddInput) this.inputText = "";
            return true;
        }
        y += addH;
        row++;

        if (this.showAddInput) {
            int inputH = this.rowHeight(row == rows - 1);
            if (click.y() >= y && click.y() < y + inputH) {
                String action = "Add";
                int actionX = innerR - PAD - GUIStyle.get().bodyFont().getWidth(action);
                if (click.x() >= actionX - 6) {
                    Press.down("friends-add");
                    this.submitFriend();
                }
                return true;
            }
            y += inputH;
            row++;
        } else if (friends.isEmpty()) {
            y += this.rowHeight(row == rows - 1);
            row++;
        }

        for (int i = 0; i < friends.size(); i++) {
            int rowH = this.rowHeight(row == rows - 1);
            if (click.y() >= y && click.y() < y + rowH) {
                int iconX = innerR - PAD - Lucide.UI;
                if (click.x() >= iconX - 6) {
                    String name = friends.get(i);
                    Press.down("friends-x:" + name);
                    FriendsManager.removeFriend(i);
                    ConfigManager.saveState();
                    Toasts.show("x", "Removed " + name);
                }
                return true;
            }
            y += rowH;
            row++;
        }

        return false;
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        if (!this.showAddInput) return false;

        if (input.key() == InputConstants.KEY_ESCAPE) {
            this.showAddInput = false;
            this.inputText = "";
            return true;
        }
        if (input.key() == InputConstants.KEY_BACKSPACE && !this.inputText.isEmpty()) {
            this.inputText = this.inputText.substring(0, this.inputText.length() - 1);
            return true;
        }
        if (input.key() == InputConstants.KEY_RETURN) {
            this.submitFriend();
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        if (!this.showAddInput) return false;
        char c = (char) input.codepoint();
        if (c >= 32 && c < 127 && this.inputText.length() < 16) {
            this.inputText += c;
            return true;
        }
        return false;
    }
}
