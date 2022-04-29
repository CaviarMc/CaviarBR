package fr.caviar.br.game;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.permission.Perm;

public class GameListener implements Listener {

	@EventHandler
	public void onPlayerPortal(PlayerPortalEvent event) {
		event.setCanCreatePortal(false);
		event.setCancelled(true);
		CaviarStrings.CANT_DO_THIS.send(event.getPlayer());
	}

	@EventHandler
	public void onEntityInteractEntity(PlayerInteractAtEntityEvent event) {
		if (event.getRightClicked() instanceof Villager v) {
			CaviarStrings.CANT_DO_THIS.send(event.getPlayer());
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		if ((event.getResult().equals(Result.KICK_FULL) || event.getResult().equals(Result.KICK_WHITELIST)) && Perm.STAFF_JOIN_FULL.has(event.getPlayer())) {
			event.setResult(Result.ALLOWED);
		}
	}
}
