package fr.caviar.br.generate;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.World;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.utils.Utils;

public class WorldLoader {

	
	private final CaviarBR plugin;
	private GameManager gameManager;
	private Chunk spawnLoader;
	private int realMapMinX;
	private int realMapMinZ;
	private int realMapMaxX;
	private int realMapMaxZ;
	private int totalChunks;
	private int mapSize;
	private int mapChunkSize;
	private int mapLength;
	private int chunksLength;
	private boolean isGenerating = false;
	private int threadsUses;
	
	private CalculateChunk calculateChunk;
	private GenerateChunk generateChunk;

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
			this.spawnLoader = gameManager.getWorld().getSpawnLocation().getChunk();
		} else 
			this.spawnLoader = gameManager.getTreasure().getChunk();
		isGenerating = true;
		mapSize = gameManager.getSettings().getMapSize().get();
		mapChunkSize = mapSize / 16;
		mapLength = mapSize * 2;
		chunksLength = mapLength / 16;
		totalChunks = chunksLength * chunksLength;
		realMapMinX = (-mapChunkSize + spawnLoader.getX()) * 16;
		realMapMinZ = (-mapChunkSize + spawnLoader.getZ()) * 16;
		realMapMaxX = (mapChunkSize + spawnLoader.getX()) * 16;
		realMapMaxZ = (mapChunkSize + spawnLoader.getZ()) * 16;
		calculateChunk = new CalculateChunk(this, threadsUses * 2);
		calculateChunk.start(force);
	}
	
	public void startGenerating(List<ChunkLoad> chunksToLoad) {
		generateChunk = new GenerateChunk(this, chunksToLoad, threadsUses, gameManager.getSettings().getChunkGenerateAsync().get());
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

	public int getTotalChunks() {
		return totalChunks;
	}

	public CaviarBR getPlugin() {
		return plugin;
	}

	public GameManager getGameManager() {
		return gameManager;
	}

	public Chunk getSpawnLoader() {
		return spawnLoader;
	}

	public int getMapSize() {
		return mapSize;
	}

	public int getMapChunkSize() {
		return mapChunkSize;
	}

	public int getMapLength() {
		return mapLength;
	}

	public int getChunksLength() {
		return chunksLength;
	}
	
	public World getWorld() {
		return gameManager.getWorld();
	}
}
