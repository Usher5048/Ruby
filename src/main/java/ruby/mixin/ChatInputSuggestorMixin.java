package ruby.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.commands.CommandSuggestions;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
    @Shadow @Final TextFieldWidget textField;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    public abstract void show(boolean narrateFirstSuggestion);

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void ruby$commandSuggestions(CallbackInfo ci) {
        String text = this.textField.getText();
        if(!CommandSuggestions.isCommandInput(text)) return;

        Suggestions suggestions = CommandSuggestions.wrap(text, this.textField.getCursor());
        if(suggestions.getList().isEmpty()) return;

        this.pendingSuggestions = CompletableFuture.completedFuture(suggestions);
        this.show(false);
        ci.cancel();
    }
}
