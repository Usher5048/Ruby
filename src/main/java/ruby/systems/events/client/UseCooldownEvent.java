package ruby.systems.events.client;

import ruby.systems.events.Event;

public class UseCooldownEvent extends Event {
    private int cooldown;

    public UseCooldownEvent(int cooldown) {
        this.cooldown = cooldown;
    }

    public int cooldown() {
        return this.cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
}
