package ruby.systems.commands;

import java.util.ArrayList;

public class Commands {
    private static final ArrayList<Command> commands = new ArrayList<>();

    static {
        Commands.commands.add(new Help());
    }

    public static ArrayList<Command> getCommands() {
        return Commands.commands;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Command> T getByClass(Class<T> _class) {
        for(Command cmd : Commands.commands)
            if(cmd.getClass() == _class) return (T) cmd;

        return null;
    }

    public static Command getByName(String name) {
        for(Command cmd : Commands.commands)
            if(cmd.name().equalsIgnoreCase(name)) return cmd;

        return null;
    }
}
