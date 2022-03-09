package fr.caviar.br.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class StatePreparing extends GameState {
	
	private static final List<Material> UNWALKABLE_ON = Arrays.asList(Material.WATER, Material.LAVA, Material.CACTUS, Material.MAGMA_BLOCK);
	
	public StatePreparing(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		Bukkit.broadcastMessage("§aWe are preparing your spawn location.");
		ArrayList<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
		int online = players.size();
		for (int i = 0; i < online; i++) {
			Player player = players.get(i);
			double theta = i * 2 * Math.PI / online;
			game.getPlugin().getTaskManager().runTaskAsynchronously(() -> {
				Location location = prepareLocation(theta);
				game.getPlugin().getTaskManager().runTask(() -> player.teleport(location));
			});
		}
	}
	
	private Location prepareLocation(double theta) {
		int x = (int) (game.getSettings().getPlayersRadius().get() * Math.cos(theta));
		int z = (int) (game.getSettings().getPlayersRadius().get() * Math.sin(theta));
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		ChunkSnapshot chunk = game.getWorld().getChunkAt(chunkX, chunkZ).getChunkSnapshot(true, false, false);
		int xInChunk = x - chunkX * 16;
		int zInChunk = z - chunkZ * 16;
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
					return new Location(game.getWorld(), chunk.getX() * 16 + x, y, chunk.getZ() * 16 + z);
			}
		}
		return null;
	}
	
}
