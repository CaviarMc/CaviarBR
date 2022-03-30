package fr.caviar.br.api;

import java.io.File;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitTask;

import fr.caviar.br.api.config.ConfigSpigot;
import fr.caviar.br.player.PlayerHandler;
import fr.caviar.br.task.TaskManagerSpigot;
import fr.caviar.br.task.UniversalTask;

public class CaviarPlugin extends JavaPlugin {

	private ConfigSpigot config;
	private ConfigSpigot playerConfig;
	private UniversalTask<BukkitTask> taskManager;
	private PlayerHandler playerHandler;

	protected CaviarPlugin() { super(); }

	protected CaviarPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) { super(loader, description, dataFolder, file); }

	public void sendMessage(String message, Object... args) {
		getServer().getConsoleSender().sendMessage(String.format(message, args));
	}

	@Override
	public void onLoad() {
		config = new ConfigSpigot(this, "config.yml", true);
		playerConfig = new ConfigSpigot(this, "players.yml", true);
		taskManager = new TaskManagerSpigot(this);
		playerHandler = new PlayerHandler(this);
		super.onLoad();
	}

	@Override
	public void onEnable() {
		config.load();
		playerConfig.load();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		ConfigSpigot.unloadAll();
		taskManager.cancelAllTasks();
		super.onDisable();
	}

	public PlayerHandler getPlayerHandler() {
		return playerHandler;
	}

	@Override
	public ConfigSpigot getConfig() {
		return config;
	}

	public ConfigSpigot getPlayerConfig() {
		return playerConfig;
	}

	public UniversalTask<BukkitTask> getTaskManager() {
		return taskManager;
	}
}
