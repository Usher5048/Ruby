package ruby.helpers;

import net.minecraft.network.chat.ClickEvent;

public class RunnableClickEvent implements ClickEvent {
    private final Runnable impl;
    public RunnableClickEvent(Runnable impl) {
        this.impl = impl;
    }

    public void run() {
        this.impl.run();
    }

    @Override
    public Action action() {
        return null;
    }
}
