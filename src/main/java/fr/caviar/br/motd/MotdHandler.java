package fr.caviar.br.motd;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import fr.caviar.br.CaviarBR;

public class MotdHandler implements Listener {

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onServerListPing(ServerListPingEvent event) {
		event.setMotd("§2CaviarBR §a" + CaviarBR.getInstance().getVersion());
	}
}
