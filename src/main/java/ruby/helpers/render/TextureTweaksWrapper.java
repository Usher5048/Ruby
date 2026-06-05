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
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import ruby.RubyClient;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.TextureTweaks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TextureTweaksWrapper extends WrapperBlockStateModel implements BlockStateModel {
    public TextureTweaksWrapper(BlockStateModel model) {
        super(model);
    }

    private Sprite getSprite(BlockState state, Random random, Direction face) {
        BlockModels models = RubyClient.client.getBakedModelManager().getBlockModels();
        List<BlockModelPart> parts = models.getModel(state).getParts(random);

        Sprite sprite = null;
        for(BlockModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(face);

            if(!quads.isEmpty()) {
                sprite = quads.getFirst().sprite();
                break;
            }

            quads = part.getQuads(null);
            for(BakedQuad q : quads) {
                if(q.face() == face) {
                    sprite = q.sprite();
                    break;
                }
            }
        }


        return sprite;
    }

    private void bakeSprite(MutableQuadView quad, BlockState state, Random random) {
        Sprite sprite = this.getSprite(state, random, Direction.UP);
        if(sprite == null) return;

        quad.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
    }

    private boolean canConnectWrapped(BlockView view, BlockState state, BlockPos pos, Direction face) {
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

    private boolean emitWrappedQuads(
            TextureTweaks tweaks, QuadEmitter emitter, BlockRenderView view,
            BlockPos pos, BlockState state, Random random
    ) {
        if(!tweaks.wrapEnabled.value()) return false;

        List<Block> blocks = tweaks.wrappedBlocks.value();
        if(!blocks.contains(state.getBlock()) && !state.isOf(Blocks.DIRT))
            return false;

        emitter.pushTransform(quad -> {
            Direction face = quad.nominalFace();
            if(face == null || face.getAxis().isVertical() || state.hasBlockEntity())
                return true;

            if(state.isOf(Blocks.DIRT)) {
                BlockState up = view.getBlockState(pos.up());
                if(up.isOf(Blocks.DIRT_PATH)) {
                    if(this.canConnectWrapped(view, up, pos.up(), face))
                        this.bakeSprite(quad, view.getBlockState(pos.up()), random);
                }

                return true;
            }

            if(this.canConnectWrapped(view, state, pos, face)) {
                boolean isSnowy = state.get(GrassBlock.SNOWY, false);
                this.bakeSprite(quad, isSnowy ? view.getBlockState(pos.up()) : state, random);
            }

            return true;
        });

        return true;
    }

    private void emitRegion(
            Sprite sprite, QuadEmitter emitter, Direction face,
            float gl, float gb, float gr, float gt,
            float ul, float vb, float ur, float vt
    ) {
        emitter.square(face, gl, gb, gr, gt, 0);
        emitter.uv(0, vb * 16, ul * 16);
        emitter.uv(1, vt * 16, ul * 16);
        emitter.uv(2, vt * 16, ur * 16);
        emitter.uv(3, vb * 16, ur * 16);
        emitter.spriteBake(sprite, MutableQuadView.BAKE_ROTATE_90 | MutableQuadView.BAKE_FLIP_V);
        emitter.emit();
    }

    private boolean emitContiguousQuads(
            TextureTweaks tweaks, QuadEmitter emitter, BlockRenderView view,
            BlockPos pos, BlockState state, Random random
    ) {
        if(!tweaks.connectedEnabled.value()) return false;

        List<Block> glass = List.of(
                Blocks.GRAY_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
                Blocks.BROWN_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS,
                Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
                Blocks.MAGENTA_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.PINK_STAINED_GLASS,
                Blocks.PURPLE_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.WHITE_STAINED_GLASS,
                Blocks.YELLOW_STAINED_GLASS
        );

        ArrayList<Block> blocks = new ArrayList<>(tweaks.connectedBlocks.value());
        if(blocks.contains(Blocks.GLASS)) blocks.addAll(glass);

        if(!blocks.contains(state.getBlock())) return false;

        Predicate<Vec3i> same = off -> view.getBlockState(pos.add(off)).isOf(state.getBlock());
        for(Direction face : Direction.values()) {
            boolean north; boolean south;
            boolean east; boolean west;
            boolean ne; boolean nw;
            boolean se; boolean sw;

            switch(face) {
                case UP, DOWN -> {
                    north = same.test(Direction.NORTH.getVector());
                    south = same.test(Direction.SOUTH.getVector());
                    east = same.test(Direction.EAST.getVector());
                    west = same.test(Direction.WEST.getVector());
                    ne = same.test(Direction.NORTH.getVector().east());
                    nw = same.test(Direction.NORTH.getVector().west());
                    se = same.test(Direction.SOUTH.getVector().east());
                    sw = same.test(Direction.SOUTH.getVector().west());
                }

                case NORTH, SOUTH -> {
                    north = same.test(Direction.UP.getVector());
                    south = same.test(Direction.DOWN.getVector());
                    east = same.test(Direction.EAST.getVector());
                    west = same.test(Direction.WEST.getVector());
                    ne = same.test(Direction.UP.getVector().east());
                    nw = same.test(Direction.UP.getVector().west());
                    se = same.test(Direction.DOWN.getVector().east());
                    sw = same.test(Direction.DOWN.getVector().west());
                }

                case EAST, WEST -> {
                    north = same.test(Direction.UP.getVector());
                    south = same.test(Direction.DOWN.getVector());
                    east = same.test(Direction.NORTH.getVector());
                    west = same.test(Direction.SOUTH.getVector());
                    ne = same.test(Direction.UP.getVector().north());
                    nw = same.test(Direction.UP.getVector().south());
                    se = same.test(Direction.DOWN.getVector().north());
                    sw = same.test(Direction.DOWN.getVector().south());
                }

                default -> {
                    return true;
                }
            }

            Sprite sprite = this.getSprite(state, random, face);
            if(sprite == null) continue;

            boolean tmp;
            if(face == Direction.NORTH || face == Direction.WEST) {
                tmp = east;
                east = west;
                west = tmp;

                tmp = ne;
                ne = nw;
                nw = tmp;

                tmp = se;
                se = sw;
                sw = tmp;
            }

            if(face == Direction.DOWN) {
                tmp = north;
                north = south;
                south = tmp;

                tmp = ne;
                ne = se;
                se = tmp;

                tmp = nw;
                nw = sw;
                sw = tmp;
            }

            float z = (float) (    (1.0 / 16));
            float o = (float) (1 - (1.0 / 16));

            this.emitRegion(sprite, emitter, face, z, z, o, o, z, z, o, o);

            boolean offsetY = !state.isOf(Blocks.BOOKSHELF) || (face == Direction.DOWN || face == Direction.UP);

            if(!offsetY || !north) this.emitRegion(sprite, emitter, face, z, o, o, 1, z, o, o, 1);
            else this.emitRegion(sprite, emitter, face, z, o, o, 1, z, z, o, z+z);
            if(!offsetY || !south) this.emitRegion(sprite, emitter, face, z, 0, o, z, z, 0, o, z);
            else this.emitRegion(sprite, emitter, face, z, 0, o, z, z, o-z, o, o);
            if(!east ) this.emitRegion(sprite, emitter, face, o, z, 1, o, o, z, 1, o);
            else this.emitRegion(sprite, emitter, face, o, z, 1, o, o-z, z, o, o);
            if(!west ) this.emitRegion(sprite, emitter, face, 0, z, z, o, 0, z, z, o);
            else this.emitRegion(sprite, emitter, face, 0, z, z, o, z, z, z+z, o);

            if(!north || !east || !ne) this.emitRegion(sprite, emitter, face, o, o, 1, 1, o, o, 1, 1);
            else this.emitRegion(sprite, emitter, face, o, o, 1, 1, o-z, offsetY ? z : 0, o, offsetY ? z+z : z);
            if(!north || !west || !nw) this.emitRegion(sprite, emitter, face, 0, o, z, 1, 0, o, z, 1);
            else this.emitRegion(sprite, emitter, face, 0, o, z, 1, z, offsetY ? z : 0, z+z, offsetY ? z+z : z);
            if(!south || !east || !se) this.emitRegion(sprite, emitter, face, o, 0, 1, z, o, 0, 1, z);
            else this.emitRegion(sprite, emitter, face, o, 0, 1, z, o-z, offsetY ? o-z : o, o, offsetY ? o : 1);
            if(!south || !west || !sw) this.emitRegion(sprite, emitter, face, 0, 0, z, z, 0, 0, z, z);
            else this.emitRegion(sprite, emitter, face, 0, 0, z, z, z, offsetY ? o-z : o, z+z, offsetY ? o : 1);
        }

        return true;
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

        boolean popWrapped = this.emitWrappedQuads(
                tweaks, emitter, view,
                pos, state, random
        );

        boolean connectEmitted = this.emitContiguousQuads(
                tweaks, emitter, view,
                pos, state, random
        );

        if(!connectEmitted) super.emitQuads(emitter, view, pos, state, random, cullTest);
        if(popWrapped) emitter.popTransform();
    }
}
