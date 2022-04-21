package fr.caviar.br.scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
	private static final List<String> top = List.of(prefix, "");
	private static final List<String> bottom = List.of("", prefix, ip);
	private static List<String> debug;

	private final CaviarBR plugin;
	private boolean isEnabled = false;
	private Map<Player, FastBoard> scoreboards = new HashMap<>();

	public Scoreboard(CaviarBR plugin) {
		this.plugin = plugin;
		
		debug = List.of("",
				ChatColor.AQUA + "Version",
				ChatColor.AQUA + Utils.getPluginVersion(plugin));
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
			plugin.getLogger().warning("FastBoard didn't work as usual. Check it.");
			return;
		} catch (NullPointerException e) {
			return; // Normal
		} catch (Exception e) {
			plugin.getLogger().severe("Can't start FastBoard. Custom NameTag has been deactivated.");
			e.printStackTrace();
			isEnabled = false;
		}
	}
	
	@Nonnull
	public FastBoard create(Player player) {
		FastBoard board = new FastBoard(player);
		board.updateTitle(ChatColor.AQUA + "CaviarBR");
		updateBoard(board,
				ChatColor.GREEN + "Hello world"
			);
		scoreboards.put(player, board);
		return board;
	}	
	
	public void waitToStart(Player player) {
		FastBoard board = getBoard(player);
		board.updateTitle(ChatColor.AQUA + "CaviarBR");
		GameManager game = plugin.getGame();
		int minToStart = game.getSettings().getMinPlayers().get();
		int maxPlayer = game.getSettings().getMaxPlayers().get();
		int online = VanishCommand.getOnlineCount();
		if (online < minToStart) {
			maxPlayer = minToStart;
		}
		if (game.getSettings().isDebug().get()) {
			updateBoard(board,
				String.format("%s%d/%d players", ChatColor.YELLOW, online, maxPlayer),
				"",
				game.getWorldLoader().getStatus()
			);
		} else
			updateBoard(board,
				String.format("%s%d/%d players", ChatColor.YELLOW, online, maxPlayer)
			);
	}	
	
	public void treasureWaiting(Player player) {
		FastBoard board = getBoard(player);
		GameManager game = plugin.getGame();
		board.updateTitle(ChatColor.AQUA + "CaviarBR - In Game");
		updateBoard(board,
				ChatColor.YELLOW + "Treasure in",
				ChatColor.YELLOW + Utils.hrFormatDuration(game.getTimestampTreasureSpawn()),
				"",
				ChatColor.YELLOW + "Compass in",
				ChatColor.YELLOW + Utils.hrFormatDuration(game.getTimestampNextCompass())
			);
	}
	
	public void compassWaiting(Player player) {
		FastBoard board = getBoard(player);
		GameManager game = plugin.getGame();
		board.updateTitle(ChatColor.AQUA + "CaviarBR - In Game");
		updateBoard(board,
				ChatColor.YELLOW + "Compass in",
				ChatColor.YELLOW + Utils.hrFormatDuration(game.getTimestampNextCompass())
			);
	}
	
	public void compassEndEffest(Player player) {
		FastBoard board = getBoard(player);
		GameManager game = plugin.getGame();
		board.updateTitle(ChatColor.AQUA + "CaviarBR - In Game");
		updateBoard(board,
				ChatColor.RED + "Remove Compass in",
				ChatColor.RED + Utils.hrFormatDuration(game.getTimestampCompassEnd())
			);
	}
	
	private void updateBoard(FastBoard board, String... lines) {
		List<String> newLines = new ArrayList<>(15);
		newLines.addAll(top);
		for (String l : lines) {
			newLines.add(l);
		}
		if (plugin.getGame().getSettings().isDebug().get()) {
			newLines.addAll(debug);
		}
		newLines.addAll(bottom);
		board.updateLines(newLines);
	}

	public void delete(Player player) {
		FastBoard fb = scoreboards.remove(player);
		fb.delete();
	}
	
	private FastBoard getBoard(Player player) {
		FastBoard fb = scoreboards.get(player);
		if (fb == null) {
			fb = create(player);
			if (fb == null)
				throw new RuntimeException(String.format("Can't create scoreboard for %s.", player.getName()));
			CaviarBR.getInstance().getLogger().warning(String.format("Scoreboad for %s didn't exist. We have created it but it should not happen.", player.getName()));
		}
		return fb;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		create(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		delete(player);
	}

}
