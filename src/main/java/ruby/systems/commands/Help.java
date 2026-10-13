package ruby.systems.commands;

import ruby.RubyClient;
import ruby.systems.gui.Toasts;

public class Help extends Command {
    protected Help() {
        super("Help", "Provides help");
    }

    @Override
    public void execute(String[] args) {
        RubyClient.notifyUser("Press " + RubyClient.openGUIKey.getTranslatedKeyMessage().getString()
                + " for Click GUI, " + RubyClient.openCommandKey.getTranslatedKeyMessage().getString()
                + " for commands (" + RubyClient.chatPrefix.value() + ")");
    }
}
