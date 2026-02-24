package ruby.systems.modules;

import ruby.RubyClient;
import ruby.systems.events.Events;

import java.util.ArrayList;

public class ModuleManager {
    private static final ArrayList<Module> activeModules = new ArrayList<>();
    private static final ArrayList<Module> modules = new ArrayList<>();

    static {
//        Events.TICK.register(TickEvents.START, ModuleManager::tick);
//        Events.RENDER_2D.register(ModuleManager::renderScreen);
//        Events.RENDER_3D.register(ModuleManager::renderWorld);

        // Combat

        // Movement

        // Player

        // Render

        // World

        // Exploit

        // Misc
    }

    public static ArrayList<Module> getModules() {
        return ModuleManager.modules;
    }
    public static ArrayList<Module> getActiveModules() {
        return ModuleManager.activeModules;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getByClass(Class<T> _class) {
        for(Module module : modules)
            if(module.getClass() == _class) return (T) module;

        return null;
    }

    public static Module getByName(String name) {
        for(Module module : modules)
            if(module.name().equalsIgnoreCase(name)) return module;

        return null;
    }

    public static void enable(Module module) {
        if(module == null) return;
        if(module.enabled()) return;

        ModuleManager.activeModules.add(module);

        if(module.showsToasts())
            RubyClient.notifyUser("Enabled " + module);

        module.enabled = true;
        module.onEnable();
    }

    public static void disable(Module module) {
        if(module == null) return;
        if(!module.enabled()) return;

        ModuleManager.activeModules.remove(module);

        if(module.showsToasts())
            RubyClient.notifyUser("Disabled " + module);

        module.enabled = false;
        module.onDisable();
    }

    public static void toggle(Module module) {
        if(module == null) return;
        if(!module.enabled()) ModuleManager.enable(module);
        else ModuleManager.disable(module);
    }

    public static void setEnabled(Module module, boolean isEnabled) {
        if(module == null) return;
        if(module.enabled != isEnabled)
            ModuleManager.toggle(module);
    }
}
