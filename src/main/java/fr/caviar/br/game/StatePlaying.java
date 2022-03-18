package fr.caviar.br.game;

import java.util.Iterator;

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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
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
	
	public Location getTreasure() {
		return treasure;
	}
	
	public void setTreasure(Location treasure) {
		Location oldTreasure = this.treasure;
		this.treasure = treasure;
		
		if (treasure != null) {
			Bukkit.getOnlinePlayers().forEach(x -> x.setCompassTarget(treasure));
			
			treasure.getWorld()
				.getChunkAtAsync(treasure)
				.thenAccept(chunk -> {
					placeTreasure(treasure);
				})
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return null;
				});
		}
		
		if (oldTreasure != null) {
			oldTreasure.getWorld()
				.getChunkAtAsync(oldTreasure)
				.thenAccept(chunk -> {
					removeTreasure(oldTreasure);
				})
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return null;
				});
		}
	}
	
	private void placeTreasure(Location loc) {
		Block treasureBlock = loc.getBlock();
		treasureBlock.setType(Material.BEDROCK);
		Block buttonBlock = treasureBlock.getRelative(BlockFace.UP);
		buttonBlock.setType(Material.CRIMSON_BUTTON, false);
		Switch buttonData = (Switch) buttonBlock.getBlockData();
		buttonData.setAttachedFace(AttachedFace.FLOOR);
		buttonBlock.setBlockData(buttonData);
	}
	
	private void removeTreasure(Location loc) {
		Block treasureBlock = loc.getBlock();
		treasureBlock.getRelative(BlockFace.UP).setType(Material.AIR);
		treasureBlock.setType(Material.AIR);
	}
	
	public ItemStack[] getCompass() {
		return compass;
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
			
			player.getInventory().setContents(getCompass());
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
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext();) {
			ItemStack item = iterator.next();
			if (item.getType() == Material.COMPASS) {
				iterator.remove();
				event.getItemsToKeep().add(item);
			}
		}
	}
	
	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().getType() == Material.COMPASS)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void onCraft(CraftItemEvent event) {
		if (event.getRecipe().getResult().getType() == Material.COMPASS)
			event.setCancelled(true);
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
