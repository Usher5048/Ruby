package ruby.helpers.world;

import net.minecraft.client.Minecraft;
import ruby.RubyClient;

/**
 * Rebuilds compiled chunk geometry on the render thread.
 * {@code resetLevelRenderData} only clears the view area and leaves the next frame with nothing to draw.
 */
public final class ChunkReloadHelper {
    private ChunkReloadHelper() {}

    public static void schedule() {
        Minecraft client = RubyClient.client;
        if (client.level == null) return;

        if (client.isSameThread()) {
            reload(client);
        } else {
            client.execute(() -> reload(client));
        }
    }

    private static void reload(Minecraft client) {
        if (client.level == null) return;
        client.levelRenderer.invalidateCompiledGeometry(
                client.level,
                client.options,
                client.gameRenderer.mainCamera(),
                client.getBlockColors()
        );
    }
}
