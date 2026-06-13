package ruby.systems.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ruby.RubyClient;
import ruby.systems.config.StringListValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * LiquidBounce {@code NoPush} port.
 */
public class NoPush extends Module {
    public enum PushBy {
        Entities,
        Blocks,
        FishingRod,
        Liquids,
        Sinking
    }

    public static NoPush INSTANCE;

    private final StringListValue pushBy;

    public NoPush() {
        super("No Push", "Disables pushing from entities and other sources.", ModuleType.MOVEMENT);
        INSTANCE = this;

        pushBy = config.create(new StringListValue.Builder("Push By")
                .description("Push sources excluded from NoPush (still allowed).")
                .defaultValue("Entities")
                .build());
    }

    public static boolean canPush(PushBy by) {
        NoPush noPush = INSTANCE;
        if (noPush == null || !noPush.enabled()) return true;
        return noPush.isSelected(by);
    }

    private boolean isSelected(PushBy by) {
        for (String tag : pushBy.value()) {
            if (by.name().equalsIgnoreCase(tag.replace(" ", ""))) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        if (!enabled() || !isSelected(PushBy.Sinking)) return;

        MinecraftClient mc = RubyClient.client;
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        if (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed()) return;

        if ((player.isTouchingWater() || player.isInLava()) && player.getVelocity().y < 0) {
            Vec3d velocity = player.getVelocity();
            player.setVelocity(velocity.x, 0, velocity.z);
        }
    }
}
