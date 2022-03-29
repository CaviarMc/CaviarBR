package fr.caviar.br.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nullable;

import org.bukkit.Chunk;
import org.bukkit.World;
import fr.caviar.br.CaviarBR;
import fr.caviar.br.task.UniversalTask;
import fr.caviar.br.utils.Utils;

public class WorldLoader {

	private final CaviarBR plugin;
	private GameSettings settings;
	private GameManager gameManager;
	private UniversalTask taskHandler;

	@Nullable
	private World world;
	private List<ChunkLoad> chunkToLoad;
	private List<Thread> threads = new ArrayList<>();
	private int chunkAlreadyGenerate = 0;
	private boolean isGenerating = false;
	private long timeStarted;
	private long timeChunkStarted;

	public WorldLoader(CaviarBR plugin) {
		this.plugin = plugin;
		this.taskHandler = plugin.getTaskManager();
	}

	public void addGameManager(GameManager gameManager) {
		this.gameManager = gameManager;
		this.settings = gameManager.getSettings();
		this.world = gameManager.getWorld();
	}

	public void start(boolean force) {
		if (isGenerating)
			stop(false);
		isGenerating = true;
		timeStarted = Utils.getCurrentTimeInSeconds();
		int mapSize = settings.getMapSize().get();
		int mapChunkSiwe = mapSize / 16;
		int mapLength = mapSize * 2;
		int chunksLength = mapLength / 16;
		int totalChunks = chunksLength * chunksLength;
		chunkToLoad = new ArrayList<>(totalChunks);
		Runnable runnable = () -> {
			plugin.getLogger().log(Level.INFO, String.format("Calculation the order of chunks for a map of -%d -%d to %d %d : %d chunks", mapSize, mapSize, mapSize, mapSize, totalChunks));
			for (int r = 1; mapChunkSiwe >= r; r++) {
				for (int x = -r; r >= x; x++) {
					if (x == -r || r == x) {
						for (int z = -r; r >= z; z++) {
							addChunkIfNeeded(x, z);
						}
					} else {
						addChunkIfNeeded(x, -r);
						addChunkIfNeeded(x, r);
					}
				}
			}
			if (chunkToLoad.isEmpty()) {
				plugin.getLogger().log(Level.INFO, String.format("World is already generate from -%d -%d to %d %d", mapSize, mapSize, mapSize, mapSize));
				stop(true);
				return;
			}
			plugin.getLogger().log(Level.INFO, String.format("Total chunk to generate : %d | started %s ago", chunkToLoad.size(), Utils.hrDuration(Utils.getCurrentTimeInSeconds() - timeStarted)));
			/*if (chunkToLoad.stream().map(ChunkLoad::getCode).distinct().count() != chunkToLoad.size()) {
				plugin.getLogger().log(Level.SEVERE, "Algo to calcul chunks add duplicating chunks");
			}*/
			launchTask();
		};
		if (force) {
			taskHandler.runTaskAsynchronously(runnable);
			return;
		}
		plugin.getLogger().log(Level.INFO, String.format("Launch generation for a map of -%d -%d to %d %d check in 10 secondes.", mapSize, mapSize, mapSize, mapSize));
		taskHandler.runTaskLater("generate.calculate", () -> {
			taskHandler.runTaskAsynchronously(runnable);
		}, 10, TimeUnit.SECONDS);
	}

	public void stop(boolean succes) {
		if (!isGenerating)
			return;
		isGenerating = false;
		taskHandler.cancelTaskByName("generate.calculate");
		if (taskHandler.cancelTaskByName("generate.chunk")) {
			plugin.getLogger().log(Level.INFO, "Task delete");
		}
		taskHandler.cancelTasksByPrefix("generate.load");
		
		threads.forEach(Thread::interrupt);
		if (!succes)
			plugin.getLogger().log(Level.INFO, String.format("Generating is stopped | %d/%d chunks generate | time taken %s",
					chunkAlreadyGenerate, chunkToLoad.size(), Utils.hrDuration(Utils.getCurrentTimeInSeconds() - timeStarted)));
		else
			plugin.getLogger().log(Level.INFO, String.format("Generating is finish | %d/%d chunks generate | time taken %s",
					chunkAlreadyGenerate, chunkToLoad.size(), Utils.hrDuration(Utils.getCurrentTimeInSeconds() - timeStarted)));
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
		/*Set<Long> allChunkCode = chunkToLoad.stream().map(ChunkLoad::getCode).collect(Collectors.toSet());
		if (chunkToLoad.removeIf(chunkLoad -> allChunkCode.stream().filter(allCode -> allCode == chunkLoad.getCode()).count() >= 2)) {
			plugin.getLogger().log(Level.INFO, String.format("Removed %d chunks from queue because they are duplicates", tempSize - (tempSize = chunkToLoad.size())));
			changes = true;
		}*/
		if (changes)
			plugin.getLogger().log(Level.INFO, String.format("Number of chunks to generate : %d", chunkToLoad.size()));
	}

	private void launchTask() {
		timeChunkStarted = Utils.getCurrentTimeInSeconds();
		cleanQueue();
		List<List<ChunkLoad>> lists = new Utils.DevideList<ChunkLoad>(chunkToLoad, 20).execute();
		Iterator<List<ChunkLoad>> its = lists.iterator();
		for (int i = 0; its.hasNext(); ++i) {
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
		lists.forEach(chunks -> {
		});
//		Iterator<ChunkLoad> it = chunkToLoad.iterator();
//		loadChunk(it);
		taskHandler.scheduleSyncRepeatingTask("generate.chunk", () -> {
			long timeDiff = Utils.getCurrentTimeInSeconds() - timeChunkStarted;
			long avrageChunksPerSecond = chunkAlreadyGenerate / timeDiff;
			long timeToEndSecs = (chunkToLoad.size() - chunkAlreadyGenerate) / avrageChunksPerSecond;
			plugin.getLogger().log(Level.INFO, String.format("Generate %d/%d chunks - %d%% | %d chunks/s | started %s ago | ETA %s - %s",
					chunkAlreadyGenerate, chunkToLoad.size(), chunkToLoad.size() / chunkAlreadyGenerate / 100, avrageChunksPerSecond,  Utils.hrDuration(timeDiff), Utils.hrDuration(timeToEndSecs),
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
			stop(true);
			return;
		}
		ChunkLoad chunkL = it.next();
		world.getChunkAtAsync(chunkL.xChunk, chunkL.zChunk, true, chunk -> {
			chunkL.addChunk(chunk);
			++chunkAlreadyGenerate;
			loadChunk(it);
		});
	}

	@Nullable
	public ChunkLoad addChunkIfNeeded(int x, int z) {
		if (world.isChunkGenerated(x, z)) {
			return null;
		}
		ChunkLoad chunkLoad = new ChunkLoad(null, x, z);
		chunkToLoad.add(chunkLoad);
		return chunkLoad;
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

		public ChunkLoad(Chunk chunk, int xChunk, int zChunk) {
			this.xChunk = xChunk;
			this.zChunk = zChunk;
			this.chunk = chunk;
			if (chunk != null)
				this.code = chunk.getChunkKey();
		}

		public ChunkLoad(int xChunk, int zChunk) {
			this(null, xChunk, zChunk);
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
}
