package fr.caviar.br.generate;

import javax.annotation.Nullable;

import org.bukkit.Chunk;
import org.bukkit.World;

import fr.caviar.br.CaviarBR;

public class ChunkLoad {
	private int xChunk;
	private int zChunk;
	@Nullable
	private Chunk chunk;
	private boolean isGenerate;
	@Nullable
	private Long code;
	private World world;

	public ChunkLoad() {}

	public ChunkLoad(World world, int xChunk, int zChunk) {
		this.world = world;
		this.xChunk = xChunk;
		this.zChunk = zChunk;
	}

	public ChunkLoad(Chunk chunk, int xChunk, int zChunk) {
		this.world = chunk.getWorld();
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
		CaviarBR plugin = CaviarBR.getInstance();
		if (!plugin.getServer().isPrimaryThread()) {
			plugin.getTaskManager().runTask(task);
		} else
			task.run();
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

	public int getXChunk() {
		return xChunk;
	}

	public int getZChunk() {
		return zChunk;
	}

	@Nullable
	public Chunk getChunk() {
		return chunk;
	}

}
