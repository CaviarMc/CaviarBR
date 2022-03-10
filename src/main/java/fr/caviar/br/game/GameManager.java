package fr.caviar.br.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import fr.caviar.br.CaviarBR;

public class GameManager {
	
	private final CaviarBR plugin;
	private final GameSettings settings;
	private final Map<UUID, GamePlayer> players = new HashMap<>();
	
	private GameState state;
	
	private World world;
	
	public GameManager(CaviarBR plugin) {
		this.plugin = plugin;
		settings = new GameSettings(this);
	}
	
	public CaviarBR getPlugin() {
		return plugin;
	}
	
	public GameSettings getSettings() {
		return settings;
	}
	
	public GameState getState() {
		return state;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Map<UUID, GamePlayer> getPlayers() {
		return players;
	}
	
	public void setState(GameState state) {
		if (this.state != null) this.state.end();
		this.state = state;
		state.start();
	}
	
	public void enable() {
		setState(new StateWait(this));
	}
	
	public void disable() {
		
	}
	
}
