package fr.caviar.br.game;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
	protected long timestampSec;
	protected long timeNextCompass;
	protected long timeCompassDuration;
	
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
			worldLoader.stop();
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
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		Bukkit.shutdown();
	}
	
}
