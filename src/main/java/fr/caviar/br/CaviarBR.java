package fr.caviar.br;

import org.bukkit.plugin.PluginManager;

import fr.caviar.br.api.CaviarPlugin;
import fr.caviar.br.commands.ConfigCommand;
import fr.caviar.br.commands.VanishCommand;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.nametag.Nametag;
import fr.caviar.br.player.listener.PlayerLoginListener;
import fr.caviar.br.scoreboard.Scoreboard;
import fr.caviar.br.utils.Utils;

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

		sendMessage("§6%s§e (%s) est chargé.", getDescription().getName(), Utils.getPluginVersion(this));
	}

	@Override
	public void onEnable() {
		super.onEnable();
		PluginManager pluginManager = getServer().getPluginManager();

		pluginManager.registerEvents(new PlayerLoginListener(), this);
		commands.enable();
		game.enable();
		scoreboard.enable();
		nameTag.enable();
		new ConfigCommand(this);
		new VanishCommand(this);


		sendMessage("§2%s§a (%s) est activé.", getDescription().getName(), Utils.getPluginVersion(this));
	}

	@Override
	public void onDisable() {
		// Note: Configs is not unload
		super.onDisable();
		// Note: Configs is already unload
		commands.disable();
		game.disable();
		scoreboard.disable();
		nameTag.disable();

		sendMessage("§4%s§c (%s) est désactivé.", getDescription().getName(), Utils.getPluginVersion(this));
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
