package ruby.systems.commands;

import ruby.RubyClient;

public abstract class Command {
    private final String name;
    private final String description;

    protected String origin;

    protected Command(String name, String description) {
        this.name = name;
        this.description = description;
        this.origin = RubyClient.MOD_NAME;
    }
}
