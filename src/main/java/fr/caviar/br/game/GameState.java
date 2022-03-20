package fr.caviar.br.game;

import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.player.CaviarPlayerSpigot;

public abstract class GameState implements Listener {
	
	protected final GameManager game;
	
	private boolean running = false;
	
	public GameState(GameManager game) {
		this.game = game;
	}
	
	public GameManager getGame() {
		return game;
	}
	
	public void start() {
		running = true;
		game.getPlugin().getLogger().info("Starting state " + getClass().getSimpleName());
		Bukkit.getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	public void end() {
		running = false;
		game.getPlugin().getLogger().info("Ending state " + getClass().getSimpleName());
		HandlerList.unregisterAll(this);
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void handleLogin(PlayerLoginEvent event, GamePlayer player) {
		if (player == null) { // disallows players who have never connected
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, CaviarStrings.LOGIN_SCREEN_STARTED_KICK.toComponent());
		}
	}
	
	public abstract void onJoin(PlayerJoinEvent event, GamePlayer player);
	
	public abstract boolean onQuit(PlayerQuitEvent event, GamePlayer player);

	@EventHandler (priority = EventPriority.HIGH)
	public final void onLogin(PlayerLoginEvent event) {
		if (event.getResult() != Result.ALLOWED) return;
		
		UUID uuid = event.getPlayer().getUniqueId();
		GamePlayer gamePlayer = game.getPlayers().get(uuid);
		CaviarPlayerSpigot caviarPlayer = game.getPlugin().getPlayerHandler().getObjectCached(uuid);
		
		Validate.notNull(caviarPlayer);
		
		handleLogin(event, gamePlayer);
		if (event.getResult() == Result.ALLOWED) {
			if (gamePlayer == null) {
				gamePlayer = new GamePlayer(caviarPlayer);
				game.getPlayers().put(uuid, gamePlayer);
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public final void onJoin(PlayerJoinEvent event) {
		GamePlayer gamePlayer = game.getPlayers().get(event.getPlayer().getUniqueId());

		// @Tristiisch Temp fix for fakePlayer
		CaviarPlayerSpigot caviarPlayer = game.getPlugin().getPlayerHandler().getObjectCached(event.getPlayer().getUniqueId());
		if (caviarPlayer != null && gamePlayer == null) {
			gamePlayer = new GamePlayer(caviarPlayer);
			game.getPlayers().put(event.getPlayer().getUniqueId(), gamePlayer);
		}
		// end Temp Fix

		Validate.notNull(gamePlayer);
		onJoin(event, gamePlayer);
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public final void onQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		GamePlayer gamePlayer = game.getPlayers().get(uuid);
		Validate.notNull(gamePlayer);
		if (onQuit(event, gamePlayer)) game.getPlayers().remove(uuid);
	}
	
}
