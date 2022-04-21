package fr.caviar.br.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.EntityType;
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
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.generate.WorldLoader;
import fr.caviar.br.task.TaskManagerSpigot;
import fr.caviar.br.utils.Cuboid;
import fr.caviar.br.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;

public class StatePreparing extends GameState {

	private List<Location> spawnPoint = new ArrayList<>();
	private List<Shulker> shulkers = new ArrayList<>();
//	private List<Location> maxDistance = null;
	private Location maxDistance = null;
	private boolean foundSpawnpoints = false;
	private TaskManagerSpigot taskManager;
	
	public StatePreparing(GameManager game) {
		super(game);
		taskManager = new TaskManagerSpigot(game.getPlugin(), this.getClass());
	}

	public void addSpawnPoint(Location ploc) {
		Location treasure = game.getTreasure();
		if (treasure != null && (maxDistance == null || ploc.distance(treasure) > maxDistance.distance(treasure)))
			maxDistance = ploc;
		spawnPoint.add(ploc);
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
		
		if (this.spawnPoint.size() >= online) {
			setSpawnPointsToPlayers(this.spawnPoint);
		} else
			calculateSpawnPoints(playerRaduis, online, this::setSpawnPointsToPlayers);

	}

	@Override
	public void end() {
		super.end();
		shulkers.forEach(shulk -> shulk.remove());
		shulkers.clear();
		game.getAllPlayers().forEach(this::exitPreparing);
		taskManager.cancelAllTasks();
	}

	public void setSpawnPointsToPlayers(List<Location> spawnPoints) {
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
	}
	
	public void calculateSpawnPoints(int playerRadius, int nbSpawnPoints, Consumer<List<Location>> consumer) {
		List<Location> spawnPoints = new ArrayList<>();
		for (int i = 0; i < nbSpawnPoints; i++) {
			double theta = i * 2 * Math.PI / nbSpawnPoints;

			int i2 = i;
			int playerX = (int) (game.getTreasure().getBlockX() + playerRadius * Math.cos(theta));
			int playerZ = (int) (game.getTreasure().getBlockZ() + playerRadius * Math.sin(theta));
			
			Cuboid cub = game.getMapCub();
			if (cub != null) {
				if (!cub.isIn(cub.getWorld(), playerX, 0, playerZ)) {
					game.getPlugin().getLogger().severe(String.format("Found spawnpoint in %d %d out of map (%d %d to %d %d).", playerX, playerZ,
							cub.getMin().getBlockX(), cub.getMin().getBlockZ(), cub.getMax().getBlockX(), cub.getMax().getBlockZ()));
				}
			}

			int chunkX = playerX >> 4;
			int chunkZ = playerZ >> 4;
			if (!game.getWorld().isChunkGenerated(chunkX, chunkZ)) {
				game.getPlugin().getLogger().info(String.format("Chunk x=%d - z=%d is not generated, wait for it before check if it is a good spawnpoint.", playerX, playerZ));
			}
			game.prepareLocation(playerX, playerZ, ploc -> {
				if (!isRunning()) return;

				game.getPlugin().getLogger().info("Found spawnpoint n°" + i2 + " in " + Utils.locToStringH(ploc));
				
				addSpawnPoint(ploc);
				spawnPoints.add(ploc);
				
				if (game.getSettings().isDebug().get()) {
		            Shulker shulk = (Shulker) ploc.getWorld().spawnEntity(ploc, EntityType.SHULKER);
		            shulk.setInvisible(true);
		            shulk.setAI(false);
		            shulk.setGlowing(true);
		            shulkers.add(shulk);
				}
				if (spawnPoints.size() == nbSpawnPoints && consumer != null) {
					consumer.accept(spawnPoints);
				}
			}, new AtomicInteger(1), 1);
		}
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
		WorldBorder worldBoader = game.getWorld().getWorldBorder();
		worldBoader.setCenter(game.getTreasure());
		worldBoader.setSize(game.getSettings().getMapSize().get() * 2 + 2);
		worldBoader.setDamageBuffer(1);
		worldBoader.setWarningDistance(25);
		game.getPlugin().getLogger().info("Start countdown. The farthest is on " + maxDistance.getX() + " " + maxDistance.getZ()
			+ " (" + maxDistance.distance(game.getTreasure()) + " from treasure)");
		WorldLoader worldLoader = game.getWorldLoader();
		if (maxDistance.getX() > worldLoader.getRealMapMaxX() || maxDistance.getX() < worldLoader.getRealMapMinX()) {
			game.getPlugin().getLogger().severe(String.format("The map should be minimum between X %d and %d, not X %d.", worldLoader.getRealMapMinX(), worldLoader.getRealMapMaxX()));
		}
		if (maxDistance.getZ() > worldLoader.getRealMapMaxZ() || maxDistance.getZ() < worldLoader.getRealMapMinZ()) {
			game.getPlugin().getLogger().severe(String.format("The map should be minimum between Z %d and %d, not Z %d.", worldLoader.getRealMapMinX(), worldLoader.getRealMapMaxX()));
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
				game.setState(new StatePlaying(game));
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
}
