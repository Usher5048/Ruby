package ruby.systems.commands;

import ruby.RubyClient;

public class Help extends Command {
    protected Help() {
        super("Help", "Provides help");
    }

    @Override
    public void execute(String[] args) {
        RubyClient.notifyUser("Press " + RubyClient.openGUIKey.getBoundKeyLocalizedText().getString() + " to open the Click GUI");
    }
}
