package fr.caviar.br;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.caviar.br.config.ConfigSpigot;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.player.PlayerHandler;
import fr.caviar.br.player.listener.PlayerLoginListener;
import fr.caviar.br.task.TaskManagerSpigot;
import fr.caviar.br.task.UniversalTask;

public class CaviarBR extends JavaPlugin {

	private static CaviarBR INSTANCE;
	
	private GameManager game;
	private ConfigSpigot config;
	private PlayerHandler playerHandler;
	private UniversalTask taskManager;
	
	@Override
	public void onLoad() {
		INSTANCE = this;
		game = new GameManager(this);
		taskManager = new TaskManagerSpigot(this);
		playerHandler = new PlayerHandler();
		config = new ConfigSpigot(this, "config.yml");
		
		sendMessage("§6%s§e (%s) est chargé.", getDescription().getName(), getDescription().getVersion());
		super.onLoad();
	}
	
	@Override
	public void onEnable() {
		PluginManager pluginManager = getServer().getPluginManager();

		game.enable();
		config.load();

		pluginManager.registerEvents(new PlayerLoginListener(), this);
		
		sendMessage("§2%s§a (%s) est activé.", getDescription().getName(), getDescription().getVersion());
		super.onEnable();
	}

	@Override
	public void onDisable() {
		game.disable();
		
		sendMessage("§4%s§c (%s) est désactivé.", getDescription().getName(), getDescription().getVersion());
		super.onDisable();
	}
	
	public GameManager getGame() {
		return game;
	}

	public UniversalTask getTaskManager() {
		return taskManager;
	}

	@Override
	public ConfigSpigot getConfig() {
		return config;
	}

	public PlayerHandler getPlayerHandler() {
		return playerHandler;
	}

	public static CaviarBR getInstance() {
		return INSTANCE;
	}

	public void sendMessage(String message, Object... args) {
		getServer().getConsoleSender().sendMessage(String.format(message, args));
	}
}
