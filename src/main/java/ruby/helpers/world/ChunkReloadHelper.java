package ruby.helpers.world;

import ruby.RubyClient;

/**
 * Schedules {@code worldRenderer.reload()} on the render thread only.
 */
public final class ChunkReloadHelper {
    private ChunkReloadHelper() {}

    public static void schedule() {
        if (RubyClient.client == null || RubyClient.client.worldRenderer == null) return;

        if (RubyClient.client.isOnThread()) {
            RubyClient.client.worldRenderer.reload();
        } else {
            RubyClient.client.execute(() -> {
                if (RubyClient.client.worldRenderer != null) {
                    RubyClient.client.worldRenderer.reload();
                }
            });
        }
    }
}
