package ruby.systems.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ruby.RubyClient;
import ruby.helpers.HitResults;
import ruby.helpers.Rotations;
import ruby.helpers.render.Renderer;
import ruby.systems.config.BooleanValue;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Automatically places blocks below the player as they walk.
 * Only works when holding a block in the main hand.
 */
public class Scaffold extends Module {

    private final BooleanValue onlyOnMove;

    private float yaw = Float.NaN;
    private float pitch = Float.NaN;
    private BlockHitResult hitResult = null;

    public Scaffold() {
        super("Scaffold", "Automatically places blocks below you.", ModuleType.WORLD);

        onlyOnMove = config.create(new BooleanValue.Builder("Only On Move")
                .description("Only place blocks when you are moving.")
                .defaultValue(true)
                .build());

        config.create(new BooleanValue.Builder("Tower")
                .description("Place blocks below when jumping upward.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void onEnable() {
        this.yaw = Float.NaN;
        this.pitch = Float.NaN;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) return;

        this.yaw = Float.NaN; this.pitch = Float.NaN;

        // Check if holding a block
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof BlockItem)) return;

        // Check if moving (if setting is enabled)
        if (onlyOnMove.value() && player.zza == 0 && player.xxa == 0
                && !mc.options.keyJump.isDown()) return;

        BlockPos below = player.blockPosition().below();

        // Check if there's air below
        BlockState belowState = mc.level.getBlockState(below);
        if (!belowState.canBeReplaced()) return;

        // Find a face to place against
        Direction placeDir = findPlaceFace(mc, below);
        if (placeDir == null) return;

        BlockPos neighbor = below.relative(placeDir);
        this.hitResult = HitResults.createBlock(neighbor, placeDir.getOpposite());

        // i tried
        float[] angles = Rotations.rotationTo(EntityAnchorArgument.Anchor.FEET, this.hitResult.getLocation());
        this.yaw = player.getYRot(); this.pitch = player.getXRot();
        player.setYRot(angles[0]); player.setXRot(angles[1]);

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, this.hitResult);
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
    }

    @Override
    public void tickEnd() {
        if(RubyClient.client.player == null) return;
        if(Float.isNaN(this.yaw) || Float.isNaN(this.pitch)) return;

        RubyClient.client.player.setYRot(this.yaw);
        RubyClient.client.player.setXRot(this.pitch);
        RubyClient.client.player.setOldRot();

        this.yaw = Float.NaN; this.pitch = Float.NaN;
    }

    @Override
    public void render3D() {
        if(this.hitResult == null) return;

        Renderer.setMode(Renderer.Mode.STROKE_ALWAYS_ON_TOP);
        Renderer.color(0x00FF00);
        Renderer.cuboid(this.hitResult.getLocation(), new Vec3(0.1, 0.1, 0.1));
    }

    private Direction findPlaceFace(Minecraft mc, BlockPos pos) {
        if(mc.level == null) return null;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState state = mc.level.getBlockState(neighbor);
            if (!state.canBeReplaced() && !state.isAir()) {
                return dir;
            }
        }
        return null;
    }
}
