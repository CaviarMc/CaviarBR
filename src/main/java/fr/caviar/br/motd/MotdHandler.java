package fr.caviar.br.motd;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.utils.Utils;

public class MotdHandler implements Listener {

	@EventHandler
	public void onServerListPing(ServerListPingEvent event) {
		event.setMotd("§2CaviarBR §a" + Utils.getPluginVersion(CaviarBR.getInstance()));
	}
}
