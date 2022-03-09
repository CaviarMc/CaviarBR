package fr.caviar.br.player;

import org.bukkit.OfflinePlayer;

public class CaviarPlayerSpigot extends UniversalPlayer {

	private OfflinePlayer player;
	
	public CaviarPlayerSpigot(OfflinePlayer player) {
		super(player.getName(), player.getUniqueId());
		this.player = player;
	}

	public OfflinePlayer getPlayer() {
		return player;
	}

}