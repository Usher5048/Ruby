package ruby.systems.gui.windows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import ruby.systems.modules.Module;

import java.util.List;

public class ModuleTypeWindow extends CollapsibleWindow {

    private final List<Module> modules;

    public ModuleTypeWindow(int x, int y, List<Module> modules) {
        super(x, y, 130, 20);
        this.modules = modules;
    }

    @Override
    public void onRender(DrawContext context, int mouseX, int mouseY) {

    }

    @Override
    public void drawHeader(DrawContext context) {

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
