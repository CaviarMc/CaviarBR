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
	private ChunkLoad lastChunkOperation;
	private Lock lock = new ReentrantLock();
	private int chunkAlreadyCalculate = 0;
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
		Runnable runnable = () -> {
			for (int r = 1; worldLoader.getMapChunkSize() >= r; ++r) {
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
			long diff1 = Utils.getCurrentTimeInSeconds() - timeStarted;
			worldLoader.getPlugin().getLogger().info(String.format("Chunks are identified | %d chunks will be check | started %s ago | %d threads will be used",
					totalChunks, Utils.hrDuration(diff1), threadsUses));

			chunkAlreadyCalculate = 0;
			if (chunkToLoad.isEmpty()) {
				end(false);
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
					lastXChunk = lastChunkOperation.getXChunk();
					lastZChunk = lastChunkOperation.getZChunk();
				}
				worldLoader.getPlugin().getLogger().info(String.format("Calculate (1/2) %d/%d chunks - %d%% | %d chunks/s | last x z chunk %d %d | started %s ago | ETA %s - %s",
					chunkAlreadyCalculate, totalChunks, percentageChunk, averageChunksPerSecond, lastXChunk, lastZChunk, Utils.hrDuration(timeDiff), Utils.hrDuration(timeToEndSecs),
					Utils.timestampToDateAndHour(Utils.getCurrentTimeInSeconds() + timeToEndSecs)));
			}, 30, 60, TimeUnit.SECONDS);
//			chunkToLoad.removeIf(cl -> world.isChunkGenerated(cl.xChunk, cl.zChunk));
			
			List<List<ChunkLoad>> lists = new Utils.DevideList<ChunkLoad>(chunkToLoad, threadsUses).nbList();
			for (Iterator<List<ChunkLoad>> its = lists.iterator(); its.hasNext();) {
				List<ChunkLoad> l = its.next();
				Iterator<ChunkLoad> it = l.iterator();
				Thread thread = new CalculateThread(it);
				threads.add(thread);
				//worldLoader.getPlugin().getLogger().info(String.format("List size %d, thread id %d", l.size(), thread.getId()));
				thread.start();
			}
		};
		if (force) {
			worldLoader.getPlugin().getLogger().info(String.format("Calculation the order of chunks for a map of %d %d to %d %d", worldLoader.getRealMapMinX(),
					worldLoader.getRealMapMinZ(), worldLoader.getRealMapMaxX(), worldLoader.getRealMapMaxZ()));
			taskHandler.runTaskAsynchronously("generate.calculate", runnable);
			return;
		}
		worldLoader.getPlugin().getLogger().info(String.format("Calculation the order of chunks for a map of %d %d to %d %d in %d seconds.", worldLoader.getRealMapMinX(),
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
		int mapSize = worldLoader.getGameManager().getSettings().getMapSize().get();
		if (chunkToLoad.isEmpty()) {
			worldLoader.getPlugin().getLogger().info(String.format("World is already generate from -%d -%d to %d %d (size %d)", worldLoader.getRealMapMinX(),
					worldLoader.getRealMapMinZ(), worldLoader.getRealMapMaxX(), mapSize));
			//worldLoader.stop(false);
			return;
		}
		/*if (chunkToLoad.stream().map(ChunkLoad::getCode).distinct().count() != chunkToLoad.size()) {
			plugin.getLogger().log(Level.SEVERE, "Algo to calcul chunks add duplicating chunks");
		}*/
		worldLoader.startGenerating(Lists.newArrayList(chunkToLoad));
	}
	
	public class CalculateThread extends Thread {
		Iterator<ChunkLoad> it;
		
		public CalculateThread(Iterator<ChunkLoad> it) {
			this.it = it;
		}
		
		@Override
		public void run() {
			while (it.hasNext()) {
				ChunkLoad chunk = it.next();
				if (worldLoader.getGameManager().getWorld().isChunkGenerated(chunk.getXChunk(), chunk.getZChunk()))
					chunkToLoad.remove(chunk);
				lastChunkOperation = chunk;
				lock.lock();
				++chunkAlreadyCalculate;
				lock.unlock();
				/*if (totalChunks - chunkAlreadyCalculate < 10) {
					worldLoader.getPlugin().getLogger().info(String.format("Il reste %d chunks", totalChunks - chunkAlreadyCalculate));
				}*/
			}
			//worldLoader.getPlugin().getLogger().info(String.format("Thread %d finish with %d", this.getId(), chunkAlreadyCalculate));
			if (chunkAlreadyCalculate >= totalChunks) {
				chunkAlreadyCalculate = -1;
				end(false);
			}
		}
	}

}
