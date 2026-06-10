package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.ConfigManager;
import ruby.systems.gui.GUIStyle;
import ruby.systems.social.FriendsManager;
import ruby.systems.social.PlayerHeadCache;

import java.util.List;

public class FriendsPanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 40;
    private static final int ADD_BTN_ROW_H = 44;
    private static final int ADD_ROW_H = 40;
    private static final int HEAD_SIZE = 24;
    private static final int CREATE_BTN_W = 52;
    private static final int INPUT_GAP = 6;

    private boolean showAddInput = false;
    private String inputText = "";
    private int cursorBlink = 0;
    private float contentAlpha = 1f;

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    public FriendsPanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    @Override
    public int getHeight() {
        List<String> friends = FriendsManager.getFriends();
        int h = this.showAddInput ? ADD_ROW_H : ADD_BTN_ROW_H;
        h += friends.size() * ROW_H;
        if (friends.isEmpty() && !this.showAddInput) h += 32;
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
        List<String> friends = FriendsManager.getFriends();

        if (this.showAddInput) {
            y = this.drawAddInput(context, style, w, innerR, y, mouseX, mouseY);
        } else {
            y = this.drawAddButton(context, style, w, innerR, y, mouseX, mouseY);
        }

        if (friends.isEmpty() && !this.showAddInput) {
            int emptyTop = y;
            int emptyBottom = y + 32;
            ModuleTypeWindow.fillBottomRoundedRect(context, INNER_L, emptyTop, innerR, emptyBottom, GUIStyle.RADIUS_ROW,
                    GUIStyle.withAlpha(style.bgPanel(), this.contentAlpha));
            int ty = y + 8;
            style.bodyFont().draw(context, "No friends added", PAD, ty,
                    GUIStyle.withAlpha(style.textMuted(), this.contentAlpha));
            y += 32;
        }

        for (int i = 0; i < friends.size(); i++) {
            String name = friends.get(i);
            boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + ROW_H;
            boolean last = i == friends.size() - 1;

            if (hovered) {
                int hover = GUIStyle.withAlpha(style.bgHover(), this.contentAlpha);
                ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + ROW_H, hover, last);
            }

            this.drawPlayerHead(context, style, name, PAD, y + (ROW_H - HEAD_SIZE) / 2);
            int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
            style.bodyFont().draw(context, name, PAD + HEAD_SIZE + 10, textY,
                    GUIStyle.withAlpha(style.text(), this.contentAlpha));

            boolean removeHovered = mouseX >= innerR - 28 && mouseX < innerR - 8
                    && mouseY >= y + 8 && mouseY < y + ROW_H - 8;
            style.bodyFont().draw(context, "\u2715", innerR - 22, textY,
                    GUIStyle.withAlpha(removeHovered ? 0xFFC25555 : style.textMuted(), this.contentAlpha));

            if (!last) {
                context.fill(INNER_L, y + ROW_H - 1, innerR, y + ROW_H, style.borderSubtle());
            }

            y += ROW_H;
        }
    }

    private int drawAddButton(DrawContext context, GUIStyle style, int w, int innerR, int y, int mx, int my) {
        int btnR = innerR - PAD;
        int by = y + 10;
        int bh = 24;
        boolean hovered = mx >= PAD && mx < btnR && my >= by && my < by + bh;
        int color = hovered ? style.textBright() : style.ruby();
        ModuleTypeWindow.drawRoundedBadge(context, PAD, by, btnR, by + bh,
                hovered ? style.rubyBg() : 0x00000000, style.border());
        this.drawTextInRect(style.bodyFont(), context, "+ Add Friend", PAD, by, btnR, by + bh,
                GUIStyle.withAlpha(color, this.contentAlpha));
        context.fill(INNER_L, y + ADD_BTN_ROW_H - 1, innerR, y + ADD_BTN_ROW_H, style.borderSubtle());
        return y + ADD_BTN_ROW_H;
    }

    private int drawAddInput(DrawContext context, GUIStyle style, int w, int innerR, int y, int mx, int my) {
        int rowY = y + 8;
        int inputH = 24;
        int contentR = innerR - PAD;
        int btnX = contentR - CREATE_BTN_W;
        int inputR = btnX - INPUT_GAP;

        ModuleTypeWindow.drawRoundedBadge(context, PAD, rowY, inputR, rowY + inputH, style.bgBase(), style.border());

        String display = this.inputText.isEmpty() ? "Username..." : this.inputText;
        int col = this.inputText.isEmpty() ? style.textMuted() : style.text();
        int textY = rowY + (inputH - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, display, PAD + 8, textY, GUIStyle.withAlpha(col, this.contentAlpha));

        if (this.cursorBlink < 10) {
            int cx = PAD + 8 + style.bodyFont().getWidth(this.inputText);
            context.fill(cx, textY, cx + 1, textY + style.bodyFont().fontHeight,
                    GUIStyle.withAlpha(style.text(), this.contentAlpha));
        }

        boolean addHovered = mx >= btnX && mx < contentR && my >= rowY && my < rowY + inputH;
        ModuleTypeWindow.fillSmoothRoundedRect(context, btnX, rowY, contentR, rowY + inputH, GUIStyle.RADIUS_BADGE,
                GUIStyle.withAlpha(addHovered ? style.rubyHover() : style.ruby(), this.contentAlpha));
        this.drawTextInRect(style.bodyFont(), context, "Add", btnX, rowY, contentR, rowY + inputH, 0xFFE8E4E5);

        context.fill(INNER_L, y + ADD_ROW_H - 1, innerR, y + ADD_ROW_H, style.borderSubtle());
        return y + ADD_ROW_H;
    }

    private void drawPlayerHead(DrawContext context, GUIStyle style, String username, int x, int y) {
        if (PlayerHeadCache.getSkin(username) != null) {
            PlayerHeadCache.drawHead(context, username, x, y, HEAD_SIZE);
        } else {
            ModuleTypeWindow.fillSmoothRoundedRect(context, x, y, x + HEAD_SIZE, y + HEAD_SIZE, GUIStyle.RADIUS_BADGE,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha));
        }
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        int w = this.getWidth();
        int innerR = w - INNER_L;
        int y = 0;
        List<String> friends = FriendsManager.getFriends();

        int contentR = innerR - PAD;

        if (this.showAddInput) {
            int rowY = y + 8;
            int btnX = contentR - CREATE_BTN_W;
            if (click.x() >= btnX && click.x() < contentR && click.y() >= rowY && click.y() < rowY + 24) {
                if (FriendsManager.addFriend(this.inputText)) {
                    ConfigManager.saveState();
                }
                this.inputText = "";
                this.showAddInput = false;
                return true;
            }
            int inputR = btnX - INPUT_GAP;
            if (click.x() >= PAD && click.x() < inputR && click.y() >= rowY && click.y() < rowY + 24) {
                return true;
            }
            y += ADD_ROW_H;
        } else {
            if (click.x() >= PAD && click.x() < contentR && click.y() >= y + 10 && click.y() < y + 34) {
                this.showAddInput = true;
                this.inputText = "";
                return true;
            }
            y += ADD_BTN_ROW_H;
        }

        if (friends.isEmpty() && !this.showAddInput) y += 32;

        for (int i = 0; i < friends.size(); i++) {
            if (click.y() >= y && click.y() < y + ROW_H) {
                FriendsManager.removeFriend(i);
                ConfigManager.saveState();
                return true;
            }
            y += ROW_H;
        }

        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        if (!this.showAddInput) return false;

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.showAddInput = false;
            this.inputText = "";
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !this.inputText.isEmpty()) {
            this.inputText = this.inputText.substring(0, this.inputText.length() - 1);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER) {
            if (FriendsManager.addFriend(this.inputText)) {
                ConfigManager.saveState();
            }
            this.inputText = "";
            this.showAddInput = false;
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        if (!this.showAddInput) return false;
        char c = (char) input.codepoint();
        if (c >= 32 && c < 127 && this.inputText.length() < 16) {
            this.inputText += c;
            return true;
        }
        return false;
    }
}
