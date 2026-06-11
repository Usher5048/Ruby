package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.RubyClient;
import ruby.systems.config.ConfigManager;
import ruby.systems.config.ProfileManager;
import ruby.systems.gui.GUIStyle;

import java.util.List;

public class ProfilesPanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 36;
    private static final int ADD_ROW_H = 40;
    private static final int BTN_H = 24;
    private static final int BTN_SECTION_TOP = 8;
    private static final int BTN_GAP = 4;
    private static final int BTN_SECTION_BOTTOM = 8;
    private static final int ADD_BTN_W = 52;
    private static final int SHARE_BTN_W = 44;
    private static final int INPUT_GAP = 6;
    private static final int INPUT_MAX_LEN = 8192;

    private boolean showCreateInput = false;
    private String inputText = "";
    private int cursorBlink = 0;
    private float savedFlash = 0f;
    private float copiedFlash = 0f;
    private String copiedProfile = null;
    private float contentAlpha = 1f;

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    public ProfilesPanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    private int buttonSectionHeight() {
        if (this.showCreateInput) return ADD_ROW_H + 1;
        return BTN_SECTION_TOP + BTN_H + BTN_GAP + BTN_H + BTN_SECTION_BOTTOM + 1;
    }

    @Override
    public int getHeight() {
        List<String> profiles = ProfileManager.getProfiles();
        int h = this.buttonSectionHeight() + profiles.size() * ROW_H;
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
        if (this.savedFlash > 0f) {
            this.savedFlash = Math.max(0f, this.savedFlash - dt * 0.15f);
        }
        if (this.copiedFlash > 0f) {
            this.copiedFlash = Math.max(0f, this.copiedFlash - dt * 0.15f);
        }
        this.drawContent(context, mouseX, mouseY);
    }

    private void drawContent(DrawContext context, int mouseX, int mouseY) {
        GUIStyle style = GUIStyle.get();
        int innerR = this.getWidth() - INNER_L;
        int y = 0;
        List<String> profiles = ProfileManager.getProfiles();
        String active = ProfileManager.getActiveProfile();

        if (this.showCreateInput) {
            y = this.drawCreateInput(context, style, innerR, y, mouseX, mouseY);
        } else {
            boolean saved = this.savedFlash > 0.01f;
            y = this.drawOutlineButton(context, style, innerR, BTN_SECTION_TOP, mouseX, mouseY, "+ New Profile", false);
            y = this.drawOutlineButton(context, style, innerR, y + BTN_GAP, mouseX, mouseY,
                    saved ? "Saved!" : "Save Profile", saved);
            y += BTN_SECTION_BOTTOM;
        }

        context.fill(INNER_L, y, innerR, y + 1, style.borderSubtle());
        y += 1;

        for (int i = 0; i < profiles.size(); i++) {
            String name = profiles.get(i);
            boolean isActive = name.equalsIgnoreCase(active);
            boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + ROW_H;
            boolean last = i == profiles.size() - 1;

            if (isActive) {
                int bg = GUIStyle.withAlpha(style.rubyActive(), this.contentAlpha);
                ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + ROW_H, bg, last);
            } else if (hovered) {
                int bg = GUIStyle.withAlpha(style.bgHover(), this.contentAlpha);
                ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + ROW_H, bg, last);
            }

            int dotY = y + (ROW_H - 6) / 2;
            ModuleTypeWindow.fillSmoothRoundedRect(context, PAD, dotY, PAD + 6, dotY + 6, 2,
                    isActive ? style.ruby() : 0xFF282426);

            int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
            int textRight = innerR - PAD;
            if (!"default".equalsIgnoreCase(name)) {
                textRight = innerR - 22 - 8 - SHARE_BTN_W;
            } else {
                textRight = innerR - PAD - SHARE_BTN_W - 8;
            }
            style.bodyFont().draw(context, name, 28, textY,
                    GUIStyle.withAlpha(isActive ? style.ruby() : style.text(), this.contentAlpha));

            int shareX = innerR - 22 - 8 - SHARE_BTN_W;
            if ("default".equalsIgnoreCase(name)) {
                shareX = innerR - PAD - SHARE_BTN_W;
            }
            int shareY = y + (ROW_H - BTN_H) / 2;
            boolean shareHovered = mouseX >= shareX && mouseX < shareX + SHARE_BTN_W
                    && mouseY >= shareY && mouseY < shareY + BTN_H;
            boolean shareCopied = this.copiedFlash > 0.01f && name.equals(this.copiedProfile);
            ModuleTypeWindow.drawRoundedBadge(context, shareX, shareY, shareX + SHARE_BTN_W, shareY + BTN_H,
                    shareHovered || shareCopied ? style.rubyBg() : 0x00000000, style.border());
            this.drawTextInRect(style.bodyFont(), context, shareCopied ? "Copied" : "Share",
                    shareX, shareY, shareX + SHARE_BTN_W, shareY + BTN_H,
                    GUIStyle.withAlpha(shareCopied ? style.ruby() : (shareHovered ? style.textBright() : style.ruby()),
                            this.contentAlpha));

            if (!"default".equalsIgnoreCase(name)) {
                boolean removeHovered = mouseX >= innerR - 28 && mouseX < innerR - 8
                        && mouseY >= y + 8 && mouseY < y + ROW_H - 8;
                style.bodyFont().draw(context, "\u2715", innerR - 22, textY,
                        GUIStyle.withAlpha(removeHovered ? 0xFFC25555 : style.textMuted(), this.contentAlpha));
            }

            if (!last) {
                context.fill(INNER_L, y + ROW_H - 1, innerR, y + ROW_H, style.borderSubtle());
            }

            y += ROW_H;
        }
    }

    private int drawOutlineButton(
            DrawContext context, GUIStyle style, int innerR, int by,
            int mx, int my, String label, boolean saved
    ) {
        int btnR = innerR - PAD;
        boolean hovered = mx >= PAD && mx < btnR && my >= by && my < by + BTN_H;
        int textColor = saved ? style.ruby() : (hovered ? style.textBright() : style.ruby());
        int bg = (hovered || saved) ? style.rubyBg() : 0x00000000;
        ModuleTypeWindow.drawRoundedBadge(context, PAD, by, btnR, by + BTN_H, bg, style.border());
        this.drawTextInRect(style.bodyFont(), context, label, PAD, by, btnR, by + BTN_H,
                GUIStyle.withAlpha(textColor, this.contentAlpha));
        return by + BTN_H;
    }

    private int drawCreateInput(DrawContext context, GUIStyle style, int innerR, int y, int mx, int my) {
        int rowY = y + 8;
        int inputH = 24;
        int contentR = innerR - PAD;
        int btnX = contentR - ADD_BTN_W;
        int inputR = btnX - INPUT_GAP;

        ModuleTypeWindow.drawRoundedBadge(context, PAD, rowY, inputR, rowY + inputH, style.bgBase(), style.border());

        String placeholder = "Name or share code...";
        String display = this.inputText.isEmpty() ? placeholder : this.truncateInputDisplay(this.inputText);
        int col = this.inputText.isEmpty() ? style.textMuted() : style.text();
        int textY = rowY + (inputH - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, display, PAD + 8, textY, GUIStyle.withAlpha(col, this.contentAlpha));

        if (this.cursorBlink < 10) {
            int cx = PAD + 8 + style.bodyFont().getWidth(this.inputText.isEmpty() ? "" : this.truncateInputDisplay(this.inputText));
            context.fill(cx, textY, cx + 1, textY + style.bodyFont().fontHeight,
                    GUIStyle.withAlpha(style.text(), this.contentAlpha));
        }

        boolean addHovered = mx >= btnX && mx < contentR && my >= rowY && my < rowY + inputH;
        ModuleTypeWindow.fillSmoothRoundedRect(context, btnX, rowY, contentR, rowY + inputH, GUIStyle.RADIUS_BADGE,
                GUIStyle.withAlpha(addHovered ? style.rubyHover() : style.ruby(), this.contentAlpha));
        this.drawTextInRect(style.bodyFont(), context, "Add", btnX, rowY, contentR, rowY + inputH, 0xFFE8E4E5);

        return y + ADD_ROW_H;
    }

    private String truncateInputDisplay(String text) {
        if (text.length() <= 28) return text;
        return text.substring(0, 12) + "..." + text.substring(text.length() - 10);
    }

    private void submitInput() {
        if (this.inputText.isBlank()) return;

        if (ProfileManager.addProfile(this.inputText)) {
            ConfigManager.setActiveProfile(ProfileManager.getActiveProfile());
            ConfigManager.saveState();
        }

        this.inputText = "";
        this.showCreateInput = false;
    }

    private boolean isControlDown() {
        long handle = RubyClient.client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        int innerR = this.getWidth() - INNER_L;
        List<String> profiles = ProfileManager.getProfiles();
        int contentR = innerR - PAD;

        if (this.showCreateInput) {
            int rowY = 8;
            int btnX = contentR - ADD_BTN_W;
            if (click.x() >= btnX && click.x() < contentR && click.y() >= rowY && click.y() < rowY + 24) {
                this.submitInput();
                return true;
            }
            int inputR = btnX - INPUT_GAP;
            if (click.x() >= PAD && click.x() < inputR && click.y() >= rowY && click.y() < rowY + 24) {
                return true;
            }
        } else {
            int newBtnY = BTN_SECTION_TOP;
            int saveBtnY = BTN_SECTION_TOP + BTN_H + BTN_GAP;
            if (click.x() >= PAD && click.x() < contentR && click.y() >= newBtnY && click.y() < newBtnY + BTN_H) {
                this.showCreateInput = true;
                this.inputText = "";
                return true;
            }
            if (click.x() >= PAD && click.x() < contentR && click.y() >= saveBtnY && click.y() < saveBtnY + BTN_H) {
                ProfileManager.saveProfile(ProfileManager.getActiveProfile());
                ConfigManager.saveState();
                this.savedFlash = 1f;
                return true;
            }
        }

        int y = this.buttonSectionHeight();
        for (String name : profiles) {
            if (click.y() >= y && click.y() < y + ROW_H) {
                int shareX = innerR - 22 - 8 - SHARE_BTN_W;
                if ("default".equalsIgnoreCase(name)) {
                    shareX = innerR - PAD - SHARE_BTN_W;
                }
                int shareY = y + (ROW_H - BTN_H) / 2;

                if (click.x() >= shareX && click.x() < shareX + SHARE_BTN_W
                        && click.y() >= shareY && click.y() < shareY + BTN_H) {
                    String code = ProfileManager.exportShareCode(name);
                    if (code != null) {
                        RubyClient.client.keyboard.setClipboard(code);
                        this.copiedProfile = name;
                        this.copiedFlash = 1f;
                    }
                    return true;
                }

                if (!"default".equalsIgnoreCase(name) && click.x() >= innerR - 28) {
                    ProfileManager.deleteProfile(name);
                    ConfigManager.saveState();
                } else if (click.x() < shareX) {
                    ProfileManager.switchProfile(name);
                    ConfigManager.setActiveProfile(name);
                    ConfigManager.saveState();
                }
                return true;
            }
            y += ROW_H;
        }

        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        if (!this.showCreateInput) return false;

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.showCreateInput = false;
            this.inputText = "";
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !this.inputText.isEmpty()) {
            this.inputText = this.inputText.substring(0, this.inputText.length() - 1);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_V && this.isControlDown()) {
            String clip = RubyClient.client.keyboard.getClipboard();
            if (clip != null && !clip.isBlank()) {
                this.appendInput(clip.trim());
            }
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER) {
            this.submitInput();
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        if (!this.showCreateInput) return false;
        char c = (char) input.codepoint();
        if (c < 32 || c >= 127) return false;
        this.appendInput(String.valueOf(c));
        return true;
    }

    private void appendInput(String text) {
        int remaining = INPUT_MAX_LEN - this.inputText.length();
        if (remaining <= 0) return;
        if (text.length() > remaining) {
            this.inputText += text.substring(0, remaining);
        } else {
            this.inputText += text;
        }
    }
}
