package fr.caviar.br.commands;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandExecutor;
import fr.caviar.br.CaviarBR;
import fr.caviar.br.CaviarStrings;

public class VanishCommand implements Listener {
	
	private CommandAPICommand command;
	private CaviarBR plugin;
	private static Set<Player> vanishPlayer = new HashSet<>();
	
	public VanishCommand(CaviarBR plugin) {
		this.plugin = plugin;
		command = new CommandAPICommand("vanish").withPermission("caviarbr.command.vanish").executes((CommandExecutor) (sender, args) -> {
			if (sender instanceof Player player)  {
				toggleVanishPlayer(player);
			} else {
				CaviarStrings.COMMAND_NO_CONSOLE.send(sender);
			}
		});
		plugin.getCommands().registerCommand(command);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public static int getOnlineCount() {
		return (int) Bukkit.getOnlinePlayers().stream().filter(p -> !vanishPlayer.contains(p)).count();
	}
	
	private void toggleVanishPlayer(Player player) {
		if (player.hasMetadata("vanish.caviar")) {
			unVanishPlayer(player);
		} else {
			vanishPlayer(player);
		}
	}
	
	private void vanishPlayer(Player player) {
		player.setMetadata("vanish.caviar", new FixedMetadataValue(plugin, true));
		Bukkit.getOnlinePlayers().forEach(p -> {
			if (!p.hasPermission("caviarbr.vanish.show"))
				p.hidePlayer(plugin, player);
		});
		vanishPlayer.add(player);
		player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
		player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
		CaviarStrings.VANISH_ON.send(player);
	}
	
	private void unVanishPlayer(Player player) {
		vanishPlayer.remove(player);
		player.removeMetadata("vanish.caviar", plugin);
		Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(plugin, player));
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		player.removePotionEffect(PotionEffectType.NIGHT_VISION);
		CaviarStrings.VANISH_OFF.send(player);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.hasMetadata("vanish.caviar")) {
			vanishPlayer(player);
		}
		if (!player.hasPermission("caviarbr.vanish.show")) {
			vanishPlayer.forEach(playerVanish -> player.hidePlayer(plugin, playerVanish));
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		player.removePotionEffect(PotionEffectType.NIGHT_VISION);
		vanishPlayer.remove(player);
	}
}
