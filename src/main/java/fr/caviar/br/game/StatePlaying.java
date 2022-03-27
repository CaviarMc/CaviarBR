package fr.caviar.br.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.HumanEntity;
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
import org.jetbrains.annotations.NotNull;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.task.UniversalTask;
import fr.caviar.br.utils.Utils;

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

		setTreasure(null);

		ItemStack compassItem = new ItemStack(Material.COMPASS);
		game.timestampSec = Utils.getCurrentTimeInSeconds();
		ItemMeta meta = compassItem.getItemMeta();
		meta.setDisplayName(CaviarStrings.ITEM_COMPASS_NAME.toString());
		compassItem.setItemMeta(meta);
		compass = new ItemStack[] { compassItem };
		Bukkit.getOnlinePlayers().forEach(player -> {
			join(player, game.getPlayers().get(player.getUniqueId()));
		});
		CaviarStrings.STATE_PLAYING_START.broadcast();
		UniversalTask taskManager = game.getPlugin().getTaskManager();
		Integer waitCompassSettings = game.getSettings().getWaitCompass().get() + game.getSettings().getCompassDuration().get();
		
		Bukkit.getOnlinePlayers().forEach(player -> {
			game.getPlugin().getScoreboard().compassTreasureWaiting(player, game.timeNextCompass);
		});
		taskManager.runTaskLater("treasure", () -> {
			Location tmp = treasure;
			treasure = null;
			setTreasure(tmp);
			CaviarStrings.STATE_PLAYING_TREASURE_SPAWN.broadcast();
			taskManager.scheduleSyncRepeatingTask("compass.give", this::giveCompass, game.getSettings().getWaitCompass().get(), waitCompassSettings, TimeUnit.MINUTES);
		}, game.getSettings().getWaitTreasure().get());
		game.getWorld().setPVP(true);
	}

	@Override
	public void end() {
		super.end();
		UniversalTask taskManager = game.getPlugin().getTaskManager();
		taskManager.cancelTaskByName("compass.give");
		taskManager.cancelTaskByName("treasure");
		setTreasure(null);
	}

	public void giveCompass() {
		int waitCompassSettings = game.getSettings().getWaitCompass().get() + game.getSettings().getCompassDuration().get();
		int compassDuration = game.getSettings().getCompassDuration().get();
		game.timeNextCompass = Utils.getCurrentTimeInSeconds() + 60 * waitCompassSettings;
		game.timeCompassDuration = Utils.getCurrentTimeInSeconds() + 60 * compassDuration;
		CaviarStrings.STATE_PLAYING_COMPASS.broadcast();
		game.getSpigotPlayers().forEach((player, gamePlayer) -> {
			giveCompass(player);
			game.getPlugin().getScoreboard().compassTreasureWaiting(player, game.timeCompassDuration);
		});
		game.getPlugin().getTaskManager().runTaskLater(this::removeCompassPower, game.getSettings().getCompassDuration().get(), TimeUnit.MINUTES);
	}

	public void giveCompass(Player player) {
		@NotNull
		HashMap<Integer, ItemStack> itemNotGive = player.getInventory().addItem(getCompass()[0]);

		if (itemNotGive.isEmpty())
			return;
		player.sendMessage(CaviarStrings.STATE_PLAYING_COMPASS_GROUND.toComponent());
		itemNotGive.forEach((amout, item) -> {
			item.setAmount(amout);
			player.getWorld().dropItem(player.getLocation(), item);
		});
	}

	public void removeCompassPower() {
		game.getSpigotPlayers().forEach((player, gamePlayer) -> {
			removeCompassPower(player);
		});
	}

	private void removeCompassPower(Player player) {
		player.setCompassTarget(null);
		player.getInventory().remove(compass[0]);
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
				game.addSpectator(player);
			} else {
				player.teleport(gamePlayer.spawnLocation);
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

	/* 
	 * Need to remove chest interaction too if we remove drop item
	@EventHandler 
	public void onDrop(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().getType() == Material.COMPASS)
			event.setCancelled(true);
	}*/

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
			
			
		} else {
			this.getGame().getSpectator().remove(event.getPlayer());
		}
		return false;
	}

}
