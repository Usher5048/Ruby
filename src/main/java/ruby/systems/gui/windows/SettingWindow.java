package ruby.systems.gui.windows;

import ruby.systems.config.Value;

public class SettingWindow extends Window {
    protected final Value<?> value;

    public SettingWindow(int x, int y, int width, int height, Value<?> value) {
        super(x, y, width, height);
        this.value = value;
    }

    public Value<?> value() {
        return this.value;
    }
}
