package fr.caviar.br.game;

public class GameManager {
	
	private GameState state;
	
	public GameManager() {
		state = GameState.WAIT;
	}
	
	public GameState getState() {
		return state;
	}
	
	public void enable() {
		
	}
	
	public void disable() {
		
	}
	
}
