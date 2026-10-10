package ruby.mixin.sodium;

import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Xray;

@Mixin(value = LightDataAccess.class, remap = false)
public abstract class SodiumLightDataAccessMixin {
    @Unique
    private static final int FULL_LIGHT = 15 | 15 << 4 | 15 << 8;

    @Shadow protected BlockAndTintGetter level;
    @Shadow @Final private BlockPos.MutableBlockPos pos;

    @ModifyVariable(method = "compute", at = @At(value = "TAIL"), name = "bl", remap = false)
    private int ruby$fullBrightOres(int light) {
        Xray xray = Modules.getByClass(Xray.class);
        if (xray == null || !xray.enabled()) return light;

        BlockState state = this.level.getBlockState(this.pos);
        if (!xray.isBlocked(state.getBlock(), this.pos.immutable(), this.level)) {
            return FULL_LIGHT;
        }

        return light;
    }
}
