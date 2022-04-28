package fr.caviar.br.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.generate.WorldLoader;
import fr.caviar.br.task.TaskManagerSpigot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;

public class StatePreparing extends GameState {

	private List<Shulker> shulkers = new ArrayList<>();
	@Nullable
	private Location maxDistance = null;
	private TaskManagerSpigot taskManager;
	private boolean foundSpawnpoints = false;

	public StatePreparing(GameManager game) {
		super(game);
		taskManager = new TaskManagerSpigot(game.getPlugin(), this.getClass());
	}
	@Override
	public void start() {
		super.start();
		Validate.notNull(game.getTreasure());

		game.getWorldLoader().stop(true);
	
		CaviarStrings.STATE_PREPARING_PREPARE.broadcast();
		game.getAllPlayers().forEach(p -> {
			blockPlayer(p);
			setPreparing(p);
		});
		WorldBorder worldBoader = game.getWorld().getWorldBorder();
		worldBoader.reset();

		int playerRaduis = game.getSettings().getPlayersRadius().get();
		int online = game.getPlayers().size();
	
//		if (spawnPoint != null && this.spawnPoint.size() >= online) {
//			setSpawnPointsToPlayers(this.spawnPoint, maxDistance);
//		} else
		game.getSpawnPoints(playerRaduis, online, this::setSpawnPointsToPlayers);

	}

	@Override
	public void end() {
		super.end();
		shulkers.forEach(shulk -> shulk.remove());
		shulkers.clear();
		game.getAllPlayers().forEach(this::exitPreparing);
		taskManager.cancelAllTasks();
	}

	public void setSpawnPointsToPlayers(List<Location> spawnPoints, Location maxDistance) {
		Iterator<Location> it = spawnPoints.iterator();
		Location ploc;
		if (it.hasNext()) {
			for (GamePlayer player : game.getPlayers().values()) {
				//Player bukkitPlayer = ((Player) player.player.getPlayer());
				player.spawnLocation = null;
				if (!it.hasNext()) {
					game.getPlugin().getLogger().severe(String.format("Can't take a spawn point for %s, they are not enough.", player.player.getName()));
					continue;
				}
				ploc = it.next();
				player.started = false;
				ploc.getChunk().addPluginChunkTicket(game.getPlugin());
				player.setSpawnLocation(ploc);
			}
		}
		if (game.getPlayers().values().stream().noneMatch(x -> x.spawnLocation == null)) {
			foundSpawnpoints = true;
			game.getAllPlayers().forEach(this::setPreparing);
			int timer = 10;
			taskManager.runTaskLater("prep.before_end", this::startCoutdown, timer, TimeUnit.SECONDS);
		} else {
			game.getPlugin().getLogger().severe(String.format("Can't launch countdown, they are not enough spawn points. /game start to retry."));
			CaviarStrings.NOT_ENOUGH_SPAWNPOINTS.send(game.getModerators().keySet());
		}
		this.maxDistance = maxDistance;
	}

	private void setPreparing(Player player) {
		CaviarStrings subtitle;
		if (game.getTreasure() == null) {
			subtitle = CaviarStrings.STATE_PREPARING_SUBTITLE_TREASURE;
		}else if (!foundSpawnpoints) {
			subtitle = CaviarStrings.STATE_PREPARING_SUBTITLE_SPAWNPOINTS;
		}else {
			subtitle = CaviarStrings.STATE_PREPARING_SUBTITLE_STARTING;
		}
		player.showTitle(Title.title(CaviarStrings.STATE_PREPARING_TITLE.toComponent(), subtitle.toComponent(), Times.times(Duration.ZERO, Duration.ofSeconds(99999), Duration.ZERO)));
	}

	private void exitPreparing(Player player) {
		unblockPlayer(player);
		player.resetTitle();
	}

	@SuppressWarnings("deprecation")
	private void tpPlayers(Player player, GamePlayer gamePlayer) {
		if (gamePlayer == null || gamePlayer.spawnLocation == null) {
			if (gamePlayer != null)
				game.getPlugin().getLogger().severe("No spawn location for player " + player.getName() + ". He is now spectator.");
			game.addSpectator(player);
		} else {
			player.setCompassTarget(game.getWorld().getSpawnLocation());
			taskManager.runTask(() -> {
				player.teleport(gamePlayer.spawnLocation);
				player.setBedSpawnLocation(gamePlayer.spawnLocation);
				player.getInventory().clear();
				player.setHealth(player.getMaxHealth());
				player.setFoodLevel(20);
				gamePlayer.spawnLocation.getChunk().removePluginChunkTicket(game.getPlugin());
			});
		}
	}

	private void setCountdown(Player player, int number) {
		player.showTitle(Title.title(CaviarStrings.STATE_PREPARING_TITLE_STARTING_IN.toComponent(), Component.text(number), Times.times(Duration.ZERO, Duration.ofSeconds(5), Duration.ZERO)));
	}

	private void startCoutdown() {
		World world = game.getWorld();
		WorldBorder worldBorder = world.getWorldBorder();
		worldBorder.setCenter(game.getTreasure());
		worldBorder.setDamageBuffer(1);
		worldBorder.setWarningDistance(25);
		int mapSizeSettings = game.getSettings().getMapSize().get();
		StatePlaying nextState = new StatePlaying(game);
		if (maxDistance == null) {
			game.getPlugin().getLogger().info("Max distance is not calculate, can't check distance.");
		} else {
			double distanceTreasure = maxDistance.distance(game.getTreasure());
			game.getPlugin().getLogger().info("Start countdown. The farthest is on " + maxDistance.getX() + " " + maxDistance.getZ()
				+ " (" + maxDistance.distance(game.getTreasure()) + " from treasure)");
			WorldLoader worldLoader = game.getWorldLoader();
			if (!worldBorder.isInside(maxDistance)) {
				int newMapSize = (int) (Math.round(distanceTreasure) + 200);
				game.getPlugin().getLogger().severe(String.format("The farthest is out of map, we need to expend the map from %d to %d.", mapSizeSettings, newMapSize));
				game.getSettings().getMapSize().set(newMapSize);
				startCoutdown();
				return;
			}
			if (maxDistance.getX() > worldLoader.getRealMapMaxX() || maxDistance.getX() < worldLoader.getRealMapMinX()) {
				game.getPlugin().getLogger().severe(String.format("The map should be minimum between X %d and %d, not X %d.", worldLoader.getRealMapMinX(), worldLoader.getRealMapMaxX()));
			}
			if (maxDistance.getZ() > worldLoader.getRealMapMaxZ() || maxDistance.getZ() < worldLoader.getRealMapMinZ()) {
				game.getPlugin().getLogger().severe(String.format("The map should be minimum between Z %d and %d, not Z %d.", worldLoader.getRealMapMinX(), worldLoader.getRealMapMaxX()));
			}
		}
		taskManager.runTaskAsynchronously("prep.asynccountdown." , () -> {
			CaviarStrings.STATE_PREPARING_TELEPORT.broadcast();
//			List<List<Player>> list = new Utils.DevideList<Player>(game.getGamers(), 10).maxSizeList();
			game.getAllPlayers().forEach(p -> {
				p.resetTitle();
				tpPlayers(p, game.getPlayers().get(p.getUniqueId()));
			});
			int timer = game.getSettings().getCountdownStart().get();
			for (int j = 0; j < timer; j++) {
				int j2 = j;
				taskManager.runTaskLater("prep.countdown." + j, () -> {
					int cd = timer - j2;
					game.getAllPlayers().forEach(p -> setCountdown(p, cd));
					if (cd % 10 == 0 || cd <= 5)
						game.getPlugin().getLogger().log(Level.INFO, String.format("Starting in %d secondes", cd));
				}, j, TimeUnit.SECONDS);
			}
			taskManager.runTaskLater("prep.countdown_play." + timer, () -> {
				game.setState(nextState);
			}, timer, TimeUnit.SECONDS);
		});
	}

	public void blockPlayer(Player player) {
		player.setWalkSpeed(0);
	}

	public void unblockPlayer(Player player) {
		player.setWalkSpeed(0.2f);
	}

	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {
		setPreparing(event.getPlayer());
		blockPlayer(event.getPlayer());
	}

	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
		unblockPlayer(event.getPlayer());
		return false;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		if (event.getFrom().getX() != event.getTo().getX() ||
				event.getFrom().getY() != event.getTo().getY() ||
				event.getFrom().getZ() != event.getTo().getZ()) {
			event.setCancelled(true);
			event.getPlayer().sendActionBar("§cYou cannot move during preparation.");
		}
	}
//
//	@EventHandler
//	public void onPlayerJump(PlayerJumpEvent event) {
//		disableEvent(event.getPlayer(), event);
//	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		disableEvent(event.getPlayer(), event);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		disableEvent(event.getPlayer(), event);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player p) {
			disableEvent(p, event);
		}
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
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player p) {
			disableEvent(p, event);
		}
	}

}
