package fr.caviar.br.player;

import java.util.Map.Entry;
import java.util.AbstractMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import org.bukkit.entity.Player;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.api.CaviarPlugin;
import fr.caviar.br.api.config.ConfigSpigot;
import fr.caviar.br.cache.BasicCache;

public class PlayerHandler extends BasicCache<UUID, CaviarPlayerSpigot> {

	private static PlayerHandler INSTANCE;

	private static Entry<CaviarPlayerSpigot, ? extends Exception> loadPlayer(UUID uuid) {
		ConfigSpigot config = CaviarBR.getInstance().getPlayerConfig();
		@Nullable
		CaviarPlayerSpigot uPlayer = config.getPlayer(uuid);

		//result.accept(uPlayer, null);
		if (uPlayer != null)
			INSTANCE.put(uuid, uPlayer);
		// TODO load from db
		return new AbstractMap.SimpleEntry<>(uPlayer, null);
	}

	private CaviarPlugin plugin;

	public PlayerHandler(CaviarPlugin caviarPlugin) {
		super(PlayerHandler::loadPlayer, 1, TimeUnit.HOURS);
		this.plugin = caviarPlugin;
		INSTANCE = this;

		// Usless
		/*plugin.getConfig().addLoadTask("player_data", config ->
			Bukkit.getOnlinePlayers().forEach(p -> {
				if (this.getObjectCached(p.getUniqueId()) != null)
					return;
				this.get(p.getUniqueId(), (cPlayer, exception) -> {
					CaviarBR.getInstance().getLogger().log(Level.INFO, String.format("Succes load player from reload Config : name = %s uuid = %s group = %s",
							cPlayer.getName(), cPlayer.getUuid(), cPlayer.getGroup()));
				});
			})
		);*/
	}

	public CaviarPlayerSpigot createPlayer(Player player) {
		CaviarPlayerSpigot uPlayer = new CaviarPlayerSpigot(player);

		savePlayer(uPlayer);
		put(player.getUniqueId(), uPlayer);

		return uPlayer;
	}

	public void savePlayer(CaviarPlayerSpigot uPlayer) {
		if (uPlayer.isFakePlayer())
			return;
		ConfigSpigot config = plugin.getPlayerConfig();
		config.setPlayer(uPlayer);
		config.save();
	}

	public void removePlayer(CaviarPlayerSpigot uPlayer) {
		this.removeFromCache(uPlayer.getUuid());
	}
}
