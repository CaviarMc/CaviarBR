package fr.caviar.br;

import java.io.File;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

public class CaviarBRTest extends JavaPlugin {

	private static CaviarBRTest INSTANCE;

	public CaviarBRTest() { super(); }

	protected CaviarBRTest(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) { super(loader, description, dataFolder, file); }

	@Override
	public void onLoad() {
		INSTANCE = this;

		sendMessage("§6%s§e (%s) est chargé.", getDescription().getName(), getDescription().getVersion());
		super.onLoad();
	}

	@Override
	public void onEnable() {
		PluginManager pluginManager = getServer().getPluginManager();

		sendMessage("§2%s§a (%s) est activé.", getDescription().getName(), getDescription().getVersion());
		super.onEnable();
	}

	@Override
	public void onDisable() {
		sendMessage("§4%s§c (%s) est désactivé.", getDescription().getName(), getDescription().getVersion());
		super.onDisable();
	}

	public void sendMessage(String message, Object... args) {
		getServer().getConsoleSender().sendMessage(String.format(message, args));
	}
}
