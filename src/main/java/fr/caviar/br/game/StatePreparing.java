package fr.caviar.br.game;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatePreparing extends GameState {
	
	private static final List<Material> UNWALKABLE_ON = Arrays.asList(Material.WATER, Material.LAVA, Material.CACTUS, Material.MAGMA_BLOCK);
	
	private Lock lock = new ReentrantLock();
	
	public StatePreparing(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		Bukkit.broadcastMessage("§aWe are preparing your spawn location.");
		int online = game.getPlayers().size();
		int i = 0;
		for (GamePlayer player : game.getPlayers().values()) {
			double theta = i++ * 2 * Math.PI / online;
			player.spawnLocation = null;
			player.teleported = false;
			
			game.getPlugin().getTaskManager().runTaskAsynchronously(() -> {
				Location loc = prepareLocation(theta);
				
				lock.lock();
				player.spawnLocation = loc.add(0, 1, 0);
				
				if (game.getPlayers().values().stream().noneMatch(x -> x.spawnLocation == null)) {
					game.getPlugin().getTaskManager().runTask(() -> game.setState(new StatePlaying(game)));
				}
				lock.unlock();
			});
		}
		Bukkit.getOnlinePlayers().forEach(this::setPreparing);
	}
	
	@Override
	public void end() {
		super.end();
		Bukkit.getOnlinePlayers().forEach(this::exitPreparing);
	}
	
	private Location prepareLocation(double theta) {
		int x = (int) (game.getSettings().getPlayersRadius().get() * Math.cos(theta));
		int z = (int) (game.getSettings().getPlayersRadius().get() * Math.sin(theta));
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		ChunkSnapshot chunk = game.getWorld().getChunkAt(chunkX, chunkZ).getChunkSnapshot(true, false, false);
		int xInChunk = x - chunkX << 4;
		int zInChunk = z - chunkZ << 4;
		int y = chunk.getHighestBlockYAt(xInChunk, zInChunk);
		if (UNWALKABLE_ON.contains(chunk.getBlockType(xInChunk, y, zInChunk))) {
			Location location;
			boolean xChanged = true;
			while ((location = getNicestBlock(chunk)) == null) {
				if (xChanged = !xChanged)
					chunkZ++;
				else
					chunkX++;
				chunk = game.getWorld().getChunkAt(chunkX, chunkZ).getChunkSnapshot(true, false, false);
			}
			return location;
		}
		return new Location(game.getWorld(), x, y, z);
	}
	
	private Location getNicestBlock(ChunkSnapshot chunk) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int y = chunk.getHighestBlockYAt(x, z);
				if (!UNWALKABLE_ON.contains(chunk.getBlockType(x, y, z)))
					return new Location(game.getWorld(), chunk.getX() << 4 + x, y, chunk.getZ() << 4 + z);
			}
		}
		return null;
	}
	
	private void setPreparing(Player player) {
		player.sendTitle("§ePreparing...", "§7We are preparing the game.", 5, 999999, 0);
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
