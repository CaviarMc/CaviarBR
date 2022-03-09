package fr.caviar.br.game;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class GameState implements Listener {
	
	protected final GameManager game;
	
	public GameState(GameManager game) {
		this.game = game;
	}
	
	public GameManager getGame() {
		return game;
	}
	
	public void start() {
		game.getPlugin().getLogger().info("Starting state " + getClass().getSimpleName());
		Bukkit.getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public void end() {
		game.getPlugin().getLogger().info("Ending state " + getClass().getSimpleName());
		HandlerList.unregisterAll(this);
	}

}
