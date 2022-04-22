package fr.caviar.br.generate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Chunk;
import org.bukkit.World;

import fr.caviar.br.task.NativeTask;
import fr.caviar.br.utils.Utils;

public class GenerateChunk {

	private WorldLoader worldLoader;
	private NativeTask taskHandler = new NativeTask(this.getClass());
	private List<Thread> threads = new ArrayList<>();
	private List<ChunkLoad> chunksToLoad;
	private int threadsUses;
	private boolean async;
	private Lock mutex = new ReentrantLock();

	public GenerateChunk(WorldLoader worldLoader, List<ChunkLoad> chunksToLoad, int threadsUses, boolean async) {
		this.worldLoader = worldLoader;
		this.chunksToLoad = chunksToLoad;
		this.threadsUses = threadsUses;
		this.async = async;
	}

	
	void start() {
		worldLoader.getStats().init(2, chunksToLoad.size(), Utils.getCurrentTimeInSeconds());
		cleanQueue();
		if (async) {
			launchAsync();
		} else {
			launchSync();
		}
		ChunkStats stats = worldLoader.getStats();
		stats.setChunkAlready(0);
		taskHandler.scheduleSyncRepeatingTask("generate.chunk.info2", () -> {
			stats.getStats();
		}, 0, 10, TimeUnit.SECONDS);
		taskHandler.scheduleSyncRepeatingTask("generate.chunk.info", () -> {
			stats.getStats();
			int lastXChunk, lastZChunk;
			if (stats.getLastchunk() == null) {
				lastXChunk = 0;
				lastZChunk = 0;
			} else {
				lastXChunk = stats.getLastchunk().getXChunk();
				lastZChunk = stats.getLastchunk().getZChunk();
			}
			worldLoader.getPlugin().getLogger().info(String.format("Generate (2/2) %d/%d chunks - %d%% | %d chunks/s | last x z chunk %d %d | started %s ago | ETA %s - %s",
					stats.getChunkAlready(), chunksToLoad.size(), stats.getPercentageChunk(), stats.getAverageChunksPerSecond(), 
				lastXChunk, lastZChunk, Utils.hrDuration(stats.getTimeDiff()), stats.getDurationETA(),
				stats.getDateETA()));
		}, 1, 5, TimeUnit.MINUTES);
		worldLoader.getPlugin().getLogger().info(String.format("Generate (2/2) starting for %d chunks...", chunksToLoad.size()));
	}

	void end(boolean force) {
		ChunkStats stats = worldLoader.getStats();
		//taskHandler.terminateTask("generate.chunk.info");
		threads.forEach(Thread::interrupt);
//		threads.forEach(Thread::stop);
		threads.clear();
		int averageChunksPerSecond;
		if (stats.getChunkAlready() > 0 && chunksToLoad != null && chunksToLoad.size() > 0)
			averageChunksPerSecond = (int) (((float) stats.getChunkAlready() / chunksToLoad.size()) * 100);
		else
			averageChunksPerSecond = 0;
		if (force) {
			taskHandler.terminateAllTasks();
			worldLoader.getPlugin().getLogger().info(String.format("Generating is stopped | %d/%d chunks generate - %d%% | time taken %s",
					stats.getChunkAlready(), chunksToLoad != null ? chunksToLoad.size() : -1,
					averageChunksPerSecond, Utils.hrDuration(Utils.getCurrentTimeInSeconds() - stats.getTimeStarted())));
			worldLoader.getGameManager().setMapCub(worldLoader.getCub());
		}
		else {
			taskHandler.cancelAllTasks();
			worldLoader.getPlugin().getLogger().info(String.format("Generating is finish | %d/%d chunks generate - %d%% | time taken %s",
					stats.getChunkAlready(), chunksToLoad != null ? chunksToLoad.size() : -1,
					averageChunksPerSecond, Utils.hrDuration(Utils.getCurrentTimeInSeconds() - stats.getTimeStarted())));
		}
		if (chunksToLoad != null)
			chunksToLoad.clear();
		worldLoader.getStats().stop();
	}
	
	private void launchAsync() {
		List<List<ChunkLoad>> lists = new Utils.DevideList<ChunkLoad>(chunksToLoad, threadsUses).nbList();
		for (Iterator<List<ChunkLoad>> its = lists.iterator(); its.hasNext();) {
			Iterator<ChunkLoad> it = its.next().iterator();
			Thread t = new Thread(() -> loadChunkAsync(it));
			threads.add(t);
			t.start();
		}
		/*Iterator<ChunkLoad> it = chunksToLoad.iterator();
		Thread t = new Thread(() -> loadChunkAsync(it));
		t.start();
		threads.add(t);*/
	}

	private void launchSync() {
		Iterator<ChunkLoad> it = chunksToLoad.iterator();
		taskHandler.runTaskAsynchronously("generate.load", () -> {
			loadChunkSync(it);
		});
	}
	
	private void loadChunkAsync(Iterator<ChunkLoad> it) {
		ChunkStats stats = worldLoader.getStats();
		if (threads.isEmpty() || !it.hasNext()) {
			if (stats.getChunkAlready() >= chunksToLoad.size())
				worldLoader.stop(false);
			return;
		}
		World world = worldLoader.getWorld();
		ChunkLoad chunkL = it.next();
		world.getChunkAtAsync(chunkL.getXChunk(), chunkL.getZChunk(), true, chunk -> {
//		world.getChunkAtAsyncUrgently(chunkL.getXChunk(), chunkL.getZChunk()).thenAccept(chunk -> {
			chunkL.addChunk(chunk);
			mutex.lock();
			stats.setLastchunk(chunkL);
			stats.addChunkAlready();
			mutex.unlock();
			world.unloadChunkRequest(chunkL.getXChunk(), chunkL.getZChunk());
			loadChunkAsync(it);
		});
	}

	/*private void loadChunkAsync(Iterator<ChunkLoad> it) {
		if (!it.hasNext()) {
			if (chunkAlreadyGenerate >= chunksToLoad.size())
				worldLoader.stop(false);
			return;
		}
		Lock mutexChunk = new ReentrantLock();
		World world = worldLoader.getWorld();
		while (it.hasNext()) {
			ChunkLoad chunkL = it.next();
			mutexChunk.lock();
			world.getChunkAtAsync(chunkL.getXChunk(), chunkL.getZChunk()).thenAccept(chunk -> {
				mutexChunk.unlock();
				mutex.lock();
				++chunkAlreadyGenerate;
				lastChunkOperation = chunkL;
				chunkL.addChunk(chunk);
				mutex.unlock();
				world.unloadChunkRequest(chunkL.getXChunk(), chunkL.getZChunk());
				if (chunkAlreadyGenerate >= chunksToLoad.size())
					worldLoader.stop(false);
			});
		}
	}*/

	private void loadChunkSync(Iterator<ChunkLoad> it) {
		World world = worldLoader.getWorld();
		ChunkStats stats = worldLoader.getStats();
		while (it.hasNext()) {
			ChunkLoad chunkL = it.next();
			mutex.lock();
			Chunk chunk = world.getChunkAt(chunkL.getXChunk(), chunkL.getZChunk());
			chunkL.addChunk(chunk);
			mutex.lock();
			stats.setLastchunk(chunkL);
			stats.addChunkAlready();
			mutex.unlock();
			world.unloadChunkRequest(chunkL.getXChunk(), chunkL.getZChunk());
			if (stats.getChunkAlready() >= chunksToLoad.size())
				worldLoader.stop(false);
		}
	}

	private void cleanQueue() {
		if (chunksToLoad == null)
			return;
		boolean changes = false;
		int tempSize = chunksToLoad.size();
		if (chunksToLoad.removeIf(cl -> cl != null && cl.isGenerate())) {
			worldLoader.getPlugin().getLogger().info(String.format("Removed %d chunks from queue because they are already generated.", tempSize - (tempSize = chunksToLoad.size())));
			changes = true;
		}
		if (changes)
			worldLoader.getPlugin().getLogger().info(String.format("Number of chunks to generate : %d", chunksToLoad.size()));
	}
}
