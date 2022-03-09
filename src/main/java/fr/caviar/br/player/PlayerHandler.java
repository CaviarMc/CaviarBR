package fr.caviar.br.player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import fr.caviar.br.cache.BasicCache;
import fr.caviar.br.utils.Utils.BiConsumerCanFail;
import fr.caviar.br.utils.Utils.BiConsumerCanFail.BiConsumerException;

public class PlayerHandler extends BasicCache<UUID, UniversalPlayer> {

	private static BiConsumerCanFail<UUID, Consumer<UniversalPlayer>> loadPlayer = null;


	public PlayerHandler() {
		super(loadPlayer, 1, TimeUnit.HOURS);
		if (loadPlayer != null)
			loadPlayer = this::loadPlayer;
	}
	
	private void loadPlayer(UUID uuid, Consumer<UniversalPlayer> result) throws BiConsumerException {
		// load from db
	}
}
