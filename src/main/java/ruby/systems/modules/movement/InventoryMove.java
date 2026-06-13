package ruby.systems.modules.movement;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.GameOptions;
import ruby.RubyClient;
import ruby.helpers.input.InputUtils;
import ruby.systems.config.BooleanValue;
import ruby.systems.gui.ClickGUI;
import ruby.systems.modules.Module;
import ruby.systems.modules.ModuleType;

/**
 * Ported from <a href="https://github.com/MeteorDevelopment/meteor-client">Meteor Client</a>
 * Licensed under GPL-3.0
 * <p>
 * Allows you to move, jump, sneak, and sprint while in GUI screens.
 * Works by pressing movement keys based on their keybind state each tick
 * when a screen is open.
 */
public class InventoryMove extends Module {

    private final BooleanValue jump;
    private final BooleanValue sneak;
    private final BooleanValue sprint;

    public InventoryMove() {
        super("Inventory Move", "Allows you to move while in GUIs.", ModuleType.MOVEMENT);

        jump = config.create(new BooleanValue.Builder("Jump")
                .description("Allows you to jump while in GUIs.")
                .defaultValue(true)
                .build());

        sneak = config.create(new BooleanValue.Builder("Sneak")
                .description("Allows you to sneak while in GUIs.")
                .defaultValue(true)
                .build());

        sprint = config.create(new BooleanValue.Builder("Sprint")
                .description("Allows you to sprint while in GUIs.")
                .defaultValue(true)
                .build());
    }

    @Override
    public void tick() {
        switch(RubyClient.client.currentScreen) {
            case null                 -> { return; }
            case ChatScreen c         -> { return; }
            case ClickGUI c           -> { return; }
            case BookScreen b         -> { return; }
            case BookEditScreen b     -> { return; }
            case SignEditScreen s     -> { return; }
            default -> {}
        }

        GameOptions opt = RubyClient.client.options;

        opt.forwardKey.setPressed(InputUtils.isKeyPressed(opt.forwardKey));
        opt.backKey.setPressed(InputUtils.isKeyPressed(opt.backKey));
        opt.leftKey.setPressed(InputUtils.isKeyPressed(opt.leftKey));
        opt.rightKey.setPressed(InputUtils.isKeyPressed(opt.rightKey));
        if(this.jump.value()) opt.jumpKey.setPressed(InputUtils.isKeyPressed(opt.jumpKey));
        if(this.sneak.value()) opt.sneakKey.setPressed(InputUtils.isKeyPressed(opt.sneakKey));
        if(this.sprint.value()) opt.sprintKey.setPressed(InputUtils.isKeyPressed(opt.sprintKey));
    }
}
