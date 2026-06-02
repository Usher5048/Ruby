package ruby;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ruby.plugins.ModelModifierPlugin;
import ruby.systems.commands.Commands;
import ruby.systems.config.ConfigManager;
import ruby.systems.config.Configuration;
import ruby.systems.events.Events;
import ruby.systems.events.render.Render2DEvent;
import ruby.systems.events.render.Render3DEvent;
import ruby.systems.events.tick.TickEvents;
import ruby.systems.gui.ClickGUI;
import ruby.systems.gui.LoadingOverlay;
import ruby.systems.modules.Modules;

import java.io.InputStream;

public class RubyClient implements ModInitializer {
	public static final String MOD_ID = "ruby";
	public static final String MOD_NAME = "Ruby";
	public static final String MC_VERSION = SharedConstants.getGameVersion().name();
	public static final String VERSION = "v" + FabricLoader
			.getInstance()
			.getModContainer(RubyClient.MOD_ID)
			.orElseThrow(() -> new RuntimeException("Wtf???"))
			.getMetadata()
			.getVersion()
			.getFriendlyString()
			.split("\\+")[0];

	public static final Logger LOGGER = LoggerFactory.getLogger(RubyClient.MOD_NAME);
	public static final MinecraftClient client = MinecraftClient.getInstance();
	public static final Configuration config = new Configuration();
	public static final KeyBinding openGUIKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.ruby.open_gui",
			GLFW.GLFW_KEY_RIGHT_SHIFT,
			KeyBinding.Category.create(Identifier.of(RubyClient.MOD_ID,  "ruby"))
	));

	public static InputStream getResourceStream(String path) {
		return RubyClient.class.getResourceAsStream(String.format(
				"/assets/%s/%s", RubyClient.MOD_ID, path
		));
	}

	public static void log(String message) {
		RubyClient.LOGGER.info(message);
	}
	public static void notifyUser(String message) {
		RubyClient.notifyUser(message, false);
	}
	public static void notifyUser(Text message) {
		RubyClient.notifyUser(message, false);
	}
	public static void notifyUser(String message, boolean actionBar) {
		RubyClient.notifyUser(Text.of(message), actionBar);
	}
	public static void notifyUser(Text message, boolean actionBar) {
		if(RubyClient.client.player == null) return;
		RubyClient.client.player.sendMessage(
				Text.empty()
						.append(!actionBar ? Text.literal("[").withColor(0x666666) : Text.empty())
						.append(!actionBar ? Text.literal(RubyClient.MOD_NAME).withColor(0xCC3366) : Text.empty())
						.append(!actionBar ? Text.literal("] ").withColor(0x666666) : Text.empty())
						.append(message),

				actionBar
		);
	}

	public static void loadClient(LoadingOverlay overlay) {
		overlay.log("Loaded " + Modules.getModules().size() + " modules");
		overlay.log("Loaded " + Commands.getCommands().size() + " commands");

		int configVer = ConfigManager.loadState();
		if(configVer != ConfigManager.VERSION_ERROR) overlay.log("Loaded client config (v" + configVer + ")");
		else overlay.log("Failed to load client configs, using default!", 0xFF3333);

		Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::saveState));
		overlay.log("Attached shutdown hook to runtime");

		Events.TICK.register(TickEvents.BEGIN, event -> {
			if(RubyClient.openGUIKey.wasPressed())
				RubyClient.client.setScreen(new ClickGUI());
		});

		overlay.log("Attached keybind listener to GUI");

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.MISC_OVERLAYS,
				Identifier.of(RubyClient.MOD_ID, "hud_render"),
				(context, tickCounter) -> Events.RENDER2D.fire(new Render2DEvent(context, tickCounter))
		);

		overlay.log("Registered 2D rendering event");

		WorldRenderEvents.END_MAIN.register(context -> Events.RENDER3D.fire(new Render3DEvent(
				0,
				context.gameRenderer().getBasicProjectionMatrix(RubyClient.client.options.getFov().getValue()),
				context.matrices().peek().getPositionMatrix(),
				context.matrices(),
				context.consumers()
		)));

		overlay.log("Registered 3D rendering event");

//		try { Thread.sleep(5000); } catch(Exception ignored) {}
	}

	@Override
	public void onInitialize() {
		ModelLoadingPlugin.register(new ModelModifierPlugin());
	}
}

