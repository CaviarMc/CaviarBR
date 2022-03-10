package fr.caviar.br.game;

import org.bukkit.Location;

import fr.caviar.br.player.CaviarPlayerSpigot;

public class GamePlayer {
	
	public final CaviarPlayerSpigot player;
	
	public Location spawnLocation;
	public boolean teleported;
	
	public GamePlayer(CaviarPlayerSpigot player) {
		this.player = player;
	}
	
}
