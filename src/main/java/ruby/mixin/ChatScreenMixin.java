package ruby.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.commands.CommandSuggestions;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow protected EditBox input;
    @Shadow private net.minecraft.client.gui.components.CommandSuggestions commandSuggestions;

    @Inject(method = "init", at = @At("TAIL"))
    private void ruby$refreshCommandSuggestions(CallbackInfo ci) {
        if(CommandSuggestions.isCommandInput(this.input.getValue()))
            this.commandSuggestions.updateCommandInfo();
    }
}
