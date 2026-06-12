package ruby.systems.gui;

import ruby.systems.config.Value;
import ruby.systems.modules.Module;

public final class ClickGuiSearch {
    private ClickGuiSearch() {}

    public static boolean matches(Module module, String query) {
        if (query == null || query.isEmpty()) return false;

        String q = query.toLowerCase();

        if (module.name().toLowerCase().contains(q)) return true;
        if (module.description().toLowerCase().contains(q)) return true;
        if (module.category().toString().toLowerCase().contains(q)) return true;

        for (String key : module.config.getAll()) {
            if (key.toLowerCase().contains(q)) return true;
            Value<?> value = module.config.get(key);
            String desc = value.description();
            if (desc != null && !desc.isEmpty() && desc.toLowerCase().contains(q)) return true;
        }

        return false;
    }
}
