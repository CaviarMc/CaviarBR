package fr.caviar.br.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.task.UniversalTask;
import fr.caviar.br.utils.Utils;

public class StatePlaying extends GameState {

	private Location treasure;
	private ItemStack[] compass;
	private ItemStack compassItem;

	public StatePlaying(GameManager game, Location treasure) {
		super(game);

		this.treasure = treasure;
	}

	@Override
	public void start() {
		super.start();

		compassItem = new ItemStack(Material.COMPASS);
		game.timestampStart = Utils.getCurrentTimeInSeconds();
		ItemMeta meta = compassItem.getItemMeta();
		meta.setDisplayName(CaviarStrings.ITEM_COMPASS_NAME.toString());
		compassItem.setItemMeta(meta);
		compass = new ItemStack[] { compassItem };
		game.getGamers().forEach(player -> {
			join(player, game.getPlayers().get(player.getUniqueId()));
		});
		CaviarStrings.STATE_PLAYING_START.broadcast();
		UniversalTask taskManager = game.getPlugin().getTaskManager();
		game.timestampNextCompass = Utils.getCurrentTimeInSeconds() + 60 * (game.getSettings().getWaitCompass().get() + game.getSettings().getWaitTreasure().get());
		waitCompass();
		taskManager.runTaskLater("playing.treasure", () -> {
			Location tmp = treasure;
			treasure = null;
			setTreasure(tmp);
			CaviarStrings.STATE_PLAYING_TREASURE_SPAWN.broadcast();
			taskManager.scheduleSyncRepeatingTask("playing.compass.give", this::giveCompass, game.getSettings().getWaitCompass().get(), game.getSettings().getWaitCompass().get(), TimeUnit.MINUTES);
		}, game.getSettings().getWaitTreasure().get(), TimeUnit.MINUTES);
		game.getWorld().setPVP(true);
	}

	@Override
	public void end() {
		super.end();
		UniversalTask taskManager = game.getPlugin().getTaskManager();
		taskManager.cancelTask("playing.compass.give");
		taskManager.cancelTask("playing.treasure");
		taskManager.cancelTask("playing.scoreboard.compass");
		taskManager.cancelTask("playing.scoreboard.compass_waiting");
		setTreasure(null);
	}

	public void giveCompass() {
		UniversalTask taskManager = game.getPlugin().getTaskManager();
		taskManager.cancelTask("playing.scoreboard.compass_waiting");
		Validate.notNull(treasure);
		CaviarStrings.STATE_PLAYING_COMPASS.broadcast();
		game.getSpigotPlayers().forEach((player, gamePlayer) -> {
			game.getGamers().forEach(x -> x.setCompassTarget(treasure));
			@NotNull
			HashMap<Integer, ItemStack> itemNotGive = player.getInventory().addItem(getCompass()[0]);
			if (itemNotGive.isEmpty())
				return;
			player.sendMessage(CaviarStrings.STATE_PLAYING_COMPASS_GROUND.toComponent());
			itemNotGive.forEach((amout, item) -> {
				item.setAmount(amout);
				player.getWorld().dropItem(player.getLocation(), item);
			});
		});
		taskManager.runTaskLater(this::removeCompassPower, game.getSettings().getCompassDuration().get(), TimeUnit.MINUTES);
		taskManager.scheduleSyncRepeatingTask("playing.scoreboard.compass", () -> {
			game.getPlugin().getServer().getOnlinePlayers().forEach(p -> game.getPlugin().getScoreboard().compassEndEffest(p));
		}, 0, 1, TimeUnit.SECONDS);
	}
	
	public void removeCompassPower() {
		game.getSpigotPlayers().forEach((player, gamePlayer) -> {
			player.setCompassTarget(game.getWorld().getSpawnLocation());
			player.getInventory().remove(compassItem);
		});
		UniversalTask taskManager = game.getPlugin().getTaskManager();
		taskManager.cancelTask("playing.scoreboard.compass");
		CaviarStrings.STATE_PLAYING_COMPASS_STOP.broadcast();
		game.timestampNextCompass = Utils.getCurrentTimeInSeconds() + 60 * game.getSettings().getWaitCompass().get();
		waitCompass();
	}

	public void waitCompass() {
		game.timestampCompassEnd = game.timestampNextCompass + 60 * game.getSettings().getCompassDuration().get();
		UniversalTask taskManager = game.getPlugin().getTaskManager();
		taskManager.scheduleSyncRepeatingTask("playing.scoreboard.compass_waiting", () -> {
			game.getPlugin().getServer().getOnlinePlayers().forEach(p -> game.getPlugin().getScoreboard().compassTreasureWaiting(p));
		}, 0, 1, TimeUnit.SECONDS);
	}

	public Location getTreasure() {
		return treasure;
	}

	public void setTreasure(Location treasure) {
		Location oldTreasure = this.treasure;
		this.treasure = treasure;

		if (treasure != null) {
			//Bukkit.getOnlinePlayers().forEach(x -> x.setCompassTarget(treasure));

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
				game.addSpectator(player);
			} else {
				player.teleport(gamePlayer.spawnLocation);
				player.getInventory().clear();
				gamePlayer.spawnLocation.getChunk().removePluginChunkTicket(game.getPlugin());
			}

		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

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
		Player player = event.getPlayer();
		this.getGame().addSpectator(player);
		for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext();) {
			ItemStack item = iterator.next();
			if (item.getType() == Material.COMPASS) {
				iterator.remove();
				event.getItemsToKeep().add(item);
			}
		}
		CaviarBR.getInstance().getNameTag().setSpectator(player);
		CaviarBR.getInstance().getNameTag().setSpectator(player);
	}

	// We'll allow users to drop compass
	/*@EventHandler 
	public void onDrop(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().getType() == Material.COMPASS)
			event.setCancelled(true);
	}*/

	// Need to remove chest interaction too if we remove drop item
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		if (event.getCursor() != null && event.getCursor().isSimilar(compassItem)) {
			if (!event.getClickedInventory().equals(event.getInventory())) {
				event.setCancelled(true);
			}
		}
		if (event.isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().isSimilar(compassItem)) {
			Player player = (Player) event.getWhoClicked();
			if (!player.getOpenInventory().getType().equals(InventoryType.CRAFTING)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onCraft(CraftItemEvent event) {
		if (event.getRecipe().getResult().getType() == Material.COMPASS)
			event.setCancelled(true);
	}

	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer gPlayer) {
		join(event.getPlayer(), gPlayer);
	}

	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer gPlayer) {
		Player player = event.getPlayer();
		if (game.getPlayers().containsKey(player.getUniqueId())) {
			player.setHealth(0);
			game.getPlayers().remove(player.getUniqueId());
			event.setQuitMessage(event.getQuitMessage() + " §cHe dies from disconnection");
		}
		this.getGame().getSpectator().remove(event.getPlayer());
		return false;
	}

}
