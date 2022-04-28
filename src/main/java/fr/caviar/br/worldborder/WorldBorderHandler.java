package fr.caviar.br.worldborder;

import org.bukkit.WorldBorder;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.utils.Utils;

public class WorldBorderHandler {

	WorldBorder worldBorder;
	int size;
	int nextSize;
	int finalSize;
	int maxTimeToFinalSecond;
	long setTime;
	long timeEnd;

	public WorldBorderHandler(WorldBorder worldBorder, int size, int finalSize, int maxTimeToFinalSecond) {
		this.worldBorder = worldBorder;
		this.size = size;
		this.finalSize = finalSize;
		this.maxTimeToFinalSecond = maxTimeToFinalSecond;
	}

	public void set() {
		worldBorder.setSize(size);
		CaviarBR.getInstance().getLogger().info(String.format("WorldBorder size is set to %d", size));
	}

	public void startReducing() {
		if (worldBorder.getSize() > size)
			worldBorder.setSize(size);
		worldBorder.setSize(finalSize, maxTimeToFinalSecond);
		setTime = Utils.getCurrentTimeInSeconds();
		timeEnd = setTime + maxTimeToFinalSecond;
		CaviarBR.getInstance().getLogger().info(String.format("WorldBorder size is %d and will be %d at %s", size, finalSize, Utils.timestampToDateAndHour(timeEnd)));
	}

}
