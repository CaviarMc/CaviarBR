package fr.caviar.br.game;

import org.bukkit.Location;

import fr.caviar.br.player.CaviarPlayerSpigot;

public class GamePlayer {
	
	public final CaviarPlayerSpigot player;
	
	public Location spawnLocation;
	public boolean started;
	
	public GamePlayer(CaviarPlayerSpigot player) {
		this.player = player;
	}
	
	public void setSpawnLocation(Location spawnpoint) {
		spawnLocation = spawnpoint.add(0.5, 1, 0.5);
		spawnLocation.setDirection(spawnLocation.toVector().multiply(-1).setY(0));
	}
	
}
