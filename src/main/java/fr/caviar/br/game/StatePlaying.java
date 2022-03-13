package fr.caviar.br.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.utils.observable.ObservableValue;

public class StatePlaying extends GameState {
	
	private ObservableValue<Location> treasure;
	private ItemStack[] compass;
	
	public StatePlaying(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		
		treasure = new ObservableValue<>(new Location(game.getWorld(), 100, 100, 0));
		treasure.observe("compass", () -> Bukkit.getOnlinePlayers().forEach(x -> x.setCompassTarget(treasure.get())));
		
		ItemStack compassItem = new ItemStack(Material.COMPASS);
		ItemMeta meta = compassItem.getItemMeta();
		meta.setDisplayName(CaviarStrings.ITEM_COMPASS_NAME.toString());
		compassItem.setItemMeta(meta);
		compass = new ItemStack[] { compassItem };
		
		Bukkit.getOnlinePlayers().forEach(player -> {
			join(player, game.getPlayers().get(player.getUniqueId()));
		});
		CaviarStrings.STATE_PLAYING_START.broadcast();
	}
	
	private void join(Player player, GamePlayer gamePlayer) {
		if (!gamePlayer.started) {
			gamePlayer.started = true;
			if (gamePlayer.spawnLocation == null) {
				game.getPlugin().getLogger().severe("No spawn location for player " + player.getName());
			}else {
				player.teleport(gamePlayer.spawnLocation);
				gamePlayer.spawnLocation.getChunk().removePluginChunkTicket(game.getPlugin());
			}
			
			player.getInventory().setContents(compass);
			player.setCompassTarget(treasure.get());
		}
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {
		join(event.getPlayer(), player);
	}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
		return false;
	}
	
}
