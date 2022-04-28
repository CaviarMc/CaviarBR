package fr.caviar.br.generate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Chunk;
import com.google.common.collect.Lists;

import fr.caviar.br.task.NativeTask;
import fr.caviar.br.utils.Utils;

public class CalculateChunk {

	private ConcurrentLinkedQueue<ChunkLoad> chunkToLoad = new ConcurrentLinkedQueue<ChunkLoad>();
	private List<Thread> threads = new ArrayList<>();
	private WorldLoader worldLoader;
	private NativeTask taskHandler = new NativeTask(this.getClass());
	private Lock lock = new ReentrantLock();
	private int totalChunks = 0;
	private int threadsUses;
	private long timeStarted;

	CalculateChunk(WorldLoader worldLoader, int threadUses) {
		this.worldLoader = worldLoader;
		this.threadsUses = threadUses;
	}

	void start(boolean force) {
		timeStarted = Utils.getCurrentTimeInSeconds();
		Chunk spawnLoader = worldLoader.getSpawnLoader();
		int mapChunkSize = worldLoader.getMapSize() / 16;
//		PluginManager plManager = worldLoader.getPlugin().getServer().getPluginManager();
//		if (plManager.isPluginEnabled("Dynmap")) {
//			DynmapAPI dynmap = (DynmapAPI) worldLoader.getPlugin().getServer().getPluginManager().getPlugin("Dynmap");
//			MarkerAPI m = dynmap.getMarkerAPI();
//			MarkerSet marketSet = m.getMarkerSet("CHUNK");
//			marketSet.createMarker(UUID.randomUUID().toString(), "Chunk", null, mapChunkSize, mapChunkSize, mapChunkSize, null, false);
//			MarkerIcon marketIcon = m.getMarkerIcon("CHUNK");
//			m.createMarkerSet(null, null, null, force);
//	        AreaMarker am = marketSet.createAreaMarker(UUID.randomUUID().toString(), "Chunk", false, worldLoader.getWorld().getName(), new double[1000], new double[1000], false);
//	        double[] d1 = {-50, -9};
//	        double[] d2 = {-720, -679};
//	        am.setCornerLocations(d1, d2);
//	        am.setLabel("test");
//	        am.setDescription("example test");
//		}
		Runnable runnable = () -> {
			for (int r = 1; mapChunkSize >= r; ++r) {
				for (int x = -r; r >= x; ++x) {
					if (x == -r || r == x) {
						for (int z = -r; r >= z; ++z) {
							chunkToLoad.add(new ChunkLoad(spawnLoader.getWorld(), x + spawnLoader.getX(), z + spawnLoader.getZ()));
						}
					} else {
						chunkToLoad.add(new ChunkLoad(spawnLoader.getWorld(), x + spawnLoader.getX(), -r + spawnLoader.getZ()));
						chunkToLoad.add(new ChunkLoad(spawnLoader.getWorld(), x + spawnLoader.getX(), r + spawnLoader.getZ()));
					}
				}
			}
			totalChunks = chunkToLoad.size();
			worldLoader.getStats().init(1, totalChunks, timeStarted);
			long diff1 = Utils.getCurrentTimeInSeconds() - timeStarted;
			worldLoader.getPlugin().getLogger().info(String.format("Chunks are identified | %d chunks will be check | started %s ago | %d threads will be used",
					totalChunks, Utils.hrDuration(diff1), threadsUses));

			ChunkStats stats = worldLoader.getStats();
			stats.setChunkAlready(0);
			if (chunkToLoad.isEmpty()) {
				end(false);
				return;
			}
			taskHandler.scheduleSyncRepeatingTask("generate.calculate.info2", () -> {
				stats.getStats();
			}, 0, 10, TimeUnit.SECONDS);
			taskHandler.scheduleSyncRepeatingTask("generate.calculate.info", () -> {
				stats.getStats();
				int lastXChunk, lastZChunk;
				if (stats.getLastchunk() == null) {
					lastXChunk = 0;
					lastZChunk = 0;
				} else {
					lastXChunk = stats.getLastchunk().getXChunk();
					lastZChunk = stats.getLastchunk().getZChunk();
				}
				worldLoader.getPlugin().getLogger().info(String.format("Calculate (1/2) %d/%d chunks - %d%% | %d chunks/s | last x z chunk %d %d | started %s ago | ETA %s - %s",
						stats.getChunkAlready(), totalChunks, stats.getPercentageChunk(), stats.getAverageChunksPerSecond(), lastXChunk, lastZChunk,
						Utils.hrDuration(stats.getTimeDiff()), stats.getDurationETA(), stats.getDateETA()));
			}, 30, 60, TimeUnit.SECONDS);
//			chunkToLoad.removeIf(cl -> world.isChunkGenerated(cl.xChunk, cl.zChunk));
		
			List<List<ChunkLoad>> lists = new Utils.DevideList<ChunkLoad>(chunkToLoad, threadsUses).nbList();
			for (Iterator<List<ChunkLoad>> its = lists.iterator(); its.hasNext();) {
				List<ChunkLoad> l = its.next();
				Iterator<ChunkLoad> it = l.iterator();
				Thread thread = new CalculateThread(stats, it);
				threads.add(thread);
				//worldLoader.getPlugin().getLogger().info(String.format("List size %d, thread id %d", l.size(), thread.getId()));
				thread.start();
			}
		};
		if (force) {
			worldLoader.getPlugin().getLogger().info(String.format("Calculation the order of chunks for a map of size %d of %d %d to %d %d", worldLoader.getMapSize(), worldLoader.getRealMapMinX(),
					worldLoader.getRealMapMinZ(), worldLoader.getRealMapMaxX(), worldLoader.getRealMapMaxZ()));
			taskHandler.runTaskAsynchronously("generate.calculate", runnable);
			return;
		}
		worldLoader.getPlugin().getLogger().info(String.format("Calculation the order of chunks for a map of size %d of %d %d to %d %d in %d seconds.", worldLoader.getMapSize(), worldLoader.getRealMapMinX(),
				worldLoader.getRealMapMinZ(), worldLoader.getRealMapMaxX(), worldLoader.getRealMapMaxZ(), 10));
		taskHandler.runTaskLater("generate.calculate", () -> {
			taskHandler.runTaskAsynchronously(runnable);
		}, 10, TimeUnit.SECONDS);
	}

	void end(boolean force) {
		long diff2 = Utils.getCurrentTimeInSeconds() - timeStarted;
		if (force) {
			taskHandler.terminateAllTasks();
			worldLoader.getPlugin().getLogger().info(String.format("Force stop calculation (1/2) %d was ready to be generated | started %s ago | %d threads",
					chunkToLoad.size(), Utils.hrDuration(diff2), threadsUses));
		} else {
			taskHandler.cancelAllTasks();
			worldLoader.getPlugin().getLogger().info(String.format("End calculation (1/2) %d to generate | started %s ago | %d threads",
				chunkToLoad.size(), Utils.hrDuration(diff2), threadsUses));
		}
//		taskHandler.cancelTask("generate.calculate.info");
		threads.forEach(Thread::interrupt);
		threads.clear();
		if (!force) {
			worldLoader.startGenerating(Lists.newArrayList(chunkToLoad));
			return;
		}
		int mapSize = worldLoader.getGameManager().getSettings().getMapSize().get();
		if (chunkToLoad.isEmpty()) {
			worldLoader.getPlugin().getLogger().info(String.format("World is already generate from %d %d to %d %d (size %d)", worldLoader.getRealMapMinX(),
					worldLoader.getRealMapMinZ(), worldLoader.getRealMapMaxX(), worldLoader.getRealMapMaxZ(), mapSize));
			//worldLoader.stop(false);
			return;
		}
		/*if (chunkToLoad.stream().map(ChunkLoad::getCode).distinct().count() != chunkToLoad.size()) {
			plugin.getLogger().log(Level.SEVERE, "Algo to calcul chunks add duplicating chunks");
		}*/
	}

	public class CalculateThread extends Thread {
		Iterator<ChunkLoad> it;

		ChunkStats stats;

		public CalculateThread(ChunkStats stats, Iterator<ChunkLoad> it) {
			this.stats = stats;
			this.it = it;
		}
	
		@Override
		public void run() {
			while (it.hasNext()) {
				if (threads.isEmpty()) {
					interrupt();
					return;
				}
				ChunkLoad chunk = it.next();
				if (worldLoader.getGameManager().getWorld().isChunkGenerated(chunk.getXChunk(), chunk.getZChunk()))
					chunkToLoad.remove(chunk);
				lock.lock();
				stats.setLastchunk(chunk);
				stats.addChunkAlready();
				lock.unlock();
				/*if (totalChunks - chunkAlreadyCalculate < 10) {
					worldLoader.getPlugin().getLogger().info(String.format("Il reste %d chunks", totalChunks - chunkAlreadyCalculate));
				}*/
			}
			//worldLoader.getPlugin().getLogger().info(String.format("Thread %d finish with %d", this.getId(), chunkAlreadyCalculate));
			if (stats.getChunkAlready() >= totalChunks) {
				stats.setChunkAlready(-1);
				end(false);
			}
		}
	}

	public int getTotalChunks() {
		return totalChunks;
	}
}
