package ruby.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ruby.RubyClient;
import ruby.systems.commands.Command;
import ruby.systems.commands.Commands;

import java.util.Arrays;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
	public void onChatMessage(String message, CallbackInfo info) {
		String chatPrefix = RubyClient.chatPrefix.value().toLowerCase();
		if(!message.toLowerCase().startsWith(chatPrefix)) return;

		info.cancel();
		RubyClient.client.inGameHud.getChatHud().addToMessageHistory(message);

		String[] args = message.substring(chatPrefix.length()).split(" ");
		String command = args[0].toLowerCase();
		args = Arrays.copyOfRange(args, 1, args.length);

		Command cmd = Commands.getByName(command);
		if(cmd == null) RubyClient.notifyUser("Unknown command!");
		else {
			RubyClient.notifyUser(command);
			cmd.execute(args);
		}
	}
}