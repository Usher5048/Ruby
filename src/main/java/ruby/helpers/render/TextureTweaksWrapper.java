package ruby.helpers.render;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
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

    private TextureAtlasSprite getSprite(BlockState state, RandomSource random, Direction face) {
        BlockStateModel model = RubyClient.client.getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);

        TextureAtlasSprite sprite = null;
        for(BlockStateModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(face);

            if(!quads.isEmpty()) {
                sprite = quads.getFirst().materialInfo().sprite();
                break;
            }

            quads = part.getQuads(null);
            for(BakedQuad q : quads) {
                if(q.direction() == face) {
                    sprite = q.materialInfo().sprite();
                    break;
                }
            }
        }


        return sprite;
    }

    private void bakeSprite(MutableQuadView quad, BlockState state, RandomSource random) {
        TextureAtlasSprite sprite = this.getSprite(state, random, Direction.UP);
        if(sprite == null) return;

        quad.materialBake(new Material.Baked(sprite, false), MutableQuadView.BAKE_LOCK_UV);
    }

    private boolean canConnectWrapped(BlockGetter view, BlockState state, BlockPos pos, Direction face) {
        TextureTweaks tweaks = Modules.getByClass(TextureTweaks.class);
        if(tweaks != null && tweaks.wrapMode.value() == TextureTweaks.WrapMode.FULL_BLOCK) return true;

        BlockState up = view.getBlockState(pos.above());
        BlockState adj = view.getBlockState(pos.relative(face).below());
        BlockState adjUp = view.getBlockState(pos.relative(face));

        boolean isSnowy = state.getValueOrElse(GrassBlock.SNOWY, false) && !up.isAir();
        boolean isAdjSnowy = adj.getValueOrElse(GrassBlock.SNOWY, false) && !adjUp.isAir();

        return state.getBlock() == adj.getBlock() && (
                adjUp.isAir() || isSnowy || isAdjSnowy ||
                !adjUp.isFaceSturdy(view, pos.relative(face), Direction.DOWN)
        );
    }

    private boolean emitWrappedQuads(
            TextureTweaks tweaks, QuadEmitter emitter, BlockAndTintGetter view,
            BlockPos pos, BlockState state, RandomSource random
    ) {
        if(!tweaks.wrapEnabled.value()) return false;

        List<Block> blocks = tweaks.wrappedBlocks.value();
        if(!blocks.contains(state.getBlock()) && !state.is(Blocks.DIRT))
            return false;

        emitter.pushTransform(quad -> {
            Vector3fc normal = quad.faceNormal();
            Direction face = Direction.getApproximateNearest(normal.x(), normal.y(), normal.z());
            if(face.getAxis().isVertical() || state.hasBlockEntity())
                return true;

            if(state.is(Blocks.DIRT)) {
                BlockState up = view.getBlockState(pos.above());
                if(up.is(Blocks.DIRT_PATH)) {
                    if(this.canConnectWrapped(view, up, pos.above(), face))
                        this.bakeSprite(quad, view.getBlockState(pos.above()), random);
                }

                return true;
            }

            if(this.canConnectWrapped(view, state, pos, face)) {
                boolean isSnowy = state.getValueOrElse(GrassBlock.SNOWY, false);
                this.bakeSprite(quad, isSnowy ? view.getBlockState(pos.above()) : state, random);
            }

            return true;
        });

        return true;
    }

    private void emitRegion(
            TextureAtlasSprite sprite, QuadEmitter emitter, Direction face,
            float gl, float gb, float gr, float gt,
            float ul, float vb, float ur, float vt
    ) {
        emitter.square(face, gl, gb, gr, gt, 0);
        emitter.uv(0, vb * 16, ul * 16);
        emitter.uv(1, vt * 16, ul * 16);
        emitter.uv(2, vt * 16, ur * 16);
        emitter.uv(3, vb * 16, ur * 16);
        emitter.materialBake(new Material.Baked(sprite, false), MutableQuadView.BAKE_ROTATE_90 | MutableQuadView.BAKE_FLIP_V);
        emitter.emit();
    }

    private boolean emitContiguousQuads(
            TextureTweaks tweaks, QuadEmitter emitter, BlockAndTintGetter view,
            BlockPos pos, BlockState state, RandomSource random
    ) {
        if(!tweaks.connectedEnabled.value()) return false;

        ArrayList<Block> blocks = new ArrayList<>(tweaks.connectedBlocks.value());
        if(blocks.contains(Blocks.GLASS)) blocks.addAll(Blocks.STAINED_GLASS.asList());

        if(!blocks.contains(state.getBlock())) return false;

        Predicate<Vec3i> same = off -> view.getBlockState(pos.offset(off)).is(state.getBlock());
        for(Direction face : Direction.values()) {
            boolean north; boolean south;
            boolean east; boolean west;
            boolean ne; boolean nw;
            boolean se; boolean sw;

            switch(face) {
                case UP, DOWN -> {
                    north = same.test(Direction.NORTH.getUnitVec3i());
                    south = same.test(Direction.SOUTH.getUnitVec3i());
                    east = same.test(Direction.EAST.getUnitVec3i());
                    west = same.test(Direction.WEST.getUnitVec3i());
                    ne = same.test(Direction.NORTH.getUnitVec3i().east());
                    nw = same.test(Direction.NORTH.getUnitVec3i().west());
                    se = same.test(Direction.SOUTH.getUnitVec3i().east());
                    sw = same.test(Direction.SOUTH.getUnitVec3i().west());
                }

                case NORTH, SOUTH -> {
                    north = same.test(Direction.UP.getUnitVec3i());
                    south = same.test(Direction.DOWN.getUnitVec3i());
                    east = same.test(Direction.EAST.getUnitVec3i());
                    west = same.test(Direction.WEST.getUnitVec3i());
                    ne = same.test(Direction.UP.getUnitVec3i().east());
                    nw = same.test(Direction.UP.getUnitVec3i().west());
                    se = same.test(Direction.DOWN.getUnitVec3i().east());
                    sw = same.test(Direction.DOWN.getUnitVec3i().west());
                }

                case EAST, WEST -> {
                    north = same.test(Direction.UP.getUnitVec3i());
                    south = same.test(Direction.DOWN.getUnitVec3i());
                    east = same.test(Direction.NORTH.getUnitVec3i());
                    west = same.test(Direction.SOUTH.getUnitVec3i());
                    ne = same.test(Direction.UP.getUnitVec3i().north());
                    nw = same.test(Direction.UP.getUnitVec3i().south());
                    se = same.test(Direction.DOWN.getUnitVec3i().north());
                    sw = same.test(Direction.DOWN.getUnitVec3i().south());
                }

                default -> {
                    return true;
                }
            }

            TextureAtlasSprite sprite = this.getSprite(state, random, face);
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

            boolean offsetY = !state.is(Blocks.BOOKSHELF) || (face == Direction.DOWN || face == Direction.UP);

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
            QuadEmitter emitter, BlockAndTintGetter view, BlockPos pos,
            BlockState state, RandomSource random, Predicate<Direction> cullTest
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
