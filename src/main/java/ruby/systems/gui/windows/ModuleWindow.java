package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import ruby.systems.config.*;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Keybind;
import ruby.systems.modules.Module;
import ruby.systems.modules.Modules;

import java.util.LinkedHashSet;

public class ModuleWindow extends CollapsibleWindow {

    private final Module module;
    private final BooleanValue moduleTOR;
    private final BooleanValue moduleToasts;

    private boolean waitingForBind;
    private boolean settingsOpen = false;
    private float expandProgress = 0f;
    private float hoverProgress = 0f;
    boolean animationUpdatedThisFrame = false;

    public ModuleWindow(int x, int y, Module module) {
        super(x, y, 240, 28);
        this.handleChildren = false;

        this.module = module;
        this.moduleTOR = (BooleanValue) new BooleanValue.Builder("Hold bind")
                .defaultValue(this.module.keybind.togglesOnRelease())
                .build();

        this.moduleToasts = (BooleanValue) new BooleanValue.Builder("Show toasts")
                .defaultValue(this.module.showsToasts())
                .build();

        // Create setting child windows from module config
        int settingY = this.getHeaderHeight() + 8;
        for (String key : module.config.getAll()) {
            Value<?> value = module.config.get(key);
            if (value instanceof BooleanValue bv) {
                this.addWindow(new SettingToggleWindow(0, settingY, 240, bv));
                settingY += 26;
            } else if (value instanceof DoubleValue dv) {
                this.addWindow(new SettingSliderWindow(0, settingY, 240, dv));
                settingY += 44;
            } else if (value instanceof IntegerValue iv) {
                this.addWindow(new SettingSliderWindow(0, settingY, 240, iv));
                settingY += 44;
            } else if (value instanceof EnumValue<?> ev) {
                this.addWindow(new SettingEnumWindow(0, settingY, 240, ev));
                settingY += 26;
            } else if (value instanceof ListValue<?> lv) {
                this.addWindow(new SettingListWindow(0, settingY, 240, lv));
                settingY += 26;
            }
        }

        this.addWindow(new SettingToggleWindow(0, settingY, 240, this.moduleTOR));
        settingY += 26;

        this.addWindow(new SettingToggleWindow(0, settingY, 240, this.moduleToasts));
    }

    private static final int SETTING_GAP = 2;

    public Module module() {
        return this.module;
    }

    private int getExpandedHeight() {
        int h = this.getHeaderHeight() + 8;
        for (Window child : this.windows()) {
            h += child.getHeight() + SETTING_GAP;
        }
        return h + 26 + 8;
    }

    @Override
    public void onTick() {
        if(this.module.showsToasts() != this.moduleToasts.value())
            this.module.showsToasts(this.moduleToasts.value());

        if(this.module.keybind.togglesOnRelease() != this.moduleTOR.value())
            this.module.keybind.togglesOnRelease(this.moduleTOR.value());
    }

    @Override
    public int getHeight() {
        if (this.expandProgress <= 0f) return this.getHeaderHeight();
        if (this.expandProgress >= 1f) return this.getExpandedHeight();
        int expanded = this.getExpandedHeight();
        float eased = easeInOutCubic(this.expandProgress);
        return this.getHeaderHeight() + (int) ((expanded - this.getHeaderHeight()) * eased);
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2 * t + 2, 3) / 2f;
    }

    /**
     * Pre-update expand/collapse animation and relayout settings.
     * Called by the parent ModuleTypeWindow BEFORE it draws its border,
     * so that getHeight() returns the current-frame value.
     */
    public void updateAnimation() {
        float target = this.settingsOpen ? 1f : 0f;
        this.expandProgress += (target - this.expandProgress) * 0.2f;
        if (Math.abs(this.expandProgress - target) < 0.005f) this.expandProgress = target;
        this.handleChildren = this.expandProgress > 0.99f;

        if(this.expandProgress > 0.01f) {
            int settingY = this.getHeaderHeight() + 8;
            for(Window child : this.windows()) {
                child.setPosition(0, settingY);
                settingY += child.getHeight() + SETTING_GAP;
            }
        }
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {
        int settingY = this.getHeaderHeight() + 8;

        LinkedHashSet<String> keys = new LinkedHashSet<>(this.module.config.getAll());
        keys.add(this.moduleTOR.name()); keys.add(this.moduleToasts.name());

        for(String key : keys) {
            Value<?> value = this.module.config.get(key);
            if(value == null) {
                if(this.moduleToasts.name().equals(key)) value = this.moduleToasts;
                if(this.moduleTOR.name().equals(key)) {
                    value = this.moduleTOR;
                    settingY += 26;
                }
            }

            for(Window child : this.windows()) {
                if(!(child instanceof SettingWindow sWin)) continue;
                if(sWin.value() != value) continue;

                child.setPosition(0, settingY);
                settingY += child.getHeight();

                break;
            }
        }

        // Animation & layout already handled by updateAnimation() called from parent.
        // Only re-run if it somehow wasn't called yet (direct rendering).
        if (!this.animationUpdatedThisFrame) {
            this.updateAnimation();
        }
        this.animationUpdatedThisFrame = false;

        // Animate hover
        float hoverTarget = (mouseX >= 0 && mouseY >= 0 && mouseX < this.getWidth() && mouseY < this.getHeaderHeight()) ? 1f : 0f;
        this.hoverProgress += (hoverTarget - this.hoverProgress) * 0.25f;
        if (Math.abs(this.hoverProgress - hoverTarget) < 0.01f) this.hoverProgress = hoverTarget;
        this.drawHeader(context);

        // Settings background when expanded/animating (inset 1px)
        if (this.expandProgress > 0.01f && !this.windows().isEmpty()) {
            context.fill(1, this.getHeaderHeight(), this.getWidth() - 1, this.getHeight(), 0x33000000);
        }
    }

    @Override
    public void drawHeader(DrawContext context) {
        GUIStyle style = GUIStyle.get();
        int w = this.getWidth();
        int hdr = this.getHeaderHeight();

        // Enabled module background highlight (inset 1px to stay inside parent border)
        if (this.module.enabled()) {
            context.fill(1, 0, w - 1, hdr, 0x22FFFFFF);
        }

        // Hover highlight (animated, inset 1px)
        if (this.hoverProgress > 0.01f) {
            int alpha = (int) (this.hoverProgress * 13);
            context.fill(1, 0, w - 1, hdr, (alpha << 24) | 0xFFFFFF);
        }

        // Small icon square — ruby accent when enabled, muted white when off
        int iconY = (hdr - 4) / 2;
        context.fill(14, iconY, 18, iconY + 4, this.module.enabled() ? 0xFFCC3344 : 0xBFFFFFFF);

        // Module name
        int textY = (hdr - (int) this.getTextHeight(style.bodyFont())) / 2;
        this.drawText(style.bodyFont(), context, this.module.name(), 28, textY, 0xFFFFFFFF);

        // Keybind badge
        String keyName = this.waitingForBind ? "..." : this.module.keybind.toString();
        int keyW = (int) this.getTextWidth(style.monospaceFont(), keyName);
        int keyH = (int) this.getTextHeight(style.monospaceFont());
        int badgeW = keyW + 12;
        int badgeH = keyH + 4;
        int badgeX = w - 14 - badgeW;
        int badgeY = (hdr - badgeH) / 2;

        // Badge background
        ModuleTypeWindow.fillSmoothRoundedRect(context, badgeX, badgeY,
                badgeX + badgeW, badgeY + badgeH, 4, 0x0DFFFFFF);

        // Badge text
        this.drawText(style.monospaceFont(), context, keyName,
                badgeX + 6, badgeY + 2, 0xFF8B8B8B);

        // Bottom separator (inset 1px)
        context.fill(1, hdr - 1, w - 1, hdr, 0x05FFFFFF);
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        if(this.waitingForBind) {
            this.waitingForBind = false;
            if(Keybind.canBindTo(click.button(), false)) {
                this.module.keybind.mouse(click.button());
                return true;
            }
        }

        if (click.y() >= this.getHeaderHeight()) return false;

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Check if click is on the keybind badge area (right side)
            GUIStyle style = GUIStyle.get();
            String keyName = this.waitingForBind ? "..." : this.module.keybind.toString();
            int keyW = (int) this.getTextWidth(style.monospaceFont(), keyName);
            int badgeW = keyW + 12;
            int badgeX = this.getWidth() - 14 - badgeW;

            if (click.x() >= badgeX) {
                this.waitingForBind = true;
                return true;
            }

            Modules.toggle(this.module);
            return true;
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.settingsOpen = !this.settingsOpen;
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        return this.waitingForBind;
    }

    @Override
    public boolean onKeyRelease(KeyInput input) {
        if(!this.waitingForBind) return false;

        this.waitingForBind = false;
        if(!Keybind.canBindTo(input.key(), true)) this.module.keybind.unbind();
        else this.module.keybind.key(input.key());

        return true;
    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        if(!this.waitingForBind) return false;
        return input.key() == GLFW.GLFW_KEY_ESCAPE;
    }

    @Override
    public boolean onFocusRemoved() {
        this.waitingForBind = false;
        return true;
    }
}
