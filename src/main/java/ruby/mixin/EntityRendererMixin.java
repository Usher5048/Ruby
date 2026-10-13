package ruby.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Nametags;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    /**
     * Players never call {@code EntityRenderer.shouldShowName}. The name is written
     * here, after every override, so this is the one place that covers all of them.
     */
    @Inject(
            method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At("TAIL")
    )
    private void ruby$hideVanillaName(
            Entity entity,
            EntityRenderState state,
            float partialTick,
            double nameRange,
            double scoreRange,
            CallbackInfo ci
    ) {
        Nametags nametags = Modules.getByClass(Nametags.class);
        if (nametags == null || !nametags.enabled() || !nametags.hidesVanilla(entity)) return;
        state.nameTag = null;
        state.scoreText = null;
    }
}
