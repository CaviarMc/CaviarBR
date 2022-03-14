package fr.caviar.br.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.caviar.br.CaviarStrings;

public class StatePlaying extends GameState {
	
	private Location treasure;
	private ItemStack[] compass;
	
	public StatePlaying(GameManager game, Location treasure) {
		super(game);
		
		this.treasure = treasure;
	}
	
	@Override
	public void start() {
		super.start();
		
		Location tmp = treasure;
		treasure = null;
		setTreasure(tmp);
		
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
	
	@Override
	public void end() {
		super.end();
		
		setTreasure(null);
	}
	
	private void setTreasure(Location treasure) {
		Bukkit.getOnlinePlayers().forEach(x -> x.setCompassTarget(treasure));
		Runnable set = treasure == null ? () -> {} : () -> treasure.getWorld()
				.getChunkAtAsync(treasure)
				.thenAccept(chunk -> {
					this.treasure = treasure;
					Block treasureBlock = treasure.getBlock();
					treasureBlock.setType(Material.BEDROCK);
					Block buttonBlock = treasureBlock.getRelative(BlockFace.UP);
					buttonBlock.setType(Material.CRIMSON_BUTTON, false);
					((Switch) buttonBlock.getBlockData()).setAttachedFace(AttachedFace.FLOOR);
				})
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return null;
				});
		if (this.treasure != null) {
			this.treasure.getWorld()
					.getChunkAtAsync(this.treasure)
					.thenAccept(chunk -> {
						Block treasureBlock = this.treasure.getBlock();
						treasureBlock.getRelative(BlockFace.UP).setType(Material.AIR);
						treasureBlock.setType(Material.AIR);
					})
					.exceptionally(throwable -> {
						throwable.printStackTrace();
						return null;
					})
					.thenRun(set);
		}else {
			set.run();
		}
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
		}
		player.setCompassTarget(treasure);
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		
		Location loc = event.getClickedBlock().getLocation();
		if (loc.subtract(0, 1, 0).equals(treasure)) {
			event.setCancelled(true);
			game.setState(new StateWin(game, game.getPlayers().get(event.getPlayer().getUniqueId())));
		}
	}
	
	@EventHandler
	public void onBreakBlock(BlockBreakEvent event) {
		Location loc = event.getBlock().getLocation();
		if (loc.equals(treasure) || loc.subtract(0, 1, 0).equals(treasure)) {
			event.setCancelled(true);
			CaviarStrings.STATE_PLAYING_BREAK_TREASURE.send(event.getPlayer());
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
