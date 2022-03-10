package fr.caviar.br.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatePlaying extends GameState {
	
	public StatePlaying(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		Bukkit.getOnlinePlayers().forEach(player -> {
			join(player, game.getPlayers().get(player.getUniqueId()));
		});
	}
	
	private void join(Player player, GamePlayer gamePlayer) {
		if (!gamePlayer.teleported) {
			gamePlayer.teleported = true;
			if (gamePlayer.spawnLocation == null) {
				game.getPlugin().getLogger().severe("No spawn location for player " + player.getName());
			}else {
				player.teleport(gamePlayer.spawnLocation);
			}
		}
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {
		join(event.getPlayer(), player);
	}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
		return false;
	}
	
}
