package fr.caviar.br.game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import fr.caviar.br.CaviarBR;
import fr.caviar.br.CaviarStrings;
import fr.caviar.br.game.commands.GameAdminCommand;
import fr.caviar.br.game.commands.SettingsCommand;

public class GameManager {
	
	private final CaviarBR plugin;
	private final GameSettings settings;
	private final Map<UUID, GamePlayer> players = new HashMap<>();
	private final Map<Player, GamePlayer> spectator = new HashMap<>();
	private final WorldLoader worldLoader;
	protected long timestampStart;
	protected long timestampTreasureSpawn;
	protected long timestampNextCompass;
	protected long timestampCompassEnd;
//	private Location treasure = null;
//	private List<Location> spawnPoints = null;

	private GameState state;
	
	private World world;
	
	public GameManager(CaviarBR plugin) {
		this.plugin = plugin;
		settings = new GameSettings(this);
		worldLoader = new WorldLoader(plugin);
	}
	
	public void enable() {
		world = Bukkit.getWorlds().get(0);
		world.setSpawnLocation(world.getHighestBlockAt(0, 0).getLocation().add(0.5, 5, 0.5));
		setState(new StateWait(this));
		new SettingsCommand(this);
		new GameAdminCommand(this);
		worldLoader.addGameManager(this);
		worldLoader.start(false);
	}
	
	public void disable() {
		if (state != null) {
			state.end();
			state = null;
		}
		if (worldLoader != null)
			worldLoader.stop(true);
	}
	
	/*public void calculateSpawnPoint() {
		int treasureRaduis = settings.getTreasureRaduis().get();
		int playerRaduis = settings.getPlayersRadius().get();
		Random random = new Random();
		int treasureX = random.nextInt(-treasureRaduis, treasureRaduis);
		int treasureZ = random.nextInt(-treasureRaduis, treasureRaduis);
		plugin.getLogger().info("Trying to find treasure.");
		prepareLocation(treasureX, treasureZ, tloc -> {
			
			plugin.getLogger().info("Found treasure at " + tloc.toString());
			treasure = tloc.add(0, 1, 0);
//			maxDistance = treasure;
			
//			this.getAllPlayers().forEach(this::setPreparing);
			
			int online = this.getPlayers().size();
			int i = 0;
			for (GamePlayer player : this.getPlayers().values()) {
				double theta = i++ * 2 * Math.PI / online;
				//Player bukkitPlayer = ((Player) player.player.getPlayer());
				player.spawnLocation = null;
				player.started = false;
				

				int i2 = i;
				int playerX = (int) (treasure.getX() + playerRaduis * Math.cos(theta));
				int playerZ = (int) (treasure.getZ() + playerRaduis * Math.sin(theta));
				prepareLocation(playerX, playerZ, ploc -> {
//					if (!isRunning()) return;

					this.getPlugin().getLogger().info("Found spawnpoint n°" + i2 + " in " + ploc.toString() + " for " + player.player.getName());
					
					ploc.getChunk().addPluginChunkTicket(plugin);
					player.setSpawnLocation(ploc);
//					addSpawnPoint(ploc);
					
//					if (game.getPlayers().values().stream().noneMatch(x -> x.spawnLocation == null)) {
//						foundSpawnpoints = true;
//						game.getAllPlayers().forEach(this::setPreparing);
//						int timer = 10;
//						taskManager.runTaskLater("prep.before_end", this::startCoutdown, timer, TimeUnit.SECONDS);
//					}
				}, new AtomicInteger(1), 1);
			}
		}, new AtomicInteger(1), 1);
	}

	
	private void prepareLocation(int x, int z, Consumer<Location> consumer, AtomicInteger operations, int chunks) {
		/*int chunkX = x >> 4;
		int chunkZ = z >> 4;
		game.getWorld().getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
			int y = chunk.getWorld().getHighestBlockYAt(x, z);
			Block block = chunk.getBlock(x - (chunkX << 4), y, z - (chunkZ << 4));
//			List<Block> listBlocks = new ArrayList<>(9);
//			for (int tempX = -1; tempX <= 1; ++tempX) {
//				for (int tempZ = -1; tempZ <= 1; ++tempZ) {
//					listBlocks.add(block.getLocation().add(tempX, 0, tempZ).getBlock());
//				}
//			}
//			if (!listBlocks.stream().allMatch(this::isGoodBlock)) {
			if (!isGoodBlock(block)) {
				tryChunk(chunk, consumer, false, operations, chunks);
			}else {
				game.getPlugin().getLogger().info("Success in " + operations + " operations, in " + chunks + " chunks.");
				consumer.accept(new Location(game.getWorld(), x, y, z));
			}
		}).exceptionally(throwable -> {
			throwable.printStackTrace();
			consumer.accept(new Location(game.getWorld(), 0.5, 80, 0.5));
			return null;
		});
	}*/

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
	
	public @NotNull Collection<? extends Player> getAllPlayers() {
		return plugin.getServer().getOnlinePlayers();
	}
	
	public Map<Player, GamePlayer> getSpigotPlayers() {
		return players.entrySet().stream().collect(Collectors.toMap(entry -> Bukkit.getPlayer(entry.getKey()), entry -> entry.getValue()));
	}
	
	public Map<Player, GamePlayer> getSpectator() {
		return spectator;
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
}
