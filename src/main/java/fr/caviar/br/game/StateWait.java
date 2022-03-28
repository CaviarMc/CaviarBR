package fr.caviar.br.game;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.utils.observable.Observable.Observer;

public class StateWait extends GameState implements Runnable {
	
	private static final String OBSERVER_KEY = "wait";
	
	private Lock lock = new ReentrantLock();
	private int left = -1;
	private int task = -1;
	
	public StateWait(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		task = game.getPlugin().getTaskManager().scheduleSyncRepeatingTask(this, 0L, 1L, TimeUnit.SECONDS);
		
		Observer observer = () -> updatePlayers(Bukkit.getOnlinePlayers().size());
		game.getSettings().getMinPlayers().observe(OBSERVER_KEY, observer);
		game.getSettings().getMaxPlayers().observe(OBSERVER_KEY, observer);
		game.getSettings().getWaitingTimeLong().observe(OBSERVER_KEY, observer);
		game.getSettings().getWaitingTimeShort().observe(OBSERVER_KEY, observer);

		game.getWorld().setPVP(false);
		updatePlayers(Bukkit.getOnlinePlayers().size());
		if (left == -1) CaviarStrings.STATE_WAIT_CANCEL.broadcast();
	}
	
	@Override
	public void end() {
		super.end();
		if (task != -1) game.getPlugin().getTaskManager().cancelTaskById(task);
		
		game.getSettings().getMinPlayers().unobserve(OBSERVER_KEY);
		game.getSettings().getMaxPlayers().unobserve(OBSERVER_KEY);
		game.getSettings().getWaitingTimeLong().unobserve(OBSERVER_KEY);
		game.getSettings().getWaitingTimeShort().unobserve(OBSERVER_KEY);
		game.getWorldLoader().stop(false);
	}
	
	@Override
	public void run() {
		lock.lock();
		
		if (left != -1) {
			
			if (left == 0) {
				game.setState(new StatePreparing(game));
			}else {
				if (left == game.getSettings().getWaitingTimeLong().get() || left == game.getSettings().getWaitingTimeShort().get() || left == 60 || left == 30 || left == 15 || left == 10 || left <= 5) {
					CaviarStrings.STATE_WAIT_COUNTDOWN.broadcast(left);
				}
			}
			left--;
		}
		
		lock.unlock();
	}
	
	@Override
	public void handleLogin(PlayerLoginEvent event, GamePlayer player) {
		// do nothing: players can join
	}
	
	protected void updatePlayers(int online) {
		lock.lock();
		
		int min = game.getSettings().getMinPlayers().get();
		if (online < min) {
			if (left != -1) {
				left = -1;
				CaviarStrings.STATE_WAIT_CANCEL.broadcast();
			} else 
				game.getAllPlayers().forEach(player -> game.getPlugin().getScoreboard().waitToStart(player));
		}else {
			int max = game.getSettings().getMaxPlayers().get();
			int waitingTime = (online == max ? game.getSettings().getWaitingTimeShort() : game.getSettings().getWaitingTimeLong()).get();
			if (left == -1 || left > waitingTime) {
				if (left == -1) CaviarStrings.STATE_WAIT_COUNTDOWN_START.broadcast();
				left = waitingTime;
			}
		}
		lock.unlock();
	}
	
	private String getOnlineFormat(int online) {
		int min = game.getSettings().getMinPlayers().get();
		
		return (online >= min ? "§a" : "§c") + " (" + online + "/" + min + ")";
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {
		int online = game.getAllPlayers().size();

		event.getPlayer().setBedSpawnLocation(game.getWorld().getSpawnLocation());
		updatePlayers(online);
		
		event.setJoinMessage(event.getJoinMessage() + getOnlineFormat(online));
	}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
//		int online = game.getAllPlayers().size() - 1;
		int online = game.getAllPlayers().size() - 1;
		
		updatePlayers(online);
		
		event.setQuitMessage(event.getQuitMessage() + getOnlineFormat(online));
		
		return true;
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		disableEvent(event.getPlayer(), event);
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		disableEvent(event.getPlayer(), event);
	}
	@EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.PHYSICAL))
        	return;
		disableEvent(event.getPlayer(), event);
	}
	
	@EventHandler
	public void onEntityPickupItem(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player p)
			disableEvent(p, event);
	}
	
	@EventHandler
	public void onEntityDropItem(EntityDropItemEvent event) {
		if (event.getEntity() instanceof Player p)
			disableEvent(p, event);
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player p)
			disableEvent(p, event);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		disableEvent(event.getPlayer(), event);
	}
	
	@EventHandler
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		disableEvent(event.getPlayer(), event);
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		disableEvent(event.getPlayer(), event);
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location spawn = game.getWorld().getSpawnLocation();
		if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
			if (event.getFrom().distance(spawn) > 100) {
				player.sendActionBar("§cDon't go too far from the spawn");
				player.teleport(spawn);
			}
		}
	}
}
