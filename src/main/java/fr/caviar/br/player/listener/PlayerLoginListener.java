package fr.caviar.br.player.listener;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.player.CaviarPlayerSpigot;
import fr.caviar.br.player.PlayerHandler;
import fr.caviar.br.utils.ColorUtils;

public class PlayerLoginListener implements Listener {

	static {
		Bukkit.getOnlinePlayers().forEach(p -> {
			PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
			playerHandler.get(p.getUniqueId(), (cPlayer, e) -> {
				if (e != null)
					e.printStackTrace();
				else if (cPlayer == null) {
					cPlayer = playerHandler.createPlayer(p);
					CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("onLOAD: New player creation : name = %s uuid = %s", cPlayer.getName(), cPlayer.getUuid()));
				} else {
					CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("onLOAD: Succes load player from cache : name = %s uuid = %s group = %s", cPlayer.getName(), cPlayer.getUuid(), cPlayer.getGroup()));
				}
			});
		});
	}
	
	@EventHandler
	public void on1PlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		UUID uuid = event.getUniqueId();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		playerHandler.getObjectNotCached(uuid, (cPlayer, exception) -> {
			if (cPlayer == null)
				CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("NEW PLAYER : %s wait PlayerJoinEvent to be created ...", uuid));
			else
				CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("Succes load player : name = %s uuid = %s group = %s", cPlayer.getName(), cPlayer.getUuid(), cPlayer.getGroup()));
		});
	}
	
//	@EventHandler
//	public void on2PlayerLogin(PlayerLoginEvent event) {
//		Player player = event.getPlayer();
//		CaviarBR main = CaviarBR.getInstance();
//		PlayerHandler playerHandler = main.getPlayerHandler();
//		CaviarPlayerSpigot uPlayer =  playerHandler.getObjectCached(player.getUniqueId());
//	}


	@EventHandler(priority = EventPriority.LOWEST)
	public void on3PlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		CaviarPlayerSpigot cPlayer = playerHandler.getObjectCached(player.getUniqueId());
		if (cPlayer == null) {
			cPlayer = playerHandler.createPlayer(player);
			CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("New player creation : name = %s uuid = %s", cPlayer.getName(), cPlayer.getUuid()));
		} else {
			CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("Succes load player from cache : name = %s uuid = %s group = %s", cPlayer.getName(), cPlayer.getUuid(), cPlayer.getGroup()));
		}
	}

	@SuppressWarnings("deprecation")
//	@EventHandler(priority = EventPriority.HIGH)
	@EventHandler
	public void on4PlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		CaviarPlayerSpigot uPlayer =  playerHandler.getObjectCached(player.getUniqueId());
		if (uPlayer == null)
			return ;
		event.setJoinMessage(ColorUtils.format("&7[&a+&7] %s", player.getName()));
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
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
