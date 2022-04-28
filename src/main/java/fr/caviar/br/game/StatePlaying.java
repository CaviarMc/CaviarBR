package fr.caviar.br.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.Validate;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.permission.Perm;
import fr.caviar.br.task.TaskManagerSpigot;
import fr.caviar.br.utils.Utils;
import fr.caviar.br.worldborder.WorldBorderHandler;

public class StatePlaying extends GameState {

	private ItemStack[] compass;
	private ItemStack compassItem;
	private TaskManagerSpigot taskManager;
	private WorldBorderHandler wbHandler;

	public StatePlaying(GameManager game) {
		super(game);
		this.taskManager = new TaskManagerSpigot(game.getPlugin(), this.getClass());
		this.wbHandler = new WorldBorderHandler(game.getWorld().getWorldBorder(), game.getSettings().getMapSize().get() * 2,
				game.getSettings().getFinalSize().get(), game.getSettings().getMaxTimeGame().getInSecond());
		this.wbHandler.set();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void start() {
		super.start();

		compassItem = new ItemStack(Material.COMPASS);
		game.timestampStart = Utils.getCurrentTimeInSeconds();
		game.timestampTreasureSpawn = game.timestampStart + game.getSettings().getWaitTreasure().getInSecond();
		game.timestampNextCompass = game.timestampStart + game.getSettings().getWaitCompass().getInSecond() + game.getSettings().getWaitTreasure().getInSecond();
		ItemMeta meta = compassItem.getItemMeta();
		if (game.getPlugin().isPaper()) {
			meta.displayName(CaviarStrings.ITEM_COMPASS_NAME.toComponent());
		} else {
			meta.setDisplayName(CaviarStrings.ITEM_COMPASS_NAME.toString());
		}
		compassItem.setItemMeta(meta);
		compass = new ItemStack[] { compassItem };
		game.getGamers().forEach(player -> {
			join(player, game.getPlayers().get(player.getUniqueId()));
		});
		CaviarStrings.STATE_PLAYING_START.broadcast();
		
		waitCompass();
		taskManager.runTaskLater("playing.treasure", () -> {
			Location tmp = game.getTreasure();
			game.setTreasure(null);
			setTreasure(tmp);
			if (!taskManager.cancelTask("playing.scoreboard.treasure_waiting")) {
				this.getGame().getPlugin().getLogger().severe("Can't cancel task playing.scoreboard.treasure_waiting");
			}
			CaviarStrings.STATE_PLAYING_TREASURE_SPAWN.broadcast();
			taskManager.scheduleSyncRepeatingTask("playing.compass.give", this::giveCompass, game.getSettings().getWaitCompass().getInMinute(), game.getSettings().getWaitCompass().getInMinute(), TimeUnit.MINUTES);
		}, game.getSettings().getWaitTreasure().get(), TimeUnit.MINUTES);
		taskManager.scheduleSyncRepeatingTask("playing.scoreboard.treasure_waiting", () -> {
			game.getPlugin().getServer().getOnlinePlayers().forEach(p -> game.getPlugin().getScoreboard().treasureWaiting(p));
		}, 0, 1, TimeUnit.SECONDS);

		if (wbHandler != null) {
			wbHandler.startReducing();
		} else {
			this.getGame().getPlugin().getLogger().severe("Can't start reducing WorldBorder");
		}

		World world = game.getWorld();
		world.setPVP(true);
		world.setGameRule(GameRule.REDUCED_DEBUG_INFO, false);
		world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
		world.setTime(0);
		
		game.getSettings().isAllowedSpectator().observe("allowSpectator", () -> {
			Boolean newSetting = game.getSettings().isAllowedSpectator().get();
			if (!newSetting) {
				game.getSpectators().keySet().stream().filter(p -> !Perm.VIP_SPECTATOR.has(p)).forEach(p -> {
					p.kick(CaviarStrings.LOGIN_SCREEN_KICK_SPEC.toComponent());
				});
			}
		});
	}

	@Override
	public void end() {
		super.end();
//		taskManager.cancelTask("playing.compass.give");
//		taskManager.cancelTask("playing.treasure");
//		taskManager.cancelTask("playing.scoreboard.compass");
//		taskManager.cancelTask("playing.scoreboard.compass_waiting");
		taskManager.cancelAllTasks();
		setTreasure(null);
	}

	public void giveCompass() {
		taskManager.cancelTask("playing.scoreboard.compass_waiting");
		Validate.notNull(game.getTreasure());
		CaviarStrings.STATE_PLAYING_COMPASS.broadcast(game.getSettings().getCompassDuration().getInSecond());
		game.getSpigotPlayers().forEach((player, gamePlayer) -> {
			player.setCompassTarget(game.getTreasure());
			HashMap<Integer, ItemStack> itemNotGive = player.getInventory().addItem(getCompass()[0]);
			if (itemNotGive.isEmpty())
				return;
			player.sendMessage(CaviarStrings.STATE_PLAYING_COMPASS_GROUND.toComponent());
			itemNotGive.forEach((amout, item) -> {
				item.setAmount(amout);
				player.getWorld().dropItem(player.getLocation(), item);
			});
		});
		taskManager.runTaskLater(this::removeCompassPower, game.getSettings().getCompassDuration().getInSecond(), TimeUnit.SECONDS);
		taskManager.scheduleSyncRepeatingTask("playing.scoreboard.compass", () -> {
			game.getPlugin().getServer().getOnlinePlayers().forEach(p -> game.getPlugin().getScoreboard().compassEndEffest(p));
		}, 0, 1, TimeUnit.SECONDS);
	}
	
	public void removeCompassPower() {
		game.getSpigotPlayers().forEach((player, gamePlayer) -> {
			player.setCompassTarget(game.getWorld().getSpawnLocation());
			player.getInventory().remove(compassItem);
		});
		
		taskManager.cancelTask("playing.scoreboard.compass");
		CaviarStrings.STATE_PLAYING_COMPASS_STOP.broadcast(game.getSettings().getWaitCompass().getInMinute());
		game.timestampNextCompass = Utils.getCurrentTimeInSeconds() + game.getSettings().getWaitCompass().getInSecond();
		waitCompass();
	}


	public void waitCompass() {
		setCompassEnd();
		
		taskManager.scheduleSyncRepeatingTask("playing.scoreboard.compass_waiting", () -> {
			game.getPlugin().getServer().getOnlinePlayers().forEach(p -> game.getPlugin().getScoreboard().compassWaiting(p));
		}, 0, 1, TimeUnit.SECONDS);
	}

	private void setCompassEnd() {
		game.timestampCompassEnd = game.timestampNextCompass + game.getSettings().getCompassDuration().getInSecond();
	}

	public void setTreasure(Location treasure) {
		Location oldTreasure = game.getTreasure();
		game.setTreasure(treasure);

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
		if (loc.subtract(0, 1, 0).equals(game.getTreasure())) {
			event.setCancelled(true);
			if (game.getGamers().size() > game.getSettings().getMinPlayerToWin().get()) {
				CaviarStrings.STATE_PLAYING_MORE_PLAYERS.send(event.getPlayer());
				return;
			}
			GamePlayer p = game.getPlayers().get(event.getPlayer().getUniqueId());
			if (p != null)
				game.setState(new StateWin(game, p));
		}
	}

	@EventHandler
	public void onBreakBlock(BlockBreakEvent event) {
		Location loc = event.getBlock().getLocation();
		if (loc.equals(game.getTreasure()) || loc.subtract(0, 1, 0).equals(game.getTreasure())) {
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
		event.setDeathSound(Sound.ENTITY_WITHER_DEATH);
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

	@SuppressWarnings("deprecation")
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer gPlayer) {
		Player player = event.getPlayer();
		if (game.getPlayers().containsKey(player.getUniqueId())) {
			player.setHealth(0);
			game.getPlayers().remove(player.getUniqueId());
			event.setQuitMessage(event.getQuitMessage() + " §cHe dies from disconnection");
		}
		this.getGame().getSpectators().remove(event.getPlayer());
		return false;
	}

	public WorldBorderHandler getWbHandler() {
		return wbHandler;
	}

}
