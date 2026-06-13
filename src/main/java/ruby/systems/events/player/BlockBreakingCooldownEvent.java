package ruby.systems.events.player;

import ruby.systems.events.Event;

public class BlockBreakingCooldownEvent extends Event {
    public int cooldown;

    public BlockBreakingCooldownEvent(int cooldown) {
        this.cooldown = cooldown;
    }
}
