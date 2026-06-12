package ruby.helpers;

import java.util.concurrent.ThreadLocalRandom;

public final class ClickScheduler {
    private final int minCps;
    private final int maxCps;
    private int ticksUntilClick;
    private double targetCps;

    public ClickScheduler(int minCps, int maxCps) {
        this.minCps = minCps;
        this.maxCps = maxCps;
        this.targetCps = (minCps + maxCps) * 0.5;
        this.ticksUntilClick = 0;
    }

    public void reset() {
        this.ticksUntilClick = 0;
        this.targetCps = (this.minCps + this.maxCps) * 0.5;
    }

    public boolean shouldClick() {
        if(this.ticksUntilClick > 0) {
            this.ticksUntilClick--;
            return false;
        }

        this.scheduleNext();
        return true;
    }

    public boolean willClickAt(int ticks) {
        return this.ticksUntilClick < ticks;
    }

    private void scheduleNext() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        this.targetCps += (rng.nextDouble() - 0.5) * 0.4;
        this.targetCps = Math.clamp(this.targetCps, this.minCps, this.maxCps);
        this.ticksUntilClick = Math.max(1, (int) Math.round(20.0 / this.targetCps));
    }
}
