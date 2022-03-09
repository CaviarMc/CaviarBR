package fr.caviar.br.game;

import fr.caviar.br.CaviarBR;

public class GameManager {
	
	private final CaviarBR plugin;
	private final GameSettings settings;
	
	private GameState state;
	
	public GameManager(CaviarBR plugin) {
		this.plugin = plugin;
		settings = new GameSettings(this);
		setState(new StateWait(this));
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
	
	public void setState(GameState state) {
		if (this.state != null) this.state.end();
		this.state = state;
		state.start();
	}
	
	public void enable() {
		
	}
	
	public void disable() {
		
	}
	
}
