package fr.caviar.br;

import org.bukkit.plugin.PluginManager;

import fr.caviar.br.api.CaviarPlugin;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.nametag.Nametag;
import fr.caviar.br.player.listener.PlayerLoginListener;
import fr.caviar.br.scoreboard.Scoreboard;

public class CaviarBR extends CaviarPlugin {

	private static CaviarBR INSTANCE;

	private GameManager game;
	private CaviarCommands commands;
	private Nametag nameTag;
	private Scoreboard scoreboard;

	@Override
	public void onLoad() {
		INSTANCE = this;
		super.onLoad();

		commands = new CaviarCommands(this);
		game = new GameManager(this);
		nameTag = new Nametag(this);
		scoreboard = new Scoreboard(this);

		sendMessage("§6%s§e (%s) est chargé.", getDescription().getName(), getDescription().getVersion());
	}

	@Override
	public void onEnable() {
		super.onEnable();
		PluginManager pluginManager = getServer().getPluginManager();

		commands.enable();
		game.enable();
		nameTag.enable();
		scoreboard.enable();

		pluginManager.registerEvents(new PlayerLoginListener(), this);

		sendMessage("§2%s§a (%s) est activé.", getDescription().getName(), getDescription().getVersion());
	}

	@Override
	public void onDisable() {
		super.onDisable();
		commands.disable();
		game.disable();
		scoreboard.disable();
		nameTag.disable();

		sendMessage("§4%s§c (%s) est désactivé.", getDescription().getName(), getDescription().getVersion());
	}

	public CaviarCommands getCommands() {
		return commands;
	}

	public GameManager getGame() {
		return game;
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
}
