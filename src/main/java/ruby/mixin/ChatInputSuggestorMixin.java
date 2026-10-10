package ruby.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.systems.commands.CommandSuggestions;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.EditBox;

@Mixin(net.minecraft.client.gui.components.CommandSuggestions.class)
public abstract class ChatInputSuggestorMixin {
    @Shadow @Final EditBox input;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void ruby$commandSuggestions(CallbackInfo ci) {
        String text = this.input.getValue();
        if(!CommandSuggestions.isCommandInput(text)) return;

        Suggestions suggestions = CommandSuggestions.wrap(text, this.input.getCursorPosition());
        if(suggestions.getList().isEmpty()) return;

        this.pendingSuggestions = CompletableFuture.completedFuture(suggestions);
        this.showSuggestions(false);
        ci.cancel();
    }
}
