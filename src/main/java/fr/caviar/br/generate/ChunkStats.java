package fr.caviar.br.generate;

import fr.caviar.br.utils.Utils;

public class ChunkStats {

	private int chunkSize;
	private long timeStarted;

	private int step = -1;
	private int percentageChunk;
	private long timeDiff;
	private long timeEnd;
	private int averageChunksPerSecond;
	private int chunkAlreadyGenerate;
	private ChunkLoad lastchunk;
	
	private Runnable run;

	public ChunkStats(Runnable run) {
		this.run = run;
	}

	public void init(int step, int chunkSize, long timeStarted) {
		this.step = step;
		this.chunkSize = chunkSize;
		this.timeStarted = timeStarted;
		this.chunkAlreadyGenerate = 0;
		getStats();
	}
	
	public void stop() {
		if (this.step == -2)
			return;
		this.step = -2;
		this.chunkSize = 0;
		this.timeStarted = 0;
		percentageChunk = 0;
		averageChunksPerSecond = 0;
		timeEnd = 0;
		run.run();
	}

	void getStats() {
		long now = Utils.getCurrentTimeInSeconds();
		timeDiff = now - timeStarted;
		if (timeDiff != 0 && chunkAlreadyGenerate > 0 && chunkSize > 0) {
			percentageChunk = (int) (((float) chunkAlreadyGenerate / chunkSize) * 100f);
			averageChunksPerSecond = (int) (chunkAlreadyGenerate / timeDiff);
			timeEnd = now + (chunkSize - chunkAlreadyGenerate) / averageChunksPerSecond;
		} else {
			percentageChunk = 0;
			averageChunksPerSecond = 0;
			timeEnd = 0;
		}
		run.run();
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public int getPercentageChunk() {
		return percentageChunk;
	}
	
	public String getDurationStarted() {
		return Utils.hrFormatDuration(timeStarted);
	}
	
	public String getDurationETA() {
		return Utils.hrFormatDuration(timeEnd);
	}
	
	public String getDateETA() {
		return Utils.timestampToDateAndHour(timeEnd);
	}

	public int getStep() {
		return step;
	}

	public long getTimeStarted() {
		return timeStarted;
	}

	public long getTimeDiff() {
		return timeDiff;
	}

	public long getTimeEnd() {
		return timeEnd;
	}

	public int getAverageChunksPerSecond() {
		return averageChunksPerSecond;
	}

	public int getChunkAlready() {
		return chunkAlreadyGenerate;
	}

	public ChunkLoad getLastchunk() {
		return lastchunk;
	}
	
	public void setChunkAlready(int chunkAlreadyGenerate) {
		this.chunkAlreadyGenerate = chunkAlreadyGenerate;
	}

	public void addChunkAlready() {
		++this.chunkAlreadyGenerate;
	}

	public void setLastchunk(ChunkLoad lastchunk) {
		this.lastchunk = lastchunk;
	}
	
}
