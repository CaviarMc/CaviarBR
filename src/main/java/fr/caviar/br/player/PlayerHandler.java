package fr.caviar.br.player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.bukkit.entity.Player;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.cache.BasicCache;
import fr.caviar.br.config.ConfigSpigot;

public class PlayerHandler extends BasicCache<UUID, CaviarPlayerSpigot> {

	private static PlayerHandler INSTANCE;

	private static void loadPlayer(UUID uuid, BiConsumer<CaviarPlayerSpigot, Exception> result) {
		ConfigSpigot config = CaviarBR.getInstance().getConfig();
		CaviarPlayerSpigot uPlayer = config.getPlayer(uuid);

		result.accept(uPlayer, null);
		INSTANCE.put(uuid, uPlayer);
		// TODO load from db
	}

	public PlayerHandler() {
		super(PlayerHandler::loadPlayer, 1, TimeUnit.HOURS);
		INSTANCE = this;
	}

	public CaviarPlayerSpigot createPlayer(Player player) {
		ConfigSpigot config = CaviarBR.getInstance().getConfig();
		CaviarPlayerSpigot uPlayer = new CaviarPlayerSpigot(player);

		put(player.getUniqueId(), uPlayer);
		config.setPlayer(uPlayer);
		config.save();

		return uPlayer;
	}

	public void savePlayer(CaviarPlayerSpigot uPlayer) {
		ConfigSpigot config = CaviarBR.getInstance().getConfig();
		config.setPlayer(uPlayer);
		config.save();
	}

	public void removePlayer(CaviarPlayerSpigot uPlayer) {
		this.removeFromCache(uPlayer.getUuid());
	}
}
