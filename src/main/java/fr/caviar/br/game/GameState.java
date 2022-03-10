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
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.player.CaviarPlayerSpigot;

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
	
	public boolean areNewPlayersAllowed() {
		return false;
	}
	
	public abstract void onJoin(PlayerJoinEvent event, GamePlayer player);
	
	public abstract boolean onQuit(PlayerQuitEvent event, GamePlayer player);

	@EventHandler (priority = EventPriority.HIGH)
	public final void onLogin(PlayerLoginEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		GamePlayer gamePlayer = game.getPlayers().get(uuid);
		CaviarPlayerSpigot caviarPlayer = game.getPlugin().getPlayerHandler().getObjectCached(uuid);
		Validate.notNull(caviarPlayer);
		
		if (gamePlayer == null) {
			if (areNewPlayersAllowed()) {
				gamePlayer = new GamePlayer(caviarPlayer);
				game.getPlayers().put(uuid, gamePlayer);
			}else {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§cYou cannot join the game yet.");
				return;
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public final void onJoin(PlayerJoinEvent event) {
		GamePlayer gamePlayer = game.getPlayers().get(event.getPlayer().getUniqueId());
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
