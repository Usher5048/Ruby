package ruby.systems.modules.movement;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
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
        switch(RubyClient.client.gui.screen()) {
            case null                 -> { return; }
            case ChatScreen c         -> { return; }
            case ClickGUI c           -> { return; }
            case BookViewScreen b         -> { return; }
            case BookEditScreen b     -> { return; }
            case SignEditScreen s     -> { return; }
            default -> {}
        }

        Options opt = RubyClient.client.options;

        opt.keyUp.setDown(InputUtils.isKeyPressed(opt.keyUp));
        opt.keyDown.setDown(InputUtils.isKeyPressed(opt.keyDown));
        opt.keyLeft.setDown(InputUtils.isKeyPressed(opt.keyLeft));
        opt.keyRight.setDown(InputUtils.isKeyPressed(opt.keyRight));
        if(this.jump.value()) opt.keyJump.setDown(InputUtils.isKeyPressed(opt.keyJump));
        if(this.sneak.value()) opt.keyShift.setDown(InputUtils.isKeyPressed(opt.keyShift));
        if(this.sprint.value()) opt.keySprint.setDown(InputUtils.isKeyPressed(opt.keySprint));
    }
}
