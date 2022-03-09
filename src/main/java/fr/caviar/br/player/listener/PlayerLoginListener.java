package fr.caviar.br.player.listener;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.player.PlayerHandler;

public class PlayerLoginListener implements Listener {
	
	@EventHandler
	public void on1PlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		UUID uuid = event.getUniqueId();
		PlayerHandler playerHandler = CaviarBR.getInstance().getPlayerHandler();
		playerHandler.get(uuid, null);
	}

}
