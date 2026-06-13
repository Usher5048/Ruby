package ruby.helpers;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtils {
    private RandomUtils() {
    }

    public static int randomInt(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static double randomDouble(double min, double max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float randomFloat(float min, float max) {
        if (min >= max) return min;
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static boolean chance(float percent) {
        if (percent >= 100f) return true;
        if (percent <= 0f) return false;
        return ThreadLocalRandom.current().nextFloat() * 100f <= percent;
    }
}
