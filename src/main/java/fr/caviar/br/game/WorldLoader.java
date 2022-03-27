package fr.caviar.br.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nullable;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.task.UniversalTask;
import fr.caviar.br.utils.Utils;

public class WorldLoader {

	private final CaviarBR plugin;
	private GameSettings settings;
	private GameManager gameManager;
	private UniversalTask taskHandler;
	Thread tread;

	@Nullable
	private World world;
	private final List<ChunkLoad> chunkToLoad = new ArrayList<>();
	private int chunkAlreadyGenerate = 0;
	private boolean isGenerating = false;
	private long time = 0;

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
		time = Utils.getCurrentTimeInSeconds();
		Integer mapSize = settings.getMapSize().get();
		tread = new Thread(() -> {
			Thread.onSpinWait();
			plugin.getLogger().log(Level.INFO, String.format("Calculation of the number of chunks to generate for a map of -%d -%d to %d %d", mapSize, mapSize, mapSize, mapSize));
			for (int r = 1; mapSize >= r; r++) {
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
				return;
			}
			plugin.getLogger().log(Level.INFO, String.format("Total chunk not generated : %d | time %s mins", chunkToLoad.size(), (Utils.getCurrentTimeInSeconds() - time) / 60));
			/*if (chunkToLoad.stream().map(ChunkLoad::getCode).distinct().count() != chunkToLoad.size()) {
				plugin.getLogger().log(Level.SEVERE, "Algo to calcul chunks add duplicating chunks");
			}*/
			launchTask(110);
		});
		if (force) {
			tread.start();
			return;
		}
		plugin.getLogger().log(Level.INFO, String.format("Launch generation for a map of -%d -%d to %d %d check in 30 secondes.", mapSize, mapSize, mapSize, mapSize));
		taskHandler.runTaskLater("generate.calculate", () -> {
			tread.start();
		}, 30, TimeUnit.SECONDS);
	}

	public void stop() {
		taskHandler.cancelTaskByName("generate.calculate");
		taskHandler.cancelTaskByName("generate.chunk");
		tread.interrupt();
		isGenerating = false;
		plugin.getLogger().log(Level.INFO, String.format("Generating is stopped | %d/%d chunks generate | time %d mins", chunkAlreadyGenerate, chunkToLoad.size(), (Utils.getCurrentTimeInSeconds() - time) / 60));
	}

	private void cleanQueue() {
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

	private void launchTask(int intervalMillis) {
		taskHandler.cancelTaskByName("generate.chunk");
		cleanQueue();
		isGenerating = true;
		taskHandler.scheduleSyncRepeatingTask("generate.chunk", new BukkitRunnable() {
			Iterator<ChunkLoad> it = chunkToLoad.iterator();
			int i = 0;

			public void run() {
				if (!it.hasNext()) {
					stop();
					return;
				}
				ChunkLoad chunk = it.next();
				chunk.loadChunk();
				if (++i % 500 == 0) {
					long timeDiff = Utils.getCurrentTimeInSeconds() - time;
					long timeToEndSecs = (timeDiff / chunkAlreadyGenerate) * (chunkToLoad.size() - chunkAlreadyGenerate);
					plugin.getLogger().log(Level.INFO, String.format("Number of chunks generate : %d/%d time %d mins | ETA %d mins", chunkAlreadyGenerate, chunkToLoad.size(), timeDiff / 60, timeToEndSecs / 60));
				}
			}

		}, 0, intervalMillis, TimeUnit.MILLISECONDS);
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

		public void loadChunk() {
			if (isGenerate()) {
				++chunkAlreadyGenerate;
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
