package fr.caviar.br.game;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.CaviarStrings;

public class StatePreparing extends GameState {
	
	private static final List<Material> UNWALKABLE_ON = Arrays.asList(Material.WATER, Material.LAVA, Material.CACTUS, Material.MAGMA_BLOCK);
	
	private int task = -1;
	private Location treasure = null;
	private boolean foundSpawnpoints = false;
	
	public StatePreparing(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		
		CaviarStrings.STATE_PREPARING_PREPARE.broadcast();
		Bukkit.getOnlinePlayers().forEach(this::setPreparing);
		
		Random random = new Random();
		int treasureX = random.nextInt(-1000, 1000);
		int treasureZ = random.nextInt(-1000, 1000);
		prepareLocation(treasureX, treasureZ, tloc -> {
			if (!isRunning()) return;
			
			game.getPlugin().getLogger().info("Found treasure at " + tloc.toString());
			treasure = tloc.add(0, 1, 0);
			
			Bukkit.getOnlinePlayers().forEach(this::setPreparing);
			
			int online = game.getPlayers().size();
			int i = 0;
			for (GamePlayer player : game.getPlayers().values()) {
				double theta = i++ * 2 * Math.PI / online;
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
						Bukkit.getOnlinePlayers().forEach(this::setPreparing);
						task = game.getPlugin().getTaskManager().runTaskLater(() -> {
							task = -1;
							game.setState(new StatePlaying(game, treasure));
						}, 3, TimeUnit.SECONDS);
					}
				});
			}
		});
	}
	
	@Override
	public void end() {
		super.end();
		Bukkit.getOnlinePlayers().forEach(this::exitPreparing);
		if (task != -1) game.getPlugin().getTaskManager().cancelTaskById(task);
	}
	
	private void prepareLocation(int x, int z, Consumer<Location> consumer) {
		game.getPlugin().getLogger().info("First spawnpoint on x:" + x + " z:" + z);
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		game.getWorld().getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
			int y = chunk.getWorld().getHighestBlockYAt(x, z);
			
			if (!isGoodBlock(chunk.getBlock(x - (chunkX << 4), y, z - (chunkZ << 4)))) {
				tryChunk(chunk, consumer, false);
			}else {
				consumer.accept(new Location(game.getWorld(), x, y, z));
			}
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
			consumer.accept(new Location(game.getWorld(), 0, 80, 0));
			return null;
		});
	}
	
	private void tryChunk(Chunk chunk, Consumer<Location> consumer, boolean xChanged) {
		game.getPlugin().getLogger().info("Trying chunk " + chunk.toString());
		Location location = getNicestBlock(chunk);
		if (location == null) { // have not found good spawnpoint in this chunk
			game.getWorld()
				.getChunkAtAsync(chunk.getX() + (xChanged ? 0 : 1), chunk.getZ() + (xChanged ? 1 : 0))
				.thenAccept(next -> tryChunk(next, consumer, !xChanged))
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return null;
				});
		}else {
			consumer.accept(location);
		}
	}
	
	private Location getNicestBlock(Chunk chunk) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int globalX = (chunk.getX() << 4) + x;
				int globalZ = (chunk.getZ() << 4) + z;
				int y = chunk.getWorld().getHighestBlockYAt(globalX, globalZ);
				if (isGoodBlock(chunk.getBlock(x, y, z))) {
					return new Location(game.getWorld(), globalX, y, globalZ);
				}
			}
		}
		return null;
	}
	
	private boolean isGoodBlock(Block block) {
		return block.getY() > 60 && !UNWALKABLE_ON.contains(block.getType());
	}
	
	private void setPreparing(Player player) {
		String subtitle;
		if (treasure == null) {
			subtitle = CaviarStrings.STATE_PREPARING_SUBTITLE_TREASURE.toString();
		}else if (!foundSpawnpoints) {
			subtitle = CaviarStrings.STATE_PREPARING_SUBTITLE_SPAWNPOINTS.toString();
		}else {
			subtitle = CaviarStrings.STATE_PREPARING_SUBTITLE_STARTING.toString();
		}
		player.sendTitle(CaviarStrings.STATE_PREPARING_TITLE.toString(), subtitle, 5, 999999, 0);
	}
	
	private void exitPreparing(Player player) {
		player.resetTitle();
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {
		setPreparing(event.getPlayer());
	}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
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
	
}
