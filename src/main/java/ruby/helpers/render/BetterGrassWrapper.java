package ruby.helpers.render;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import ruby.RubyClient;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.TextureTweaks;

import java.util.List;
import java.util.function.Predicate;

public class BetterGrassWrapper extends WrapperBlockStateModel implements BlockStateModel {
    public BetterGrassWrapper(BlockStateModel model) {
        super(model);
    }

    private void bakeSprite(MutableQuadView quad, BlockState state, Random random) {
        BlockModels models = RubyClient.client.getBakedModelManager().getBlockModels();
        List<BakedQuad> quads = models.getModel(state).getParts(random).getFirst().getQuads(Direction.UP);

        Sprite sprite = null;
        if(!quads.isEmpty()) sprite = quads.getFirst().sprite();

        if(sprite == null) {
            quads = models.getModel(state).getParts(random).getFirst().getQuads(null);
            for(BakedQuad q : quads) if(q.face() == Direction.UP) sprite = q.sprite();
        }

        if(sprite == null) return;
        quad.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
    }

    private boolean canConnect(BlockView view, BlockState state, BlockPos pos, Direction face) {
        TextureTweaks tweaks = Modules.getByClass(TextureTweaks.class);
        if(tweaks != null && tweaks.wrapMode.value() == TextureTweaks.WrapMode.FULL_BLOCK) return true;

        BlockState up = view.getBlockState(pos.up());
        BlockState adj = view.getBlockState(pos.offset(face).down());
        BlockState adjUp = view.getBlockState(pos.offset(face));

        boolean isSnowy = state.get(GrassBlock.SNOWY, false) && !up.isAir();
        boolean isAdjSnowy = adj.get(GrassBlock.SNOWY, false) && !adjUp.isAir();

        return state.getBlock() == adj.getBlock() && (
                adjUp.isAir() || isSnowy || isAdjSnowy ||
                !adjUp.isSideSolidFullSquare(view, pos.offset(face), Direction.DOWN)
        );
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter, BlockRenderView view, BlockPos pos,
            BlockState state, Random random, Predicate<Direction> cullTest
    ) {
        TextureTweaks tweaks = Modules.getByClass(TextureTweaks.class);
        if(tweaks == null) {
            super.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        if(!tweaks.enabled()) {
            super.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        if(!tweaks.wrapEnabled.value()) {
            super.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        List<Block> blocks = tweaks.wrappedBlocks.value();
        if(!blocks.contains(state.getBlock()) && !state.isOf(Blocks.DIRT)) {
            super.emitQuads(emitter, view, pos, state, random, cullTest);
            return;
        }

        emitter.pushTransform(quad -> {
            Direction face = quad.nominalFace();
            if(face == null || face.getAxis().isVertical() || state.hasBlockEntity())
                return true;

            if(state.isOf(Blocks.DIRT)) {
                BlockState up = view.getBlockState(pos.up());
                if(up.isOf(Blocks.DIRT_PATH)) {
                    if(this.canConnect(view, up, pos.up(), face))
                        this.bakeSprite(quad, view.getBlockState(pos.up()), random);
                }

                return true;
            }

            if(this.canConnect(view, state, pos, face)) {
                boolean isSnowy = state.get(GrassBlock.SNOWY, false);
                this.bakeSprite(quad, isSnowy ? view.getBlockState(pos.up()) : state, random);
            }

            return true;
        });

        super.emitQuads(emitter, view, pos, state, random, cullTest);
        emitter.popTransform();
    }
}
