package ruby.plugins;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = "ruby.mixin";

    private static boolean loaded;
    private static boolean indigoLoaded;
    private static boolean sodiumLoaded;
    private static boolean lithiumLoaded;
    private static boolean viaFabricPlusLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        if (loaded) return;

        FabricLoader loader = FabricLoader.getInstance();
        indigoLoaded = loader.isModLoaded("fabric-renderer-indigo");
        sodiumLoaded = loader.isModLoaded("sodium");
        lithiumLoaded = loader.isModLoaded("lithium");
        viaFabricPlusLoaded = loader.isModLoaded("viafabricplus");

        loaded = true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE)) {
            throw new IllegalStateException("Mixin " + mixinClassName + " is not in the " + MIXIN_PACKAGE + " package.");
        }

        if (mixinClassName.startsWith(MIXIN_PACKAGE + ".sodium")) return sodiumLoaded;
        if (mixinClassName.startsWith(MIXIN_PACKAGE + ".indigo")) return indigoLoaded;
        if (mixinClassName.startsWith(MIXIN_PACKAGE + ".lithium")) return lithiumLoaded;
        if (mixinClassName.startsWith(MIXIN_PACKAGE + ".viafabricplus")) return viaFabricPlusLoaded;

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
