package fr.caviar.br;

import java.io.File;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import fr.caviar.br.config.ConfigSpigot;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.nametag.Nametag;
import fr.caviar.br.player.PlayerHandler;
import fr.caviar.br.player.listener.PlayerLoginListener;
import fr.caviar.br.scorebard.Scoreboard;
import fr.caviar.br.task.TaskManagerSpigot;
import fr.caviar.br.task.UniversalTask;

public class CaviarBR extends JavaPlugin {

	private static CaviarBR INSTANCE;

	private GameManager game;
	private ConfigSpigot config;
	private PlayerHandler playerHandler;
	private UniversalTask taskManager;
	private Nametag nameTag;
	private Scoreboard scoreboard;

	public CaviarBR() { super(); }

	protected CaviarBR(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) { super(loader, description, dataFolder, file); }

	@Override
	public void onLoad() {
		INSTANCE = this;
		game = new GameManager(this);
		taskManager = new TaskManagerSpigot(this);
		playerHandler = new PlayerHandler();
		config = new ConfigSpigot(this, "config.yml");
		nameTag = new Nametag(this);
		scoreboard = new Scoreboard(this);

		sendMessage("§6%s§e (%s) est chargé.", getDescription().getName(), getDescription().getVersion());
		super.onLoad();
	}

	@Override
	public void onEnable() {
		PluginManager pluginManager = getServer().getPluginManager();

		game.enable();
		config.load();
		nameTag.enable();
		scoreboard.enable();

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

	public Nametag getNameTag() {
		return nameTag;
	}

	public Scoreboard getScoreboard() {
		return scoreboard;
	}

	public static CaviarBR getInstance() {
		return INSTANCE;
	}

	public void sendMessage(String message, Object... args) {
		getServer().getConsoleSender().sendMessage(String.format(message, args));
	}
}
