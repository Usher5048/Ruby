package ruby.systems.events;

public abstract class Event {
    private boolean cancelled = false;

    public boolean isCancelled() {
        return this.cancelled;
    }
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
