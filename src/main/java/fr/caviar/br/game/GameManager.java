package fr.caviar.br.game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.commands.GameAdminCommand;
import fr.caviar.br.game.commands.SettingsCommand;
import fr.caviar.br.generate.WorldLoader;
import fr.caviar.br.permission.Perm;
import fr.caviar.br.utils.Cuboid;
import fr.caviar.br.utils.Utils;
import fr.caviar.br.worldborder.WorldBorderHandler;

public class GameManager {

	private static final List<Material> UNSPAWNABLE_ON = Arrays.asList(
			Material.LAVA, Material.CACTUS, Material.MAGMA_BLOCK, // because they deal damage
			Material.WATER, Material.BUBBLE_COLUMN, Material.KELP, Material.KELP_PLANT, Material.TALL_SEAGRASS, Material.CONDUIT, // because it's in water
			Material.ICE, Material.FROSTED_ICE, Material.BLUE_ICE // because it means on a frozen river
	);

	private final CaviarBR plugin;
	private final GameSettings settings;
	private final Map<UUID, GamePlayer> players = new HashMap<>();
	private final Map<Player, GamePlayer> spectator = new HashMap<>();
	private final WorldLoader worldLoader;
	protected long timestampStart;
	protected long timestampTreasureSpawn;
	protected long timestampNextCompass;
	protected long timestampCompassEnd;
	private Location treasure = null;
	private Cuboid mapCub = null;
	private Location tmpMaxDistance = null;
	private List<Location> spawnPoints;

	private GameState state;
	
	private World world;
	
	public GameManager(CaviarBR plugin) {
		this.plugin = plugin;
		settings = new GameSettings(this);
		worldLoader = new WorldLoader(plugin);
	}
	
	public void enable() {
		world = Bukkit.getWorlds().get(0);
		world.setStorm(false);
		world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
		world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
		world.setTime(0);
		setState(new StateWait(this));
		new SettingsCommand(this);
		new GameAdminCommand(this);
		worldLoader.addGameManager(this);
		prepareLocation(0, 0, tloc -> {
			tloc = tloc.add(0, 1, 0);
			this.getPlugin().getLogger().info("Found world spawn at " + Utils.locToStringH(tloc));
			world.setSpawnLocation(tloc);
			world.getWorldBorder().setCenter(world.getSpawnLocation());
			this.calculateTreasureSpawnPoint(treasure -> {
//				calcSpawnPoint((spawnPoints, maxDistance) -> worldLoader.start(false));
				worldLoader.start(false);
			});
		}, new AtomicInteger(1), 1);
	}
	
	public void disable() {
		if (state != null) {
			state.end();
			state = null;
		}
		if (worldLoader != null)
			worldLoader.stop(true);
	}

	public void calcSpawnPoint(BiConsumer<List<Location>, Location> consumer) {
		int playerRadius = settings.getPlayersRadius().get();
		int nbSpawnPoints = settings.getMaxPlayers().get();
		BiConsumer<List<Location>, Location> biConsumer = (list, max) -> {
//			int distGameTreasure = (int) Math.round(max.distance(treasure));
//			if (distGameTreasure > settings.getMapSize().get()) {
//				int newMapSize = ((int) distGameTreasure / 100) * 100 + 100;
//				plugin.getLogger().severe(String.format("The farthest is out of map, we need to expend the map from %d to %d.", settings.getMapSize().get(), newMapSize));
//				settings.getMapSize().set(newMapSize);
//			}
			spawnPoints = list;
			if (consumer != null)
				consumer.accept(list, max);
		};
		getSpawnPoints(playerRadius, nbSpawnPoints, biConsumer);
	}

	public void getSpawnPoints(int playerRadius, int nbSpawnPoints, BiConsumer<List<Location>, Location> consumer) {
		plugin.getTaskManager().runTaskAsynchronously("spawnPoints", () -> {
			List<Location> spawnPoints = new ArrayList<>();
			tmpMaxDistance = null;
			for (int i = 0; i < nbSpawnPoints; i++) {
				double theta = i * 2 * Math.PI / nbSpawnPoints;

				int i2 = i;
				int playerX = (int) (treasure.getBlockX() + playerRadius * Math.cos(theta));
				int playerZ = (int) (treasure.getBlockZ() + playerRadius * Math.sin(theta));
				
				Cuboid cub = mapCub;
				if (cub != null) {
					if (!cub.isIn(cub.getWorld(), playerX, 0, playerZ)) {
						plugin.getLogger().severe(String.format("Found spawnpoint in %d %d out of map (%d %d to %d %d).", playerX, playerZ,
								cub.getMin().getBlockX(), cub.getMin().getBlockZ(), cub.getMax().getBlockX(), cub.getMax().getBlockZ()));
					}
				}

				int chunkX = playerX >> 4;
				int chunkZ = playerZ >> 4;
				if (!world.isChunkGenerated(chunkX, chunkZ)) {
					plugin.getLogger().info(String.format("Chunk x=%d - z=%d is not generated, wait for it before check if it is a good spawnpoint.", playerX, playerZ));
				}
				this.prepareLocation(playerX, playerZ, ploc -> {
					//if (!isRunning()) return;
					plugin.getLogger().info("Found spawnpoint n°" + i2 + " in " + Utils.locToStringH(ploc));

//					addSpawnPoint(ploc);
					if (treasure != null && (tmpMaxDistance == null || ploc.distance(treasure) > tmpMaxDistance.distance(treasure)))
						tmpMaxDistance = ploc;
					spawnPoints.add(ploc);
					
//					if (settings.isDebug().get()) {
//			            Shulker shulk = (Shulker) ploc.getWorld().spawnEntity(ploc, EntityType.SHULKER);
//			            shulk.setInvisible(true);
//			            shulk.setAI(false);
//			            shulk.setGlowing(true);
//			            shulkers.add(shulk);
//					}
					if (spawnPoints.size() == nbSpawnPoints && consumer != null) {
						consumer.accept(spawnPoints, tmpMaxDistance);
					}
				}, new AtomicInteger(1), 1);
			}
		});
	}

	public void calculateTreasureSpawnPoint(Consumer<Location> consumer) {
		if (state != null && !(state instanceof StateWait)) {
			this.getPlugin().getLogger().warning("Can't calculte treasure while they are a game");
			return;
		}
		int treasureRaduis = this.getSettings().getTreasureRaduis().get();
		Random random = new Random();
		int treasureX = random.nextInt(-treasureRaduis, treasureRaduis);
		int treasureZ = random.nextInt(-treasureRaduis, treasureRaduis);
		this.getPlugin().getLogger().info("Trying to find treasure.");
		prepareLocation(treasureX, treasureZ, tloc -> {
			this.getPlugin().getLogger().info("Found treasure at " + Utils.locToStringH(tloc));
			treasure = tloc.add(0, 1, 0);
			if (consumer != null)
				consumer.accept(treasure);
		}, new AtomicInteger(1), 1);
	}

	public void prepareLocation(int x, int z, Consumer<Location> consumer, AtomicInteger operations, int chunks) {
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		this.getWorld().getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
			int y = chunk.getWorld().getHighestBlockYAt(x, z);
			Block block = chunk.getBlock(x - (chunkX << 4), y, z - (chunkZ << 4));
			/*List<Block> listBlocks = new ArrayList<>(9);
			for (int tempX = -1; tempX <= 1; ++tempX) {
				for (int tempZ = -1; tempZ <= 1; ++tempZ) {
					listBlocks.add(block.getLocation().add(tempX, 0, tempZ).getBlock());
				}
			}
			if (!listBlocks.stream().allMatch(this::isGoodBlock)) {*/
			if (!isGoodBlock(block)) {
				tryChunk(chunk, consumer, false, operations, chunks);
			}else {
				this.getPlugin().getLogger().info("Success in " + operations + " operations, in " + chunks + " chunks.");
				consumer.accept(new Location(this.getWorld(), x, y, z));
			}
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
			consumer.accept(new Location(this.getWorld(), 0.5, 80, 0.5));
			return null;
		});
	}
	
	private Location getNicestBlock(Chunk chunk, AtomicInteger operations) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				operations.incrementAndGet();
				int globalX = (chunk.getX() << 4) + x;
				int globalZ = (chunk.getZ() << 4) + z;
				int y = chunk.getWorld().getHighestBlockYAt(globalX, globalZ);
				Block block = chunk.getBlock(x, y, z);
				
				if (!isGoodBlock(block))
					continue;
				List<Block> listBlocks = new ArrayList<>(9);
				for (int tempX = -1; tempX <= 1; ++tempX) {
					for (int tempZ = -1; tempZ <= 1; ++tempZ) {
						listBlocks.add(block.getLocation().add(tempX, 0, tempZ).getBlock());
					}
				}
				List<Block> listBlocksAir = new ArrayList<>(18);
				for (int tempX = -1; tempX <= 1; ++tempX) {
					for (int tempZ = -1; tempZ <= 1; ++tempZ) {
						listBlocksAir.add(block.getLocation().add(tempX, 1, tempZ).getBlock());
						listBlocksAir.add(block.getLocation().add(tempX, 2, tempZ).getBlock());
					}
				}
				if (listBlocksAir.stream().allMatch(b -> b.isPassable()) && listBlocks.stream().allMatch(this::isGoodBlock)) {
					return block.getLocation();
				}
			}
		}
		return null;
	}
	
	private boolean isGoodBlock(Block block) {
		if (block.getY() < 60 || block.isEmpty()) return false;
		Material blockType = block.getType();
		if (UNSPAWNABLE_ON.contains(blockType)) return false;
		
		if (Tag.UNDERWATER_BONEMEALS.isTagged(blockType)) return false;
		if (Tag.LEAVES.isTagged(blockType)) return false;
		return true;
	}
	
	private void tryChunk(Chunk chunk, Consumer<Location> consumer, boolean xChanged, AtomicInteger operations, int chunks) {
		Location location = getNicestBlock(chunk, operations);
		if (location == null) { // have not found good spawnpoint in this chunk
			this.getWorld()
				.getChunkAtAsync(chunk.getX() + (xChanged ? 0 : 1), chunk.getZ() + (xChanged ? 1 : 0))
				.thenAccept(next -> tryChunk(next, consumer, !xChanged, operations, chunks + 1))
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return null;
				});
		}else {
			this.getPlugin().getLogger().info("Success in " + operations + " operations, in " + chunks + " chunks.");
			consumer.accept(location);
		}
	}

	public Location getTreasure() {
		return treasure;
	}

	public void setTreasure(Location treasure) {
		this.treasure = treasure;
	}

	public CaviarBR getPlugin() {
		return plugin;
	}
	
	public GameSettings getSettings() {
		return settings;
	}
	
	public WorldLoader getWorldLoader() {
		return worldLoader;
	}
	
	public GameState getState() {
		return state;
	}

	public World getWorld() {
		return world;
	}
	
	public Map<UUID, GamePlayer> getPlayers() {
		return players;
	}
	
	public boolean isGamer(Player player) {
		return players.containsKey(player.getUniqueId());
	}
	
	public Set<Player> getGamers() {
		return getAllPlayers().stream().filter(this::isGamer).collect(Collectors.toSet());
	}
	
	public @Nonnull Collection<? extends Player> getAllPlayers() {
		return plugin.getServer().getOnlinePlayers();
	}
	
	public Map<Player, GamePlayer> getSpigotPlayers() {
		return players.entrySet().stream().collect(Collectors.toMap(entry -> Bukkit.getPlayer(entry.getKey()), entry -> entry.getValue()));
	}
	
	public Map<Player, GamePlayer> getSpectators() {
		return spectator;
	}
	
	public Map<Player, GamePlayer> getModerators() {
		Map<Player, GamePlayer> moderators = new HashMap<>();
		getSpigotPlayers().forEach((player, gamePlayer) -> {
			if (Perm.MODERATOR.has(player))
				moderators.put(player, gamePlayer);
		});
		getSpectators().forEach((player, gamePlayer) -> {
			if (Perm.MODERATOR.has(player))
				moderators.put(player, gamePlayer);
		});
		return moderators;
	}
	
	public GamePlayer addSpectator(Player player) {
		GamePlayer gamePlayer = players.remove(player.getUniqueId());
		if (gamePlayer == null) {
			return null;
		}
		spectator.put(player, gamePlayer);
		CaviarBR.getInstance().getNameTag().setSpectator(player);
		player.setGameMode(GameMode.SPECTATOR);
		CaviarStrings.ENTER_SPECTATOR_MODE.send(player);
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			if (player.getKiller() != null)
				player.setSpectatorTarget(player.getKiller());
			else if (!getGamers().isEmpty()) {
				Player target = getGamers().stream()
						.sorted((p1, p2) -> (int) (p2.getLocation().distance(player.getLocation()) - p1.getLocation().distance(player.getLocation())))
						.findFirst().orElse(null);
				//Player target = Utils.getRandomFrom(new Random(), getGamers());
				if (target != null) {
					player.setSpectatorTarget(target);
				}
			}
			
		}
		return gamePlayer;
	}
	
	public void setState(GameState state) {
		if (this.state != null) this.state.end();
		this.state = state;
		if (state != null) state.start();
	}
	
	public void shutdown() {
		setState(null);
		CaviarStrings.GAME_SHUTDOWN.broadcast();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			File worldFile = new File("world");
			try (Stream<Path> stream = Files.walk(worldFile.toPath())) {
				AtomicInteger deleted = new AtomicInteger();
				stream
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.filter(file -> !file.getName().equals("caviarbr_datapack.zip") &&!file.getName().equals("datapacks"))
					.filter(file -> !file.equals(worldFile))
					.forEach(file -> {
						file.delete();
						deleted.incrementAndGet();
					});
				System.out.println("World folder has been erased. " + deleted + " files deleted.");
			}catch (IOException e) {
				e.printStackTrace();
			}
		}, "World reset"));
		new ArrayList<>(Bukkit.getOnlinePlayers()).forEach(p -> {
			try {
				p.kick(CaviarStrings.LOGIN_SCREEN_FINISHED.toComponent());
			} catch (Exception e) {}
		});
		Bukkit.shutdown();
	}

	
	public long getTimestampTreasureSpawn() {
		return timestampTreasureSpawn;
	}

	public long getTimestampStart() {
		return timestampStart;
	}

	public long getTimestampNextCompass() {
		return timestampNextCompass;
	}

	public long getTimestampCompassEnd() {
		return timestampCompassEnd;
	}

	@Nullable
	public Cuboid getMapCub() {
		return mapCub;
	}

	public void setMapCub(Cuboid mapCub) {
		this.mapCub = mapCub;
	}
	
}
