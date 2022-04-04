package fr.caviar.br.api;

import java.io.File;
import java.util.logging.Level;

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
	private boolean isPaper;
	private boolean isTunity;
	private boolean isPurpur;

	protected CaviarPlugin() { super(); }

	protected CaviarPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) { super(loader, description, dataFolder, file); }

	public void sendMessage(String message, Object... args) {
		getServer().getConsoleSender().sendMessage(String.format(message, args));
	}

	@Override
	public void onLoad() {
		this.config = new ConfigSpigot(this, "config.yml", true);
		this.playerConfig = new ConfigSpigot(this, "players.yml", true);
		this.taskManager = new TaskManagerSpigot(this);
		this.playerHandler = new PlayerHandler(this);
		super.onLoad();
	}

	@Override
	public void onEnable() {
		config.load();
		playerConfig.load();
		super.onEnable();
		try {
			isPaper = Class.forName("com.destroystokyo.paper.VersionHistoryManager$VersionData") != null;
		} catch (ClassNotFoundException e) {
			isPaper = false;
		}
		try {
			isTunity = Class.forName("com.tuinity.tuinity.util.TickThread") != null;
		} catch (ClassNotFoundException e) {
			isTunity = false;
		}
		try {
			isPurpur = Class.forName("net.pl3x.purpur.event.PlayerAFKEvent") != null;
		} catch (ClassNotFoundException e) {
			isPurpur = false;
		}
		this.getLogger().log(Level.INFO, String.format("Detected %s as server framework.", getVersionBukkit()));
		if (!isPaper) {
			this.getLogger().log(Level.SEVERE, "You sould use PaperSpigot instead of Spigot : https://papermc.io/downloads");
		}
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

	public boolean isPaper() {
		return isPaper;
	}

	public String getVersionBukkit() {
		return isPaper ? isTunity ? isPurpur ? "Purpur" : "Tunity" : "PaperSpigot" : "Spigot";
	}
}
