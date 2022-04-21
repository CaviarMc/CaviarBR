package fr.caviar.br.game;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.WorldBorder;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.task.TaskManagerSpigot;
import fr.caviar.br.utils.observable.Observable.Observer;

public class StateWait extends GameState implements Runnable {
	
	private static final String OBSERVER_KEY = "wait";
	
	private Lock lock = new ReentrantLock();
	private int left = -1;
	private TaskManagerSpigot taskManager;
	
	public StateWait(GameManager game) {
		super(game);
		taskManager = new TaskManagerSpigot(game.getPlugin(), this.getClass());
	}
	
	@Override
	public void start() {
		super.start();
		taskManager.scheduleSyncRepeatingTask(this, 0L, 1L, TimeUnit.SECONDS);
		
		Observer observer = () -> updatePlayers(Bukkit.getOnlinePlayers().size());
		game.getSettings().getMinPlayers().observe(OBSERVER_KEY, observer);
		game.getSettings().getMaxPlayers().observe(OBSERVER_KEY, observer);
		game.getSettings().getWaitingTimeLong().observe(OBSERVER_KEY, observer);
		game.getSettings().getWaitingTimeShort().observe(OBSERVER_KEY, observer);
		game.getSettings().getTreasureRaduis().observe(OBSERVER_KEY, () -> game.calculateTreasureSpawnPoint(null));

		game.getWorld().setPVP(false);
		updatePlayers(Bukkit.getOnlinePlayers().size());
		if (left == -1) CaviarStrings.STATE_WAIT_CANCEL.broadcast();
		
		WorldBorder worldBoader = game.getWorld().getWorldBorder();
		worldBoader.reset();
		worldBoader.setCenter(game.getWorld().getSpawnLocation());
		worldBoader.setSize(50 * 2);
		worldBoader.setWarningDistance(10);
		game.getWorld().setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
	}
	
	@Override
	public void end() {
		super.end();
		taskManager.cancelAllTasks();
		
		game.getSettings().getMinPlayers().unobserve(OBSERVER_KEY);
		game.getSettings().getMaxPlayers().unobserve(OBSERVER_KEY);
		game.getSettings().getWaitingTimeLong().unobserve(OBSERVER_KEY);
		game.getSettings().getWaitingTimeShort().unobserve(OBSERVER_KEY);
		game.getSettings().getTreasureRaduis().unobserve(OBSERVER_KEY);
	}
	
	@Override
	public void run() {
		lock.lock();
		
		if (left != -1) {
			if (left == 0) {
				game.setState(new StatePreparing(game));
			} else {
				if (left == game.getSettings().getWaitingTimeLong().get() || left == game.getSettings().getWaitingTimeShort().get()
						|| left == 60 || left == 30 || left == 15 || left == 10 || left <= 5) {
					CaviarStrings.STATE_WAIT_COUNTDOWN.broadcast(left);
				}
			}
			--left;
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
		int max = game.getSettings().getMaxPlayers().get();
		if (online >= min)
			return String.format(" §a(%d/%d)", online, max);
		return String.format(" §c(%d/%d)", online, min);
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {
		Player p = event.getPlayer();
		int online = game.getAllPlayers().size();

//		p.setBedSpawnLocation(game.getWorld().getSpawnLocation());
		p.teleport(game.getWorld().getSpawnLocation());
		updatePlayers(online);
		
		event.setJoinMessage(event.getJoinMessage() + getOnlineFormat(online));
	}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
//		int online = game.getAllPlayers().size() - 1;
		int online = game.getAllPlayers().size();
		
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
		if (event.getEntity() instanceof Player p) {
			if (event.getCause().equals(DamageCause.SUFFOCATION)) { // Worldboarder damage
				p.teleport(game.getWorld().getSpawnLocation());
			}
			disableEvent(p, event);
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player p) {
			disableEvent(p, event);
		}
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
	public void onEntityTarget(EntityTargetEvent event) {
		if (event.getTarget() instanceof Player p) {
			event.setTarget(null);
			//event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		event.setDroppedExp(0);
		event.getDrops().clear();
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent e) {
		if (e.getEntity() == null || e.getEntity().getType() != EntityType.ENDERMAN)
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player p) {
			disableEvent(p, event);
		}
	}
	
	/*@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location spawn = game.getWorld().getSpawnLocation();
		if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
			if (event.getFrom().distance(spawn) > 100) {
				player.sendActionBar("§cDon't go too far from the spawn");
				player.teleport(spawn);
			}
		}
	}*/
}
