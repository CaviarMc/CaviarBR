package fr.caviar.br.scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.commands.VanishCommand;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.utils.Utils;
import fr.mrmicky.fastboard.FastBoard;

public class Scoreboard implements Listener {

	private static final String prefix = ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "______________________________";
	private static final String subdomain = "eu";
	private static final String ip = ChatColor.GREEN + subdomain + ".caviarwrld.com";
	private final CaviarBR plugin;
	private boolean isEnabled = false;
	private Map<Player, FastBoard> scoreboards = new HashMap<>();

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
		scoreboards.clear();
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
	
	@Nonnull
	public FastBoard create(Player player) {
		FastBoard board = new FastBoard(player);
		board.updateTitle(ChatColor.AQUA + "CaviarBR");
		board.updateLines(
				prefix,
				"",
				ChatColor.GREEN + "Hello world",
				"",
				prefix,
				ip
				);
		scoreboards.put(player, board);
		return board;
	}	
	
	public void waitToStart(Player player) {
		FastBoard board = getBoard(player);
		GameManager game = plugin.getGame();
		int needPlayer = game.getSettings().getMinPlayers().get() - Bukkit.getOnlinePlayers().size();
		int online = VanishCommand.getOnlineCount();
		String linePlayerWait;
		if (needPlayer > 0) {
			String s = Utils.withOrWithoutS(needPlayer);
			linePlayerWait = ChatColor.YELLOW + "Waiting " + needPlayer + " player" + s;
		} else {
			linePlayerWait = ChatColor.YELLOW + "The game will start ...";
		}
		String s1 = Utils.withOrWithoutS(online);
		board.updateLines(
				prefix,
				"",
				linePlayerWait,
				"",
				ChatColor.YELLOW + String.valueOf(online) + " player" + s1 + " online" + s1,
				"",
				ChatColor.AQUA + "Version",
				ChatColor.AQUA + Utils.getPluginVersion(plugin),
				"",
				prefix,
				ip
				);
	}	
	
	public void treasureWaiting(Player player) {
		FastBoard board = getBoard(player);
		GameManager game = plugin.getGame();
		board.updateTitle(ChatColor.AQUA + "CaviarBR - In Game");
		board.updateLines(
				prefix,
				"",
				ChatColor.YELLOW + "Treasure in",
				ChatColor.YELLOW + Utils.hrFormatDuration(game.getTimestampTreasureSpawn()),
				"",
				ChatColor.YELLOW + "Compass in",
				ChatColor.YELLOW + Utils.hrFormatDuration(game.getTimestampNextCompass()),
				"",
				prefix,
				ip
				);
	}
	
	public void compassWaiting(Player player) {
		FastBoard board = getBoard(player);
		GameManager game = plugin.getGame();
		board.updateLines(
				prefix,
				"",
				ChatColor.YELLOW + "Compass in",
				ChatColor.YELLOW + Utils.hrFormatDuration(game.getTimestampNextCompass()),
				"",
				prefix,
				ip
				);
	}
	
	public void compassEndEffest(Player player) {
		FastBoard board = getBoard(player);
		GameManager game = plugin.getGame();
		board.updateLines(
				prefix,
				"",
				ChatColor.RED + "Remove Compass in",
				ChatColor.RED + Utils.hrFormatDuration(game.getTimestampCompassEnd()),
				"",
				prefix,
				ip
				);
	}
	
	public void delete(Player player) {
		FastBoard board = getBoard(player);
		board.delete();
	}
	
	public FastBoard getBoard(Player player) {
		FastBoard fb = scoreboards.get(player);
		if (fb == null) {
			fb = create(player);
			CaviarBR.getInstance().getLogger().warning(String.format("Scoreboad for %s didn't exist. We have created it but it should not happen.", player.getName()));
		}
		return fb;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		create(player);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		scoreboards.remove(player);
	}

}
