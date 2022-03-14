package fr.caviar.br.game;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarStrings;

public class StateWin extends GameState {
	
	private GamePlayer winner;
	
	public StateWin(GameManager game, GamePlayer winner) {
		super(game);
		this.winner = winner;
	}
	
	@Override
	public void start() {
		super.start();
		
		CaviarStrings.STATE_WIN.broadcast(winner.player.getName());
	}
	
	@Override
	public void handleLogin(PlayerLoginEvent event, GamePlayer player) {
		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "The game has ended!");
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
		return false;
	}
	
}
