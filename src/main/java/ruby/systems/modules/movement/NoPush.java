package ruby.systems.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.systems.config.StringListValue;
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

        Minecraft mc = RubyClient.client;
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.options.keyJump.isDown() || mc.options.keyShift.isDown()) return;

        if ((player.isInWater() || player.isInLava()) && player.getDeltaMovement().y < 0) {
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, 0, velocity.z);
        }
    }
}
