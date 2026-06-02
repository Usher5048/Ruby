package ruby.plugins;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import ruby.helpers.render.BetterGrassWrapper;
import ruby.helpers.render.UnbakedWrapper;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.TextureTweaks;

public class ModelModifierPlugin implements ModelLoadingPlugin {
    @Override
    public void initialize(ModelLoadingPlugin.Context pluginContext) {
        pluginContext.modifyBlockModelOnLoad().register(ModelModifier.WRAP_LAST_PHASE, (model, context) -> {
            if(Modules.getByClass(TextureTweaks.class) == null) return model;
            return new UnbakedWrapper(model, BetterGrassWrapper::new);
        });
    }
}
