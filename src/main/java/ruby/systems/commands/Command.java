package ruby.systems.commands;

import ruby.RubyClient;

import java.util.List;

public abstract class Command {
    private final String name;
    private final String description;

    protected String origin;

    protected Command(String name, String description) {
        this.name = name;
        this.description = description;
        this.origin = RubyClient.MOD_NAME;
    }

    public String name() {
        return this.name;
    }
    public String description() {
        return this.description;
    }

    public abstract void execute(String[] args);

    public List<String> suggest(String[] args, int argIndex) {
        return List.of();
    }
}
