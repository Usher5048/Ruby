package ruby;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruby.plugins.ModelModifierPlugin;
import ruby.systems.bypasses.Bypasses;
import ruby.systems.accounts.AccountsManager;
import ruby.systems.commands.Commands;
import ruby.systems.config.ConfigManager;
import ruby.systems.config.Configuration;
import ruby.systems.config.ProfileManager;
import ruby.systems.config.StringValue;
import ruby.systems.events.Events;
import ruby.systems.events.Render2DEvent;
import ruby.systems.events.TickEvents;
import ruby.systems.gui.ClickGUI;
import ruby.systems.gui.Toasts;
import ruby.systems.gui.LoadingOverlay;
import ruby.systems.modules.Modules;
import ruby.systems.modules.render.Freecam;

import java.io.InputStream;

public class RubyClient implements ModInitializer {
	public static final String MOD_ID = "ruby";
	public static final String MOD_NAME = "Ruby";
	public static final String MC_VERSION = SharedConstants.getCurrentVersion().name();
	public static final String VERSION = "v" + FabricLoader
			.getInstance()
			.getModContainer(RubyClient.MOD_ID)
			.orElseThrow(() -> new RuntimeException("Wtf???"))
			.getMetadata()
			.getVersion()
			.getFriendlyString()
			.split("\\+")[0];

	public static final Logger LOGGER = LoggerFactory.getLogger(RubyClient.MOD_NAME);
	public static final Minecraft client = Minecraft.getInstance();
	public static final Configuration config = new Configuration();
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, "ruby"));
	public static final KeyMapping openGUIKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.ruby.open_gui",
			InputConstants.KEY_RSHIFT,
			RubyClient.KEY_CATEGORY
	));
	public static final KeyMapping openCommandKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.ruby.command",
			InputConstants.KEY_PERIOD,
			RubyClient.KEY_CATEGORY
	));

	public static final StringValue chatPrefix = RubyClient.config.create(new StringValue.Builder("Chat Prefix")
			.description("The prefix for chat commands")
			.defaultValue(".")
			.build());

	public static InputStream getResourceStream(String path) {
		return RubyClient.class.getResourceAsStream(String.format(
				"/assets/%s/%s", RubyClient.MOD_ID, path
		));
	}

	public static void log(String message) {
		RubyClient.LOGGER.info(message);
	}
	public static Identifier identifier(String value) {
		return Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, value);
	}
	public static void loadClient(LoadingOverlay overlay) {
		overlay.log("Loaded " + Modules.getModules().size() + " modules");
		overlay.log("Loaded " + Commands.getCommands().size() + " commands");

		int configVer = ConfigManager.loadState();
		if(configVer != ConfigManager.VERSION_ERROR) overlay.log("Loaded client configs (v" + configVer + ")");
		else overlay.log("Failed to load client configs, using default!", 0xFF3333);

		AccountsManager.load();
		AccountsManager.loginLast();
		overlay.log("Loaded " + AccountsManager.getAccounts().size() + " accounts");
		overlay.log("Loaded profile: '" + ProfileManager.activeProfile() + "'");

		Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::saveState));
		overlay.log("Attached shutdown hook to runtime");

		Bypasses.get().init();
		Events.TICK.register(TickEvents.BEGIN, event -> {
			Bypasses.get().tick();
			if(RubyClient.openGUIKey.consumeClick()) {
				if(RubyClient.client.gui.screen() instanceof ClickGUI clickGui) clickGui.requestClose();
				else if(RubyClient.client.gui.screen() == null) RubyClient.openClickGui();
			}

			if(RubyClient.openCommandKey.isDown())
				RubyClient.client.gui.setScreen(new ChatScreen(RubyClient.chatPrefix.value(), false));
		});

		overlay.log("Attached keybind listener to GUI");

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.MISC_OVERLAYS,
				Identifier.fromNamespaceAndPath(RubyClient.MOD_ID, "hud_render"),
				(context, tickCounter) -> Events.RENDER2D.fire(new Render2DEvent(context, tickCounter))
		);

		overlay.log("Registered 2D rendering event");
	}

	public static void notifyUser(String message) {
		Toasts.toast(message).show();
	}
	public static void notifyUser(Component m, boolean inChat) {
		if(!inChat) Toasts.toast(m.getString()).show();
		else if(RubyClient.client.player != null) {
			Component chatDisplay = Component.empty()
					.append(Component.literal("[").withColor(0x666666))
					.append(Component.literal(RubyClient.MOD_NAME).withColor(0xCC3366))
					.append(Component.literal("] ").withColor(0x666666))
					.append(m);

			RubyClient.client.player.sendSystemMessage(chatDisplay);
		} else RubyClient.log("Attempt to notify missing player: `" + m.getString() + "`");
	}

	public static void openClickGui() {
		Freecam freecam = Modules.getByClass(Freecam.class);
		if (freecam != null && freecam.enabled()) freecam.onScreenOpen();
		RubyClient.client.gui.setScreen(new ClickGUI());
	}

	@Override
	public void onInitialize() {
		ModelLoadingPlugin.register(new ModelModifierPlugin());
	}
}
