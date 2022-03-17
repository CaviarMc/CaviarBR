package fr.caviar.br.player.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.player.CaviarPlayerSpigot;
import fr.caviar.br.player.PlayerHandler;
import fr.caviar.br.utils.ColorUtils;

public class PlayerLoginListener implements Listener {

	@EventHandler
	public void on1PlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		UUID uuid = event.getUniqueId();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		playerHandler.get(uuid, (uPlayer, exception) -> {
//			System.out.println("Uuid : " + uPlayer.getUuid());
//			System.out.println("Group : " + uPlayer.getGroup());
		}); // TODO remove runTaskAsync
	}
	
	@EventHandler
	public void on2PlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		CaviarBR main = CaviarBR.getInstance();
		PlayerHandler playerHandler = main.getPlayerHandler();
		CaviarPlayerSpigot uPlayer =  playerHandler.getObjectCached(player.getUniqueId());
		if (uPlayer == null)
			uPlayer = playerHandler.createPlayer(player);
		main.getNameTag().updatePlayer(player);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void on3PlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		CaviarPlayerSpigot uPlayer =  playerHandler.getObjectCached(player.getUniqueId());
		if (uPlayer == null)
			return ;
		event.setJoinMessage(ColorUtils.format("&7[&a+&7] %s", player.getName()));
		
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		event.setQuitMessage(ColorUtils.format("&7[&c-&7] %s", player.getName()));
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onPlayerQuitHigh(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		playerHandler.get(player.getUniqueId(), (uPlayer, exception) -> {
			if (uPlayer == null)
				return;
			playerHandler.savePlayer(uPlayer);
			playerHandler.removePlayer(uPlayer);
		});
	}
}
