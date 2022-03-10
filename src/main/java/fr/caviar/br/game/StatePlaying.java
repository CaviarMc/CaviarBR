package fr.caviar.br.game;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatePlaying extends GameState {
	
	public StatePlaying(GameManager game) {
		super(game);
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
		return false;
	}
	
}
