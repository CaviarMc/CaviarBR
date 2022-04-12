package fr.caviar.br.generate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nullable;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import fr.caviar.br.CaviarBR;
import fr.caviar.br.game.GameManager;
import fr.caviar.br.game.GameSettings;
import fr.caviar.br.task.NativeTask;
import fr.caviar.br.utils.Utils;

public class WorldLoader {

	private final static int threadUses = Runtime.getRuntime().availableProcessors() * 2;
	private final CaviarBR plugin;
	private GameSettings settings;
	private GameManager gameManager;
	private NativeTask taskHandler;
	private ChunkLoad lastChunkOperation;

	@Nullable
	private World world;
//	private List<ChunkLoad> chunkToLoad;
	private ConcurrentLinkedQueue<ChunkLoad> chunkToLoad;
	private List<Thread> threads = new ArrayList<>();
	private int chunkAlreadyGenerate = 0;
	private int chunkAlreadyCalculate;
	private boolean isGenerating = false;
	private long timeStarted;
	private long timeChunkStarted;
	private Chunk spawnLoader;
	private int realMapMinX;
	private int realMapMinZ;
	private int realMapMaxX;
	private int realMapMaxZ;
	private int totalChunks;

	public WorldLoader(CaviarBR plugin) {
		this.plugin = plugin;
		this.taskHandler = new NativeTask(this.getClass());
	}

	public void addGameManager(GameManager gameManager) {
		this.gameManager = gameManager;
		this.settings = gameManager.getSettings();
		this.world = gameManager.getWorld();
	}

	public void start(boolean force) {
		if (this.gameManager == null) {
			stop(true);
			throw new RuntimeException("GameManager was not added before start generating.");
		}
		if (isGenerating)
			stop(true);
		if (gameManager.getTreasure() == null) {
			plugin.getLogger().severe(String.format("The treasure has not been calculated yet, we take the world spawn as the spawn for the generation : %s.", Utils.locToStringH(world.getSpawnLocation())));
			this.spawnLoader = world.getSpawnLocation().getChunk();
		} else 
			this.spawnLoader = gameManager.getTreasure().getChunk();
		isGenerating = true;
		timeStarted = Utils.getCurrentTimeInSeconds();
		int mapSize = settings.getMapSize().get();
		int mapChunkSize = mapSize / 16;
		int mapLength = mapSize * 2;
		int chunksLength = mapLength / 16;
		totalChunks = chunksLength * chunksLength;
		realMapMinX = (-mapChunkSize + spawnLoader.getX()) * 16;
		realMapMinZ = (-mapChunkSize + spawnLoader.getZ()) * 16;
		realMapMaxX = (mapChunkSize + spawnLoader.getX()) * 16;
		realMapMaxZ = (mapChunkSize + spawnLoader.getZ()) * 16;
//		chunkToLoad = new ArrayList<>(totalChunks);
		chunkToLoad = new ConcurrentLinkedQueue<ChunkLoad>();
		Runnable runnable = () -> {
			for (int r = 1; mapChunkSize >= r; ++r) {
				for (int x = -r; r >= x; ++x) {
					if (x == -r || r == x) {
						for (int z = -r; r >= z; ++z) {
							chunkToLoad.add(new ChunkLoad(x + spawnLoader.getX(), z + spawnLoader.getZ()));
						}
					} else {
						chunkToLoad.add(new ChunkLoad(x + spawnLoader.getX(), -r + spawnLoader.getZ()));
						chunkToLoad.add(new ChunkLoad(x + spawnLoader.getX(), r + spawnLoader.getZ()));
					}
				}
			}
			totalChunks = chunkToLoad.size();
			long diff1 = Utils.getCurrentTimeInSeconds() - timeStarted;
			plugin.getLogger().log(Level.INFO, String.format("Chunks are identified | %d chunks will be check | started %s ago | %d threads will be used",
					totalChunks, Utils.hrDuration(diff1), threadUses));

			chunkAlreadyCalculate = 0;
			if (chunkToLoad.isEmpty()) {
				endCalculationChunks();
				return;
			}
			taskHandler.scheduleSyncRepeatingTask("generate.calculate.info", () -> {
				long timeDiff = Utils.getCurrentTimeInSeconds() - timeStarted;
				long timeToEndSecs;
				int averageChunksPerSecond, percentageChunk;
				if (chunkAlreadyCalculate > 0 && totalChunks > 0) {
					percentageChunk = (int) (((float) chunkAlreadyCalculate / totalChunks) * 100);
					averageChunksPerSecond = (int) (chunkAlreadyCalculate / timeDiff);
					timeToEndSecs = (totalChunks - chunkAlreadyCalculate) / averageChunksPerSecond;
				} else {
					percentageChunk = 0;
					averageChunksPerSecond = 0;
					timeToEndSecs = 0;
				}
				int lastXChunk, lastZChunk;
				if (lastChunkOperation == null) {
					lastXChunk = 0;
					lastZChunk = 0;
				} else {
					lastXChunk = lastChunkOperation.xChunk;
					lastZChunk = lastChunkOperation.zChunk;
				}
				plugin.getLogger().log(Level.INFO, String.format("Calculate (1/2) %d/%d chunks - %d%% | %d chunks/s | last x z chunk %d %d | started %s ago | ETA %s - %s",
					chunkAlreadyCalculate, totalChunks, percentageChunk, averageChunksPerSecond, 
					lastXChunk, lastZChunk, Utils.hrDuration(timeDiff), Utils.hrDuration(timeToEndSecs),
					Utils.timestampToDateAndHour(Utils.getCurrentTimeInSeconds() + timeToEndSecs)));
			}, 30, 60, TimeUnit.SECONDS);
//			chunkToLoad.removeIf(cl -> world.isChunkGenerated(cl.xChunk, cl.zChunk));
			
			List<List<ChunkLoad>> lists = new Utils.DevideList<ChunkLoad>(chunkToLoad, threadUses).nbList();
			for (Iterator<List<ChunkLoad>> its = lists.iterator(); its.hasNext();) {
				List<ChunkLoad> l = its.next();
				Iterator<ChunkLoad> it = l.iterator();
				Thread t = new Thread(() -> {
					removeIfGenerated(it);
				});
				threads.add(t);
				t.start();
			}
		};
		if (force) {
			plugin.getLogger().log(Level.INFO, String.format("Calculation the order of chunks for a map of %d %d to %d %d : %d chunks", realMapMinX,
					realMapMinZ, realMapMaxX, realMapMaxZ, totalChunks));
			taskHandler.runTaskAsynchronously("generate.calculate", runnable);
			return;
		}
		plugin.getLogger().log(Level.INFO, String.format("Calculation the order of chunks for a map of %d %d to %d %d : %d chunks in %d seconds.", realMapMinX,
				realMapMinZ, realMapMaxX, realMapMaxZ, totalChunks, 10));
		taskHandler.runTaskLater("generate.calculate", () -> {
			taskHandler.runTaskAsynchronously(runnable);
		}, 10, TimeUnit.SECONDS);
	}

	private void removeIfGenerated(Iterator<ChunkLoad> it) {
		if (!it.hasNext() || chunkAlreadyCalculate == -1) {
			if (chunkAlreadyCalculate >= totalChunks) {
				chunkAlreadyCalculate = -1;
				endCalculationChunks();
			}
			return;
		}
		ChunkLoad chunk = it.next();
		if (world.isChunkGenerated(chunk.xChunk, chunk.zChunk))
			chunkToLoad.remove(chunk);
		lastChunkOperation = chunk;
		++chunkAlreadyCalculate;
		removeIfGenerated(it);
	}
	

	private void endCalculationChunks() {
		taskHandler.cancelTask("generate.calculate.info");
		if (!isGenerating)
			return;
		long diff2 = Utils.getCurrentTimeInSeconds() - timeStarted;
		plugin.getLogger().log(Level.INFO, String.format("End calculation (1/2) %d to generate | started %s ago | %d threads",
			chunkToLoad.size(), Utils.hrDuration(diff2), threadUses));
		threads.forEach(Thread::interrupt);
		threads.clear();
		int mapSize = settings.getMapSize().get();
		if (chunkToLoad.isEmpty()) {
			plugin.getLogger().log(Level.INFO, String.format("World is already generate from -%d -%d to %d %d (size %d)", realMapMinX, realMapMinZ, realMapMaxX, realMapMaxZ, mapSize));
			stop(false);
			return;
		}
		/*if (chunkToLoad.stream().map(ChunkLoad::getCode).distinct().count() != chunkToLoad.size()) {
			plugin.getLogger().log(Level.SEVERE, "Algo to calcul chunks add duplicating chunks");
		}*/
		launchTask();
	}

	public void stop(boolean force) {
		taskHandler.terminateTask("generate.chunk.info");
		if (!isGenerating)
			return;
		isGenerating = false;
		threads.forEach(Thread::interrupt);
		threads.clear();
		int averageChunksPerSecond;
		if (chunkAlreadyGenerate > 0 && chunkToLoad.size() > 0)
			averageChunksPerSecond = (int) (((float) chunkAlreadyGenerate / chunkToLoad.size()) * 100);
		else
			averageChunksPerSecond = 0;
		if (force) {
			taskHandler.terminateAllTasks();
			plugin.getLogger().log(Level.INFO, String.format("Generating is stopped | %d/%d chunks generate - %d%% | time taken %s",
					chunkAlreadyGenerate, chunkToLoad.size(), averageChunksPerSecond, Utils.hrDuration(Utils.getCurrentTimeInSeconds() - timeStarted)));
		}
		else {
			taskHandler.cancelAllTasks();
			plugin.getLogger().log(Level.INFO, String.format("Generating is finish | %d/%d chunks generate - %d%% | time taken %s",
					chunkAlreadyGenerate, chunkToLoad.size(), averageChunksPerSecond, Utils.hrDuration(Utils.getCurrentTimeInSeconds() - timeStarted)));
		}
		if (chunkToLoad != null)
			chunkToLoad.clear();
	}

	private void cleanQueue() {
		if (chunkToLoad == null)
			return;
		boolean changes = false;
		int tempSize = chunkToLoad.size();
		if (chunkToLoad.removeIf(chunkLoad -> chunkLoad.isGenerate())) {
			plugin.getLogger().log(Level.INFO, String.format("Removed %d chunks from queue because they are already generated.", tempSize - (tempSize = chunkToLoad.size())));
			changes = true;
		}
		if (changes)
			plugin.getLogger().log(Level.INFO, String.format("Number of chunks to generate : %d", chunkToLoad.size()));
	}

	private void launchTask() {
		timeChunkStarted = Utils.getCurrentTimeInSeconds();
		cleanQueue();
		List<List<ChunkLoad>> lists = new Utils.DevideList<ChunkLoad>(chunkToLoad, threadUses).nbList();
		for (Iterator<List<ChunkLoad>> its = lists.iterator(); its.hasNext();) {
			Iterator<ChunkLoad> it = its.next().iterator();
			Thread t = new Thread(() -> {
				loadChunk(it);
			});
			threads.add(t);
			t.start();
//			taskHandler.runTaskAsynchronously("generate.load." + i, () -> {
//				loadChunk(it);
//			});
			
		}
//		lists.forEach(chunks -> {
//		});
//		Iterator<ChunkLoad> it = chunkToLoad.iterator();
//		loadChunk(it);
		taskHandler.scheduleSyncRepeatingTask("generate.chunk.info", () -> {
			long timeDiff = Utils.getCurrentTimeInSeconds() - timeChunkStarted;
			long timeToEndSecs;
			int averageChunksPerSecond, percentageChunk;
			if (chunkAlreadyGenerate > 0 &&  chunkToLoad.size() > 0) {
				percentageChunk = (int) (((float) chunkAlreadyGenerate / chunkToLoad.size()) * 100);
				averageChunksPerSecond = (int) (chunkAlreadyGenerate / timeDiff);
				timeToEndSecs = (chunkToLoad.size() - chunkAlreadyGenerate) / averageChunksPerSecond;
			} else {
				percentageChunk = 0;
				averageChunksPerSecond = 0;
				timeToEndSecs = 0;
			}
			Location loc;
			if (lastChunkOperation == null || lastChunkOperation.chunk == null) {
				loc = gameManager.getWorld().getBlockAt(0, 0, 0).getLocation();
			} else {
				loc = lastChunkOperation.chunk.getBlock(0, 100, 0).getLocation();
				loc = gameManager.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ()).getLocation();
			}
			plugin.getLogger().log(Level.INFO, String.format("Generate (2/2) %d/%d chunks - %d%% | %d chunks/s | last chunk x/y/z %d %d %d | started %s ago | ETA %s - %s",
				chunkAlreadyGenerate, chunkToLoad.size(), percentageChunk, averageChunksPerSecond, 
				loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), Utils.hrDuration(timeDiff), Utils.hrDuration(timeToEndSecs),
				Utils.timestampToDateAndHour(Utils.getCurrentTimeInSeconds() + timeToEndSecs)));
		}, 1, 5, TimeUnit.MINUTES);
		
//		taskHandler.scheduleSyncRepeatingTask("generate.chunk", new BukkitRunnable() {
//			Iterator<ChunkLoad> it = chunkToLoad.iterator();
//			int i = 0;
//
//			public void run() {
//				if (!it.hasNext()) {
//					stop();
//					return;
//				}
//				ChunkLoad chunk = it.next();
//				chunk.loadChunk();
//				if (++i % 500 == 0) {
//					long timeDiff = Utils.getCurrentTimeInSeconds() - time;
//					double timeToEndSecs = (timeDiff / chunkAlreadyGenerate) * (chunkToLoad.size() - chunkAlreadyGenerate);
//					plugin.getLogger().log(Level.INFO, String.format("Number of chunks generate : %d/%d time %d mins | ETA %d mins", chunkAlreadyGenerate, chunkToLoad.size(), timeDiff / 60, timeToEndSecs / 60));
//				}
//			}
//
//		}, 0, intervalMillis, TimeUnit.MILLISECONDS);
	}
	
	private void loadChunk(Iterator<ChunkLoad> it) {
		if (!it.hasNext() || !isGenerating) {
			stop(false);
			return;
		}
		ChunkLoad chunkL = it.next();
		world.getChunkAtAsync(chunkL.xChunk, chunkL.zChunk, true, chunk -> {
			chunkL.addChunk(chunk);
			++chunkAlreadyGenerate;
			loadChunk(it);
		});
	}

	public class ChunkLoad {
		private int xChunk;
		private int zChunk;
		@Nullable
		private Chunk chunk;
		private boolean isGenerate;
		@Nullable
		private Long code;

		public ChunkLoad() {}
		
		public ChunkLoad(int xChunk, int zChunk) {
			this.xChunk = xChunk;
			this.zChunk = zChunk;
		}

		public ChunkLoad(Chunk chunk, int xChunk, int zChunk) {
			this.xChunk = xChunk;
			this.zChunk = zChunk;
			this.chunk = chunk;
			if (chunk != null)
				this.code = chunk.getChunkKey();
		}

		public ChunkLoad(Chunk chunk) {
			this(chunk, chunk.getX(), chunk.getZ());
		}

		public void addChunk(Chunk chunk) {
			if (this.chunk != null) {
				return;
			}
			if (!isGenerate)
				isGenerate = chunk.isLoaded();
			this.chunk = chunk;
			this.code = chunk.getChunkKey();
			lastChunkOperation = this;
		}
		
		public void loadChunk() {
			if (isGenerate()) {
				return;
			}
			Runnable task = () -> {
				chunk = world.getChunkAt(xChunk, zChunk);
				if (chunk == null)
					return;
				this.code = chunk.getChunkKey();
				world.unloadChunkRequest(xChunk, zChunk);
				/*if (!chunk.isLoaded()) {
					isGenerate = chunk.load(true);
					if (!isGenerate)
						plugin.getLogger().log(Level.SEVERE, String.format("Unable to load chunk : %d|%d", xChunk, zChunk));
					else
						++chunkAlreadyGenerate;
					if (Arrays.stream(chunk.getEntities()).filter(e -> e instanceof LivingEntity).findAny().isEmpty())
						world.unloadChunkRequest(xChunk, zChunk);
				}*/
			};
			if (!plugin.getServer().isPrimaryThread()) {
				taskHandler.runTask(task);
			} else
				task.run();
			++chunkAlreadyGenerate;
		}

		@Nullable
		public Long getCode() {
			return code;
		}

		public boolean isGenerate() {
			return isGenerate || (isGenerate = world.isChunkGenerated(xChunk, zChunk));
		}
		
		@Override
		public String toString() {
			return xChunk + "/" + zChunk;
		}
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
}
