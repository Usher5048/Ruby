package ruby.systems.gui.windows;

import com.mojang.blaze3d.platform.InputConstants;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;
import ruby.systems.config.ConfigManager;
import ruby.systems.config.ProfileManager;
import ruby.systems.gui.GUIStyle;
import ruby.systems.gui.GuiTime;
import ruby.systems.gui.Motion;
import ruby.systems.gui.Press;
import ruby.systems.gui.SystemClipboard;
import ruby.systems.gui.Toasts;
import ruby.systems.gui.icons.Lucide;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class ProfilesPanelContent extends Window {
    private static final int PAD = 14;
    private static final int INNER_L = 1;
    private static final int ROW_H = 31;
    private static final int LAST_EXTRA = 4;
    private static final int INPUT_MAX_LEN = 8192;
    private static final float HINT_DELAY = 0.3f;

    private boolean showCreateInput = false;
    private String inputText = "";
    private int cursorBlink = 0;
    private float contentAlpha = 1f;
    private float newMix;
    private float[] rowMix;
    private String clipboardRaw = "";
    private String clipboardName;
    private String hintKey;
    private String hint;
    private float hintTime;
    private int hintX;
    private int hintY;
    private boolean hintOffered;

    public void setContentAlpha(float contentAlpha) {
        this.contentAlpha = contentAlpha;
    }

    public ProfilesPanelContent(int x, int y, int width) {
        super(x, y, width, 100);
        this.draggableBounds = new int[] {0, 0, 0, 0};
        this.handleChildren = false;
    }

    /** Reads the clipboard once, when Profiles is opened. Not safe to call every frame. */
    public void refreshClipboard() {
        try {
            String clip = RubyClient.client.keyboardHandler.getClipboard();
            clip = clip.trim();
            this.clipboardRaw = clip;
            this.clipboardName = ProfileManager.peekName(clip);
        } catch (RuntimeException ignored) {
            this.clipboardRaw = "";
            this.clipboardName = null;
        }
    }

    private boolean clipboardSaved() {
        return this.clipboardName != null && this.isSaved(this.clipboardName);
    }

    private boolean showClipboardRow() {
        return this.clipboardName != null && !this.clipboardName.isBlank() && !this.clipboardSaved();
    }

    private boolean isSaved(String name) {
        for (String profile : ProfileManager.profiles()) {
            if (profile.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private int bodyRows(int profileCount) {
        int rows = 1 + profileCount;
        if (this.showCreateInput) rows++;
        if (this.showClipboardRow()) rows++;
        return rows;
    }

    @Override
    public int getHeight() {
        int h = this.bodyRows(ProfileManager.profiles().size()) * ROW_H + LAST_EXTRA;
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
        this.hintOffered = false;
        GUIStyle style = GUIStyle.get();
        int innerR = this.getWidth() - INNER_L;
        List<String> profiles = ProfileManager.profiles();
        int rows = this.bodyRows(profiles.size());
        int y = 0;
        int row = 0;

        y = this.drawNewRow(context, style, innerR, y, mouseX, mouseY, row == rows - 1, seconds);
        row++;

        if (this.showCreateInput) {
            y = this.drawInputRow(context, style, innerR, y, mouseX, mouseY, row == rows - 1, seconds);
            row++;
        }

        if (this.showClipboardRow()) {
            y = this.drawClipboardRow(context, style, innerR, y, mouseX, mouseY, row == rows - 1, seconds);
            row++;
        }

        String active = ProfileManager.activeProfile();
        for (int i = 0; i < profiles.size(); i++) {
            String name = profiles.get(i);
            y = this.drawProfile(context, style, innerR, y, mouseX, mouseY, name,
                    name.equalsIgnoreCase(active), i, row == rows - 1, seconds);
            row++;
        }
        if (!this.hintOffered) {
            this.hintKey = null;
            this.hint = null;
            this.hintTime = 0f;
        }
    }

    private void offerHint(String key, String text, boolean hot, int mouseX, int mouseY, float seconds) {
        if (!hot) return;
        this.hintOffered = true;
        if (!key.equals(this.hintKey)) {
            this.hintKey = key;
            this.hintTime = 0f;
        }
        this.hintTime += Math.min(seconds, 0.05f);
        this.hint = text;
        this.hintX = mouseX;
        this.hintY = mouseY;
    }

    /** Drawn by the module panel after its scissor, so the hint can follow the cursor. */
    public void drawHint(GuiGraphicsExtractor context) {
        if (this.hint == null || this.hintTime < HINT_DELAY) return;
        GUIStyle style = GUIStyle.get();
        float show = Math.clamp((this.hintTime - HINT_DELAY) / 0.08f, 0f, 1f) * this.contentAlpha;
        if (show <= 0.01f) return;

        var font = style.labelFont();
        int padX = 6;
        int padY = 3;
        int textW = font.getWidth(this.hint);
        int boxW = textW + padX * 2;
        int boxH = font.fontHeight + padY * 2;
        int cursorX = this.getX() + this.hintX;
        int cursorY = this.getY() + this.hintY;
        int x = cursorX - boxW / 2;
        int y = cursorY - boxH - 8 - Math.round((1f - show) * 4f);
        int minX = 4;
        int maxX = this.getX() + this.getWidth() - boxW - 4;
        if (x < minX) x = minX;
        if (x > maxX) x = Math.max(minX, maxX);
        if (y < 2) y = cursorY + 14;

        int bg = GUIStyle.withAlpha(style.bgElevated(), show);
        int edge = GUIStyle.withAlpha(style.borderPanel(), show);
        ModuleTypeWindow.drawRoundedPanel(context, x, y, x + boxW, y + boxH, GUIStyle.RADIUS_BADGE, bg, edge);
        font.draw(context, this.hint, x + padX, y + padY, GUIStyle.withAlpha(style.text(), show));
    }

    private int rowHeight(boolean last) {
        return last ? ROW_H + LAST_EXTRA : ROW_H;
    }

    private void drawSeparator(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y, int rowH) {
        context.fill(INNER_L, y + rowH - 1, innerR, y + rowH,
                GUIStyle.withAlpha(style.borderSubtle(), this.contentAlpha));
    }

    private int drawNewRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                           int mouseX, int mouseY, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        float target = hovered || this.showCreateInput ? 1f : 0f;
        this.newMix = Motion.follow(this.newMix, target, seconds, Motion.HOVER);
        if (this.newMix > 0.01f) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * this.newMix), last);
        }

        int iconColor = Motion.mix(style.textMuted(), style.ruby(), this.newMix);
        int textColor = Motion.mix(style.text(), style.textBright(), this.newMix);
        int iconY = y + (ROW_H - Lucide.UI) / 2;
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        boolean plusHovered = mouseX >= PAD - 2 && mouseX < PAD + Lucide.UI + 4
                && mouseY >= y && mouseY < y + rowH;
        this.offerHint("new", "New profile", plusHovered, mouseX, mouseY, seconds);
        int plusColor = GUIStyle.withAlpha(iconColor, this.contentAlpha);
        int labelColor = GUIStyle.withAlpha(textColor, this.contentAlpha);
        Press.icon(context, "plus", PAD, iconY, plusColor, "profiles-new");
        Press.label(style.bodyFont(), context, "New profile", PAD + Lucide.UI + 8, textY, labelColor, "profiles-new");
        if (!last) this.drawSeparator(context, style, innerR, y, rowH);
        return y + rowH;
    }

    private int drawInputRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                             int mouseX, int mouseY, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        context.fill(INNER_L, y, innerR, y + ROW_H, GUIStyle.withAlpha(style.bgBase(), this.contentAlpha));

        String shown = this.inputText.isEmpty() ? "Name or code" : this.truncateInputDisplay(this.inputText);
        int col = this.inputText.isEmpty() ? style.textMuted() : style.text();
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        style.bodyFont().draw(context, shown, PAD, textY, GUIStyle.withAlpha(col, this.contentAlpha));

        if (this.cursorBlink < 10) {
            String measured = this.inputText.isEmpty() ? "" : this.truncateInputDisplay(this.inputText);
            int cx = PAD + style.bodyFont().getWidth(measured);
            context.fill(cx, textY, cx + 1, textY + style.bodyFont().fontHeight,
                    GUIStyle.withAlpha(style.text(), this.contentAlpha));
        }

        String action = "Add";
        int actionX = innerR - PAD - style.bodyFont().getWidth(action);
        boolean actionHovered = mouseX >= actionX - 6 && mouseX < innerR - 6
                && mouseY >= y && mouseY < y + ROW_H;
        this.offerHint("add", "Add", actionHovered, mouseX, mouseY, seconds);
        Press.label(style.bodyFont(), context, action, actionX, textY,
                GUIStyle.withAlpha(actionHovered ? style.rubyHover() : style.ruby(), this.contentAlpha), "profiles-add");

        if (!last) this.drawSeparator(context, style, innerR, y, rowH);
        return y + rowH;
    }

    private int drawClipboardRow(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                                 int mouseX, int mouseY, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        float mix = this.rowMix(0, hovered ? 0.7f : 0f, seconds);
        if (mix > 0.01f) {
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(style.bgHover(), this.contentAlpha * mix), last);
        }

        int iconY = y + (ROW_H - Lucide.UI) / 2;
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        boolean clipHovered = mouseX >= PAD - 2 && mouseX < PAD + Lucide.UI + 4
                && mouseY >= y && mouseY < y + rowH;
        this.offerHint("clipboard", "Clipboard", clipHovered, mouseX, mouseY, seconds);
        Lucide.draw(context, "clipboard", PAD, iconY, Lucide.UI,
                GUIStyle.withAlpha(style.textMuted(), this.contentAlpha));
        style.bodyFont().draw(context, this.clipboardName, PAD + Lucide.UI + 8, textY,
                GUIStyle.withAlpha(style.text(), this.contentAlpha));

        int saveX = innerR - PAD - Lucide.UI;
        boolean saveHovered = mouseX >= saveX - 4 && mouseX < innerR - 6 && mouseY >= y && mouseY < y + rowH;
        this.offerHint("clipboard-save", "Save", saveHovered, mouseX, mouseY, seconds);
        Press.icon(context, "check", saveX, iconY,
                GUIStyle.withAlpha(saveHovered ? style.ruby() : style.textMuted(), this.contentAlpha), "profiles-clip-save");

        if (!last) this.drawSeparator(context, style, innerR, y, rowH);
        return y + rowH;
    }

    private int drawProfile(GuiGraphicsExtractor context, GUIStyle style, int innerR, int y,
                            int mouseX, int mouseY, String name, boolean active,
                            int index, boolean last, float seconds) {
        int rowH = this.rowHeight(last);
        boolean hovered = mouseX >= INNER_L && mouseY >= y && mouseX < innerR && mouseY < y + rowH;
        float mix = this.rowMix(index + 1, active || hovered ? 1f : 0f, seconds);
        if (mix > 0.01f) {
            int bg = active ? style.rubyBg() : style.bgHover();
            ModuleTypeWindow.fillRowBackground(context, INNER_L, y, innerR, y + rowH,
                    GUIStyle.withAlpha(bg, this.contentAlpha * mix), last);
        }

        int iconY = y + (ROW_H - Lucide.UI) / 2;
        int textY = y + (ROW_H - style.bodyFont().fontHeight) / 2;
        boolean copyHovered = mouseX >= PAD - 2 && mouseX < PAD + Lucide.UI + 4
                && mouseY >= y && mouseY < y + rowH;
        this.offerHint("copy:" + name, "Copy", copyHovered, mouseX, mouseY, seconds);
        Press.icon(context, "forward", PAD, iconY,
                GUIStyle.withAlpha(copyHovered ? style.ruby() : style.textMuted(), this.contentAlpha), "profiles-copy:" + name);
        Press.label(style.bodyFont(), context, name, PAD + Lucide.UI + 8, textY,
                GUIStyle.withAlpha(active ? style.ruby() : style.text(), this.contentAlpha), "profiles-load:" + name);

        int saveX = innerR - PAD - Lucide.UI;
        boolean saveHovered = mouseX >= saveX - 4 && mouseX < innerR - 6 && mouseY >= y && mouseY < y + rowH;
        this.offerHint("save:" + name, "Save", saveHovered, mouseX, mouseY, seconds);
        Press.icon(context, "check", saveX, iconY,
                GUIStyle.withAlpha(saveHovered ? style.ruby() : style.textMuted(), this.contentAlpha), "profiles-save:" + name);

        if (!"default".equalsIgnoreCase(name)) {
            int removeX = saveX - 8 - Lucide.UI;
            boolean removeHovered = mouseX >= removeX - 4 && mouseX < saveX - 4
                    && mouseY >= y && mouseY < y + rowH;
            this.offerHint("delete:" + name, "Delete", removeHovered, mouseX, mouseY, seconds);
            Press.icon(context, "x", removeX, iconY,
                    GUIStyle.withAlpha(removeHovered ? 0xFFC25555 : style.textMuted(), this.contentAlpha), "profiles-delete:" + name);
        }

        if (!last) this.drawSeparator(context, style, innerR, y, rowH);
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

    private String truncateInputDisplay(String text) {
        if (text.length() <= 22) return text;
        return text.substring(0, 10) + "..." + text.substring(text.length() - 8);
    }

    private void submitInput() {
        if (this.inputText.isBlank()) return;
        String text = this.inputText.trim();
        if (ProfileManager.importProfile(text.replaceAll("\\s", ""))) {
            String name = ProfileManager.peekName(text);
            Toasts.show("clipboard", name == null || name.isBlank() ? "Imported profile" : "Imported " + name);
        } else if (ProfileManager.createProfile(text)) {
            Toasts.show("plus", "Created " + text);
        }
        this.inputText = "";
        this.showCreateInput = false;
    }

    private void saveClipboard() {
        if (this.clipboardRaw == null || this.clipboardRaw.isBlank()) return;
        String name = this.clipboardName;
        if (ProfileManager.importProfile(this.clipboardRaw.replaceAll("\\s", ""))) {
            ConfigManager.saveState();
            Toasts.show("check", name == null || name.isBlank() ? "Saved profile" : "Saved " + name);
        }
    }

    private void saveProfile(String name) {
        ProfileManager.saveProfile(name);
        ConfigManager.saveState();
        Toasts.show("check", "Saved " + name);
    }

    private void copyProfile(String name) {
        String code = ProfileManager.exportProfile(name);
        if (code == null) return;
        this.clipboardRaw = code.trim();
        this.clipboardName = name;
        SystemClipboard.set(code);
        Toasts.show("forward", "Copied " + name);
    }

    private boolean isControlDown() {
        return InputUtils.isKeyPressed(InputConstants.KEY_LCONTROL)
                || InputUtils.isKeyPressed(InputConstants.KEY_RCONTROL);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        int innerR = this.getWidth() - INNER_L;
        List<String> profiles = ProfileManager.profiles();
        int rows = this.bodyRows(profiles.size());
        int y = 0;
        int row = 0;

        int newH = this.rowHeight(row == rows - 1);
        if (click.y() >= y && click.y() < y + newH) {
            Press.down("profiles-new");
            this.showCreateInput = !this.showCreateInput;
            if (this.showCreateInput) this.inputText = "";
            return true;
        }
        y += newH;
        row++;

        if (this.showCreateInput) {
            int inputH = this.rowHeight(row == rows - 1);
            if (click.y() >= y && click.y() < y + inputH) {
                int actionX = innerR - PAD - GUIStyle.get().bodyFont().getWidth("Add");
                if (click.x() >= actionX - 6) {
                    Press.down("profiles-add");
                    this.submitInput();
                }
                return true;
            }
            y += inputH;
            row++;
        }

        if (this.showClipboardRow()) {
            int clipH = this.rowHeight(row == rows - 1);
            if (click.y() >= y && click.y() < y + clipH) {
                int saveX = innerR - PAD - Lucide.UI;
                if (click.x() >= saveX - 4) {
                    Press.down("profiles-clip-save");
                    this.saveClipboard();
                }
                return true;
            }
            y += clipH;
            row++;
        }

        for (int i = 0; i < profiles.size(); i++) {
            int rowH = this.rowHeight(row == rows - 1);
            if (click.y() >= y && click.y() < y + rowH) {
                String name = profiles.get(i);
                boolean canDelete = !"default".equalsIgnoreCase(name);
                int saveX = innerR - PAD - Lucide.UI;
                int removeX = saveX - 8 - Lucide.UI;
                if (click.x() >= saveX - 4) {
                    Press.down("profiles-save:" + name);
                    this.saveProfile(name);
                } else if (canDelete && click.x() >= removeX - 4) {
                    Press.down("profiles-delete:" + name);
                    ProfileManager.deleteProfile(name);
                    ConfigManager.saveState();
                    Toasts.show("x", "Deleted " + name);
                } else if (click.x() < PAD + Lucide.UI + 4) {
                    Press.down("profiles-copy:" + name);
                    this.copyProfile(name);
                } else {
                    Press.down("profiles-load:" + name);
                    ProfileManager.setActiveProfile(name);
                    ConfigManager.saveState();
                    Toasts.show("chevron-right", "Loaded " + name);
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
        if (!this.showCreateInput) return false;

        if (input.key() == InputConstants.KEY_ESCAPE) {
            this.showCreateInput = false;
            this.inputText = "";
            return true;
        }
        if (input.key() == InputConstants.KEY_BACKSPACE && !this.inputText.isEmpty()) {
            this.inputText = this.inputText.substring(0, this.inputText.length() - 1);
            return true;
        }
        if (input.key() == InputConstants.KEY_V && this.isControlDown()) {
            String clip = RubyClient.client.keyboardHandler.getClipboard();
            if (!clip.isBlank()) this.appendInput(clip.trim());
            return true;
        }
        if (input.key() == InputConstants.KEY_RETURN) {
            this.submitInput();
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        if (!this.showCreateInput) return false;
        char c = (char) input.codepoint();
        if (c < 32 || c >= 127) return false;
        this.appendInput(String.valueOf(c));
        return true;
    }

    private void appendInput(String text) {
        int remaining = INPUT_MAX_LEN - this.inputText.length();
        if (remaining <= 0) return;
        if (text.length() > remaining) this.inputText += text.substring(0, remaining);
        else this.inputText += text;
    }
}
