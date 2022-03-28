package fr.caviar.br.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import fr.caviar.br.CaviarStrings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;

public class StatePreparing extends GameState {
	
	private static final List<Material> UNSPAWNABLE_ON = Arrays.asList(
			Material.LAVA, Material.CACTUS, Material.MAGMA_BLOCK, // because they deal damage
			Material.WATER, Material.BUBBLE_COLUMN, Material.KELP, Material.KELP_PLANT, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.TUBE_CORAL, Material.BRAIN_CORAL, Material.BUBBLE_CORAL, Material.FIRE_CORAL, Material.CONDUIT, // because it's in water
			Material.ICE, Material.FROSTED_ICE, Material.BLUE_ICE // because it means on a frozen river
	);

	private Location treasure = null;
	private boolean foundSpawnpoints = false;
	
	public StatePreparing(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		
		CaviarStrings.STATE_PREPARING_PREPARE.broadcast();
		game.getAllPlayers().forEach(p -> {
			blockPlayer(p);
			setPreparing(p);
		});
		Random random = new Random();
		int treasureX = random.nextInt(-1000, 1000);
		int treasureZ = random.nextInt(-1000, 1000);
		game.getPlugin().getLogger().info("Trying to find treasure.");
		prepareLocation(treasureX, treasureZ, tloc -> {
			if (!isRunning()) return;
			
			game.getPlugin().getLogger().info("Found treasure at " + tloc.toString());
			treasure = tloc.add(0, 1, 0);
			
			game.getAllPlayers().forEach(this::setPreparing);
			
			int online = game.getPlayers().size();
			int i = 0;
			for (GamePlayer player : game.getPlayers().values()) {
				double theta = i++ * 2 * Math.PI / online;
				//Player bukkitPlayer = ((Player) player.player.getPlayer());
				player.spawnLocation = null;
				player.started = false;
				
				int playerX = (int) (treasure.getX() + game.getSettings().getPlayersRadius().get() * Math.cos(theta));
				int playerZ = (int) (treasure.getZ() + game.getSettings().getPlayersRadius().get() * Math.sin(theta));
				prepareLocation(playerX, playerZ, ploc -> {
					if (!isRunning()) return;
					
					game.getPlugin().getLogger().info("Found spawnpoint.");
					
					ploc.getChunk().addPluginChunkTicket(game.getPlugin());
					player.setSpawnLocation(ploc);
					
					if (game.getPlayers().values().stream().noneMatch(x -> x.spawnLocation == null)) {
						foundSpawnpoints = true;
						game.getAllPlayers().forEach(this::setPreparing);
						int timer = 10;
						game.getPlugin().getTaskManager().runTaskLater("prep.before_end", this::startCoutdown, timer, TimeUnit.SECONDS);
					}
				}, new AtomicInteger(1), 1);
			}
		}, new AtomicInteger(1), 1);
	}
	
	@Override
	public void end() {
		super.end();
		game.getAllPlayers().forEach(this::exitPreparing);
		game.getPlugin().getTaskManager().cancelTasksByPrefix("prep.");
	}
	
	private void prepareLocation(int x, int z, Consumer<Location> consumer, AtomicInteger operations, int chunks) {
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		game.getWorld().getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
			int y = chunk.getWorld().getHighestBlockYAt(x, z);
			Block block = chunk.getBlock(x - (chunkX << 4), y, z - (chunkZ << 4));
			List<Block> listBlocks = new ArrayList<>(9);
			for (int tempX = -1; tempX <= 1; ++tempX) {
				for (int tempZ = -1; tempZ <= 1; ++tempZ) {
					listBlocks.add(block.getLocation().add(tempX, 0, tempZ).getBlock());
				}
			}
			if (!listBlocks.stream().allMatch(this::isGoodBlock)) {
				tryChunk(chunk, consumer, false, operations, chunks);
			}else {
				game.getPlugin().getLogger().info("Success in " + operations + " operations, in " + chunks + " chunks.");
				consumer.accept(new Location(game.getWorld(), x, y, z));
			}
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
			consumer.accept(new Location(game.getWorld(), 0.5, 80, 0.5));
			return null;
		});
	}
	
	private void tryChunk(Chunk chunk, Consumer<Location> consumer, boolean xChanged, AtomicInteger operations, int chunks) {
		Location location = getNicestBlock(chunk, operations);
		if (location == null) { // have not found good spawnpoint in this chunk
			game.getWorld()
				.getChunkAtAsync(chunk.getX() + (xChanged ? 0 : 1), chunk.getZ() + (xChanged ? 1 : 0))
				.thenAccept(next -> tryChunk(next, consumer, !xChanged, operations, chunks + 1))
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return null;
				});
		}else {
			game.getPlugin().getLogger().info("Success in " + operations + " operations, in " + chunks + " chunks.");
			consumer.accept(location);
		}
	}
	
	private Location getNicestBlock(Chunk chunk, AtomicInteger operations) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				operations.incrementAndGet();
				int globalX = (chunk.getX() << 4) + x;
				int globalZ = (chunk.getZ() << 4) + z;
				int y = chunk.getWorld().getHighestBlockYAt(globalX, globalZ);
				Block block = chunk.getBlock(x, y, z);
				if (isGoodBlock(block)) {
					return block.getLocation();
				}
			}
		}
		return null;
	}
	
	private boolean isGoodBlock(Block block) {
		if (block.getY() < 60) return false;
		Material blockType = block.getType();
		if (UNSPAWNABLE_ON.contains(blockType)) return false;
		
		if (Tag.LEAVES.isTagged(blockType)) return false;
		if (Tag.PREVENT_MOB_SPAWNING_INSIDE.isTagged(blockType)) return false;
		if (Tag.UNSTABLE_BOTTOM_CENTER.isTagged(blockType)) return false;
		return true;
	}
	
	private void setPreparing(Player player) {
		CaviarStrings subtitle;
		if (treasure == null) {
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
				game.getPlugin().getLogger().severe("No spawn location for player " + player.getName());
			game.addSpectator(player);
		} else {
			player.setCompassTarget(game.getWorld().getSpawnLocation());
			player.teleport(gamePlayer.spawnLocation);
			player.setBedSpawnLocation(gamePlayer.spawnLocation);
			player.getInventory().clear();
			gamePlayer.spawnLocation.getChunk().removePluginChunkTicket(game.getPlugin());
		}
	}
	
	private void setCountdown(Player player, int number) {
		player.showTitle(Title.title(CaviarStrings.STATE_PREPARING_TITLE_STARTING_IN.toComponent(), Component.text(number), Times.times(Duration.ZERO, Duration.ofSeconds(5), Duration.ZERO)));
	}

	private void startCoutdown() {
		CaviarStrings.STATE_PREPARING_TELEPORT.broadcast();
		game.getAllPlayers().forEach(p -> {
			p.resetTitle();
			tpPlayers(p, game.getPlayers().get(p.getUniqueId()));
		});
		int timer = game.getSettings().getCountdownStart().get();
		for (int j = 0; j < timer; j++) {
			int j2 = j;
			game.getPlugin().getTaskManager().runTaskLater("prep.countdown." + j, () -> {
				game.getAllPlayers().forEach(p -> setCountdown(p, timer - j2));
			}, j, TimeUnit.SECONDS);
		}
		game.getPlugin().getTaskManager().runTaskLater("prep.countdown_play." + timer, () -> {
			game.setState(new StatePlaying(game, treasure));
		}, timer, TimeUnit.SECONDS);
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
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		if (event.getFrom().getX() != event.getTo().getX() ||
				event.getFrom().getY() != event.getTo().getY() ||
				event.getFrom().getZ() != event.getTo().getZ()) {
			event.setCancelled(true);
			event.getPlayer().sendActionBar("§cYou cannot move during preparation.");
		}
	}
	
	@EventHandler
	public void onPlayerJump(PlayerJumpEvent event) {
		disableEvent(event.getPlayer(), event);
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
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player p)
			disableEvent(p, event);
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
	public void onPlayerInteract(PlayerInteractEvent event) {
		disableEvent(event.getPlayer(), event);
	}
}
