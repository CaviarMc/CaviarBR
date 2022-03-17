package fr.caviar.br.scoreboard;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import fr.mrmicky.fastboard.FastBoard;

public class Scoreboard implements Listener {

	private final Plugin plugin;
	private boolean isEnabled = false;

	public Scoreboard(Plugin plugin) {
		this.plugin = plugin;
	}

	public void enable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		isEnabled = true;
		testIt();
		if (isEnabled)
			plugin.getServer().getOnlinePlayers().forEach(p -> create(p));
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

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		create(player);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {

	}

}
