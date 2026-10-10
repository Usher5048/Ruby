package ruby.systems.modules.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
        if (RubyClient.client.player == null || RubyClient.client.level == null
                || RubyClient.client.gameMode == null) return;
        if (RubyClient.client.gui.screen() != null) return;
        if (!RubyClient.client.options.keyUse.isDown()) return;

        ItemStack stack = RubyClient.client.player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem)) return;

        if (RubyClient.client.hitResult != null
                && RubyClient.client.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }

        Vec3 eyes = RubyClient.client.player.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(RubyClient.client.player.getRotationVector());
        Vec3 end = eyes.add(look.scale(range.value()));

        BlockHitResult hit = RubyClient.client.level.clip(new ClipContext(
                eyes, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                RubyClient.client.player
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            BlockPos neighbor = BlockPos.containing(end).below();
            if (!RubyClient.client.level.getBlockState(neighbor).isAir()) {
                hit = new BlockHitResult(Vec3.atCenterOf(neighbor), Direction.UP, neighbor, false);
            } else {
                return;
            }
        }

        var result = RubyClient.client.gameMode.useItemOn(RubyClient.client.player, InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction()) RubyClient.client.player.swing(
                InteractionHand.MAIN_HAND,
                SwingAnimation.DEFAULT,
                true
        );
    }
}
