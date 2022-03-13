package fr.caviar.br.game;

import java.util.Arrays;
import java.util.List;
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
	private boolean foundSpawnpoints = false;
	
	public StatePreparing(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		CaviarStrings.STATE_PREPARING_PREPARE.broadcast();
		int online = game.getPlayers().size();
		int i = 0;
		for (GamePlayer player : game.getPlayers().values()) {
			double theta = i++ * 2 * Math.PI / online;
			player.spawnLocation = null;
			player.started = false;
			
			prepareLocation(theta, loc -> {
				game.getPlugin().getLogger().info("Found spawnpoint.");
				
				loc.getChunk().addPluginChunkTicket(game.getPlugin());
				player.setSpawnLocation(loc);
				
				if (game.getPlayers().values().stream().noneMatch(x -> x.spawnLocation == null)) {
					foundSpawnpoints = true;
					Bukkit.getOnlinePlayers().forEach(this::setPreparing);
					task = game.getPlugin().getTaskManager().runTaskLater(() -> {
						task = -1;
						game.setState(new StatePlaying(game));
					}, 3, TimeUnit.SECONDS);
				}
			});
		}
		Bukkit.getOnlinePlayers().forEach(this::setPreparing);
	}
	
	@Override
	public void end() {
		super.end();
		Bukkit.getOnlinePlayers().forEach(this::exitPreparing);
		if (task != -1) game.getPlugin().getTaskManager().cancelTaskById(task);
	}
	
	private void prepareLocation(double theta, Consumer<Location> consumer) {
		int x = (int) (game.getSettings().getPlayersRadius().get() * Math.cos(theta));
		int z = (int) (game.getSettings().getPlayersRadius().get() * Math.sin(theta));
		game.getPlugin().getLogger().info("First spawnpoint on x:" + x + " z:" + z);
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		game.getWorld().getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
			int y = chunk.getWorld().getHighestBlockYAt(x, z);
			
			if (!isGoodSpawnpoint(chunk.getBlock(x - (chunkX << 4), y, z - (chunkZ << 4)))) {
				tryChunk(chunk, consumer, false);
			}else {
				consumer.accept(new Location(game.getWorld(), x, y, z));
			}
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
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
				if (isGoodSpawnpoint(chunk.getBlock(x, y, z))) {
					return new Location(game.getWorld(), globalX, y, globalZ);
				}
			}
		}
		return null;
	}
	
	private boolean isGoodSpawnpoint(Block block) {
		return block.getY() > 60 && !UNWALKABLE_ON.contains(block.getType());
	}
	
	private void setPreparing(Player player) {
		player.sendTitle(CaviarStrings.STATE_PREPARING_TITLE.toString(), foundSpawnpoints ? CaviarStrings.STATE_PREPARING_SUBTITLE_2.toString() : CaviarStrings.STATE_PREPARING_SUBTITLE_1.toString(), 5, 999999, 0);
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
