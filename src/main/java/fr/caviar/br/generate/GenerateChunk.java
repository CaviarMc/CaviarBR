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
	private ChunkLoad lastChunkOperation;
	private long timeStarted;
	private int chunkAlreadyGenerate = 0;
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
		timeStarted = Utils.getCurrentTimeInSeconds();
		cleanQueue();
		if (async) {
			launchAsync();
		} else {
			launchSync();
		}
		taskHandler.scheduleSyncRepeatingTask("generate.chunk.info", () -> {
			long timeDiff = Utils.getCurrentTimeInSeconds() - timeStarted;
			long timeToEndSecs;
			int averageChunksPerSecond, percentageChunk;
			if (chunkAlreadyGenerate > 0 &&  chunksToLoad.size() > 0) {
				percentageChunk = (int) (((float) chunkAlreadyGenerate / chunksToLoad.size()) * 100);
				averageChunksPerSecond = (int) (chunkAlreadyGenerate / timeDiff);
				timeToEndSecs = (chunksToLoad.size() - chunkAlreadyGenerate) / averageChunksPerSecond;
			} else {
				percentageChunk = 0;
				averageChunksPerSecond = 0;
				timeToEndSecs = 0;
			}
			Chunk lastchunk;
			if (lastChunkOperation == null || lastChunkOperation.getChunk() == null) {
				lastchunk = worldLoader.getWorld().getSpawnLocation().getChunk();
			} else {
				lastchunk = lastChunkOperation.getChunk();
			}
			worldLoader.getPlugin().getLogger().info(String.format("Generate (2/2) %d/%d chunks - %d%% | %d chunks/s | last x z chunk %d %d | started %s ago | ETA %s - %s",
				chunkAlreadyGenerate, chunksToLoad.size(), percentageChunk, averageChunksPerSecond, 
				lastchunk.getX(), lastchunk.getZ(), Utils.hrDuration(timeDiff), Utils.hrDuration(timeToEndSecs),
				Utils.timestampToDateAndHour(Utils.getCurrentTimeInSeconds() + timeToEndSecs)));
		}, 1, 5, TimeUnit.MINUTES);
	}

	void end(boolean force) {
		//taskHandler.terminateTask("generate.chunk.info");
		threads.forEach(Thread::interrupt);
		threads.clear();
		int averageChunksPerSecond;
		if (chunkAlreadyGenerate > 0 && chunksToLoad != null && chunksToLoad.size() > 0)
			averageChunksPerSecond = (int) (((float) chunkAlreadyGenerate / chunksToLoad.size()) * 100);
		else
			averageChunksPerSecond = 0;
		if (force) {
			taskHandler.terminateAllTasks();
			worldLoader.getPlugin().getLogger().info(String.format("Generating is stopped | %d/%d chunks generate - %d%% | time taken %s",
					chunkAlreadyGenerate, chunksToLoad != null ? chunksToLoad.size() : -1, averageChunksPerSecond, Utils.hrDuration(Utils.getCurrentTimeInSeconds() - timeStarted)));
		}
		else {
			taskHandler.cancelAllTasks();
			worldLoader.getPlugin().getLogger().info(String.format("Generating is finish | %d/%d chunks generate - %d%% | time taken %s",
					chunkAlreadyGenerate, chunksToLoad != null ? chunksToLoad.size() : -1, averageChunksPerSecond, Utils.hrDuration(Utils.getCurrentTimeInSeconds() - timeStarted)));
		}
		if (chunksToLoad != null)
			chunksToLoad.clear();
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
		if (!it.hasNext()) {
			if (chunkAlreadyGenerate >= chunksToLoad.size())
				worldLoader.stop(false);
			return;
		}
		World world = worldLoader.getWorld();
		ChunkLoad chunkL = it.next();
		world.getChunkAtAsync(chunkL.getXChunk(), chunkL.getZChunk(), true, chunk -> {
//		world.getChunkAtAsyncUrgently(chunkL.getXChunk(), chunkL.getZChunk()).thenAccept(chunk -> {
			lastChunkOperation = chunkL;
			chunkL.addChunk(chunk);
			mutex.lock();
			++chunkAlreadyGenerate;
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
		while (it.hasNext()) {
			ChunkLoad chunkL = it.next();
			mutex.lock();
			Chunk chunk = world.getChunkAt(chunkL.getXChunk(), chunkL.getZChunk());
			++chunkAlreadyGenerate;
			mutex.unlock();
			lastChunkOperation = chunkL;
			chunkL.addChunk(chunk);
			world.unloadChunkRequest(chunkL.getXChunk(), chunkL.getZChunk());
			if (chunkAlreadyGenerate >= chunksToLoad.size())
				worldLoader.stop(false);
		}
	}

	private void cleanQueue() {
		if (chunksToLoad == null)
			return;
		boolean changes = false;
		int tempSize = chunksToLoad.size();
		if (chunksToLoad.removeIf(cl -> cl.isGenerate())) {
			worldLoader.getPlugin().getLogger().info(String.format("Removed %d chunks from queue because they are already generated.", tempSize - (tempSize = chunksToLoad.size())));
			changes = true;
		}
		if (changes)
			worldLoader.getPlugin().getLogger().info(String.format("Number of chunks to generate : %d", chunksToLoad.size()));
	}

}
