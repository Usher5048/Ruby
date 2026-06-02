package ruby.helpers;

import net.minecraft.text.ClickEvent;

public class RunnableClickEvent implements ClickEvent {
    private final Runnable impl;
    public RunnableClickEvent(Runnable impl) {
        this.impl = impl;
    }

    public void run() {
        this.impl.run();
    }

    @Override
    public Action getAction() {
        return null;
    }
}
