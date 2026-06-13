package ruby.systems.modules.world;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ruby.RubyClient;
import ruby.systems.config.IntegerValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Air place ported from Meteor Client.
 */
public class AirPlace extends Module {

    private final IntegerValue range;

    public AirPlace() {
        super("Air Place", "Places blocks in midair.", ModuleType.WORLD);

        range = config.create(new IntegerValue.Builder("Range")
                .range(1, 6).defaultValue(4).build());
    }

    @Override
    public void tick() {
        if (RubyClient.client.player == null || RubyClient.client.world == null
                || RubyClient.client.interactionManager == null) return;
        if (RubyClient.client.currentScreen != null) return;
        if (!RubyClient.client.options.useKey.isPressed()) return;

        ItemStack stack = RubyClient.client.player.getMainHandStack();
        if (!(stack.getItem() instanceof BlockItem)) return;

        if (RubyClient.client.crosshairTarget != null
                && RubyClient.client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return;
        }

        Vec3d eyes = RubyClient.client.player.getEyePos();
        Vec3d look = RubyClient.client.player.getRotationVector();
        Vec3d end = eyes.add(look.multiply(range.value()));

        BlockHitResult hit = RubyClient.client.world.raycast(new RaycastContext(
                eyes, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                RubyClient.client.player
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            BlockPos neighbor = BlockPos.ofFloored(end).down();
            if (!RubyClient.client.world.getBlockState(neighbor).isAir()) {
                hit = new BlockHitResult(Vec3d.ofCenter(neighbor), Direction.UP, neighbor, false);
            } else {
                return;
            }
        }

        var result = RubyClient.client.interactionManager.interactBlock(
                RubyClient.client.player, Hand.MAIN_HAND, hit
        );
        if (result.isAccepted()) RubyClient.client.player.swingHand(Hand.MAIN_HAND);
    }
}
