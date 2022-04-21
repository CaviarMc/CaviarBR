package fr.caviar.br.generate;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.scoreboard.Scoreboard;
import fr.caviar.br.utils.Cuboid;
import fr.caviar.br.utils.Utils;

public class WorldLoader {

	
	private final CaviarBR plugin;
	private GameManager gameManager;
	private Location spawnLoader;
	private int realMapMinX;
	private int realMapMinZ;
	private int realMapMaxX;
	private int realMapMaxZ;
	private int mapSize;
	private int mapLength;
	private boolean isGenerating = false;
	private int threadsUses;
	
	private CalculateChunk calculateChunk;
	private GenerateChunk generateChunk;
	private Cuboid cub;

	public WorldLoader(CaviarBR plugin) {
		this.plugin = plugin;
		this.threadsUses = Runtime.getRuntime().availableProcessors();
	}

	public void addGameManager(GameManager gameManager) {
		this.gameManager = gameManager;
	}

	public void start(boolean force) {
		if (this.gameManager == null) {
			stop(true);
			throw new RuntimeException("GameManager was not added before start generating.");
		}
		if (isGenerating)
			stop(true);
		if (gameManager.getTreasure() == null) {
			plugin.getLogger().severe(String.format("The treasure has not been calculated yet, we take the world spawn as the spawn for the generation : %s.",
					Utils.locToStringH(gameManager.getWorld().getSpawnLocation())));
			this.spawnLoader = gameManager.getWorld().getSpawnLocation();
		} else 
			this.spawnLoader = gameManager.getTreasure();
		calcMap();
		isGenerating = true;
		calculateChunk = new CalculateChunk(this, threadsUses * 2);
		calculateChunk.start(force);
	}

	public void calcMap() {
		mapSize = gameManager.getSettings().getMapSize().get();
		mapLength = mapSize + 1 * 2;
		realMapMinX = (-mapSize + spawnLoader.getBlockX());
		realMapMinZ = (-mapSize + spawnLoader.getBlockZ());
		realMapMaxX = (mapSize + spawnLoader.getBlockX());
		realMapMaxZ = (mapSize + spawnLoader.getBlockZ());
		cub = new Cuboid(getWorld(), realMapMinX, realMapMinZ, getWorld().getMinHeight(), realMapMaxX, getWorld().getMaxHeight(), realMapMaxZ);
	}

	public void startGenerating(List<ChunkLoad> chunksToLoad) {
		generateChunk = new GenerateChunk(this, chunksToLoad, threadsUses, gameManager.getSettings().isChunkGenerateAsync().get());
		generateChunk.start();
		calculateChunk = null;
	}

	public void stop(boolean force) {
		if (calculateChunk != null)
			calculateChunk.end(force);
		if (generateChunk != null)
			generateChunk.end(force);
		calculateChunk = null;
		generateChunk = null;
		isGenerating = false;
	}

	public int getRealMapMinX() {
		return realMapMinX;
	}

	public int getRealMapMinZ() {
		return realMapMinZ;
	}

	public int getRealMapMaxX() {
		return realMapMaxX;
	}

	public int getRealMapMaxZ() {
		return realMapMaxZ;
	}

	public CaviarBR getPlugin() {
		return plugin;
	}

	public GameManager getGameManager() {
		return gameManager;
	}

	public Chunk getSpawnLoader() {
		return spawnLoader.getChunk();
	}

	public int getMapSize() {
		return mapSize;
	}

	public int getMapLength() {
		return mapLength;
	}
	
	public World getWorld() {
		return gameManager.getWorld();
	}
	
	public Cuboid getCub() {
		return cub;
	}
	
	protected void updatePlayerScoreboard() {
		if (!gameManager.getSettings().isDebug().get())
			return;
		
		Scoreboard sb = gameManager.getPlugin().getScoreboard();
		gameManager.getAllPlayers().forEach(player -> sb.waitToStart(player));
	}

	public String getStatus() {
		int step, percentage, chunkPerSec;
		if (calculateChunk != null) {
			step = 1;
			percentage = calculateChunk.getPercentageChunk();
			chunkPerSec = calculateChunk.getAverageChunksPerSecond();
		} else if (generateChunk != null) {
			step = 2;
			percentage = generateChunk.getPercentageChunk();
			chunkPerSec = generateChunk.getAverageChunksPerSecond();
		} else {
			return "Generation done";
		}
		return String.format("(%d/2) %d%% - %d chunk/s", step, percentage, chunkPerSec);
	}

	public int getTotalChunks() {
		if (cub != null)
			return cub.getTotalChunksSize();
		return calculateChunk != null ? calculateChunk.getTotalChunks() : -1;
	}
	
}
