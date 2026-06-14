package ruby.mixin;

import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.commands.CommandSuggestions;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow protected TextFieldWidget chatField;
    @Shadow protected ChatInputSuggestor chatInputSuggestor;

    @Inject(method = "init", at = @At("TAIL"))
    private void ruby$refreshCommandSuggestions(CallbackInfo ci) {
        if(CommandSuggestions.isCommandInput(this.chatField.getText()))
            this.chatInputSuggestor.refresh();
    }
}
