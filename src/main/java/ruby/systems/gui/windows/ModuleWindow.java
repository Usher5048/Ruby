package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import ruby.systems.gui.GUIStyle;
import ruby.systems.modules.Module;

public class ModuleWindow extends CollapsibleWindow {

    private final Module module;

    public ModuleWindow(int x, int y, Module module) {
        super(x, y, 130, 20);
        this.module = module;
        this.handleChildren = false;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {

    }

    @Override
    public void drawHeader(DrawContext context) {
        if (this.module.enabled()) {
            context.fill(0, 0, this.getWidth(), this.getHeaderHeight(), GUIStyle.get().enabledBGColor());
        } else {
            context.fill(0, 0, this.getWidth(), this.getHeaderHeight(), GUIStyle.get().disabledBGColor());
        }
        context.fill(0, 0, this.getWidth(), this.getHeaderHeight(), GUIStyle.get().headerBGColor());
    }

    @Override
    public boolean onMouseDown(Click click, boolean doubled) {
        return false;
    }

    @Override
    public boolean onMouseUp(Click click) {
        return false;
    }

    @Override
    public boolean onMouseDragged(Click click, double deltaX, double deltaY) {
        return false;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY) {

    }

    @Override
    public boolean onKeyPress(KeyInput input) {
        return false;
    }

    @Override
    public boolean onKeyRelease(KeyInput input) {
        return false;
    }

    @Override
    public boolean onCharTyped(CharInput input) {
        return false;
    }

    @Override
    public void onFocused() {

    }

    @Override
    public void onFocusRemoved() {

    }
}
