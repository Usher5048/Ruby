package ruby.systems.gui;

public sealed interface ClickGuiSelection permits ClickGuiSelection.ModuleCategory, ClickGuiSelection.Special {
    record ModuleCategory(ruby.systems.modules.ModuleType type) implements ClickGuiSelection {}
    record Special(SpecialView view) implements ClickGuiSelection {}

    enum SpecialView {
        FRIENDS,
        PROFILES,
        THEME
    }
}
