package ruby.systems.gui;

public sealed interface ClickGuiSelection permits ClickGuiSelection.ModuleCategory, ClickGuiSelection.Special, ClickGuiSelection.Search {
    record ModuleCategory(ruby.systems.modules.ModuleType type) implements ClickGuiSelection {}
    record Special(SpecialView view) implements ClickGuiSelection {}
    record Search(String query) implements ClickGuiSelection {}

    enum SpecialView {
        FRIENDS,
        PROFILES,
        THEME
    }
}
