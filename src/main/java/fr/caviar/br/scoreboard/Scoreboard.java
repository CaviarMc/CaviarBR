package fr.caviar.br.scoreboard;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.commands.VanishCommand;
import fr.caviar.br.game.GameManager;
import fr.mrmicky.fastboard.FastBoard;

public class Scoreboard implements Listener {

	private final CaviarBR plugin;
	private boolean isEnabled = false;

	public Scoreboard(CaviarBR plugin) {
		this.plugin = plugin;
	}
	
	public void enable() {
		isEnabled = true;
		testIt();
		if (!isEnabled)
			return;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getServer().getOnlinePlayers().forEach(p -> create(p));
	}

	public void disable() {
		if (!isEnabled)
			return;
		HandlerList.unregisterAll(this);
		plugin.getServer().getOnlinePlayers().forEach(p -> delete(p));
		isEnabled = false;
	}
	
	public void testIt() {
		try {
			new FastBoard(null);
			plugin.getLogger().log(Level.INFO, "FastBoard didn't work as usual. Check it.");
			return;
		} catch (NullPointerException e) {
			return; // Normal
		} catch (Exception e) {
			plugin.getLogger().log(Level.SEVERE, "Can't start FastBoard. Custom NameTag has been deactivated.");
			e.printStackTrace();
			isEnabled = false;
		}
	}
	
	public void create(Player player) {
		FastBoard board = new FastBoard(player);
		board.updateTitle(ChatColor.AQUA + "CaviarBR");
		board.updateLines(
				ChatColor.GREEN + "Hello world",
				"",
				ChatColor.RED + "eu.caviar.com"
				);
	}	
	
	public void waitToStart(Player player) {
		FastBoard board = new FastBoard(player);
		GameManager game = plugin.getGame();
		int needPlayer = game.getSettings().getMinPlayers().get() - Bukkit.getOnlinePlayers().size();
		String linePlayerWait;
		if (needPlayer > 0) {
			linePlayerWait = ChatColor.YELLOW + "waiting " + needPlayer + " players";
		} else {
			linePlayerWait = ChatColor.YELLOW + String.valueOf(VanishCommand.getOnlineCount()) + " players onlines";
		}
		board.updateLines(
				linePlayerWait,
				"",
				ChatColor.RED + "eu.caviar.com"
				);
	}	
	
	public void compassTreasureWaiting(Player player, long timestampCompass) {
		FastBoard board = new FastBoard(player);
		GameManager game = plugin.getGame();
		board.updateLines(
				ChatColor.YELLOW + "Compass in " + timestampCompass,
				"",
				ChatColor.GREEN + "eu.caviar.com"
				);
	}
	
	public void compassEndEffest(Player player, long timestampCompass) {
		FastBoard board = new FastBoard(player);
		GameManager game = plugin.getGame();
		board.updateLines(
				ChatColor.RED + "Remove Compass in " + timestampCompass,
				"",
				ChatColor.GREEN + "eu.caviar.com"
				);
	}

	public void delete(Player player) {
		FastBoard board = new FastBoard(player);
		board.delete();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		create(player);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {

	}

}
