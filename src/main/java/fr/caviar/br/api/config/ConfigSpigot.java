package fr.caviar.br.api.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import com.google.common.io.ByteStreams;

import fr.caviar.br.api.CaviarPlugin;
import fr.caviar.br.player.CaviarPlayerSpigot;
import fr.caviar.br.utils.ColorUtils;
import fr.caviar.br.utils.Utils;

public class ConfigSpigot extends YamlConfiguration {

	private static List<ConfigSpigot> configs = new ArrayList<>();

	public static List<ConfigSpigot> getConfigs() {
		return configs;
	}

	public static ConfigSpigot getConfig(String name) {
		return configs.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(configs.stream().filter(c -> c.fileName.equals(name)).findFirst().orElse(null));
	}

	public static void unloadAll() {
		for (Iterator<ConfigSpigot> it = configs.iterator(); it.hasNext();) {
			it.next().unload();
		}
	}

	{
//		ConfigurationSerialization.registerClass(Cuboid.class);
//		ConfigurationSerialization.registerClass(Cuboid.class, "fr.olympa.api.region.shapes.Cuboid");
//		ConfigurationSerialization.registerClass(ExpandedCuboid.class);
//		ConfigurationSerialization.registerClass(ChunkCuboid.class);
//		ConfigurationSerialization.registerClass(Polygon.class);
//		ConfigurationSerialization.registerClass(Polygon.class, "fr.olympa.api.region.shapes.Polygon");
//		ConfigurationSerialization.registerClass(ChunkPolygon.class);
//		ConfigurationSerialization.registerClass(Cylinder.class);
//		ConfigurationSerialization.registerClass(WorldRegion.class);
//
//		ConfigurationSerialization.registerClass(FixedLine.class);
//		ConfigurationSerialization.registerClass(CyclingLine.class);
	}

	private File configFile;

	private boolean isLoaded = false;
	private boolean saveOnUnload = false;
	private String fileName;
	private CaviarPlugin plugin;
	private Map<String, Consumer<ConfigSpigot>> loadTasks = new HashMap<>();
	//private Map<String, Consumer<ConfigSpigot>> unloadTasks = new HashMap<>();

	public ConfigSpigot() {
		super();
	}

//	public ConfigSpigot(Plugin plugin, String filename) {
//		this((CaviarPlugin) plugin, filename);
//	}

	public ConfigSpigot(CaviarPlugin plugin, String filename, boolean saveOnUnload) {
		this.plugin = plugin;
		this.saveOnUnload = saveOnUnload;
		if (!filename.toLowerCase().endsWith(".yml"))
			filename += ".yml";
		fileName = filename;
		configFile = new File(plugin.getDataFolder(), fileName);
		configs.add(this);
	}

	public boolean removeLoadTask(String name) {
		return loadTasks.remove(name) != null;
	}

	public boolean addLoadTask(String name, Consumer<ConfigSpigot> consumer) {
		if (isLoaded)
			consumer.accept(this);
		boolean isTaskDidntExist = loadTasks.put(name, consumer) == null;
		if (!isTaskDidntExist)
			new IllegalAccessError("Config load task " + name + " already exist. It was replaced but this won't be happened").printStackTrace();
		return isTaskDidntExist;
	}

	/*public boolean removeUnLoadTask(String name) {
		return loadTasks.remove(name) != null;
	}

	public boolean addUnLoadTask(String name, Consumer<ConfigSpigot> consumer) {
		if (isLoaded)
			consumer.accept(this);
		boolean isTaskDidntExist = unloadTasks.put(name, consumer) == null;
		if (!isTaskDidntExist)
			new IllegalAccessError("Config unload task " + name + " already exist. It was replaced but this won't be happened").printStackTrace();
		return isTaskDidntExist;
	}*/

	public boolean eraseFile() {
		try {
			Files.delete(configFile.toPath());
			return configFile.createNewFile();
		} catch (IOException e) {
			System.err.printf("Error while erasing file %s in %s (is file loaded %s, plugin %s)%n", fileName, configFile.getPath(), isLoaded, plugin.getName());
			e.printStackTrace();
		}
		return false;
	}

	public File getFile() {
		return configFile;
	}

	public String getFileName() {
		return fileName;
	}

	public CaviarPlugin getPlugin() {
		return plugin;
	}

	public InputStream getRessource() {
		return plugin.getResource(fileName);
	}

	public boolean hasResource() {
		return getRessource() != null;
	}

	@Override
	public String getName() {
		return plugin.getDescription().getName() + "/" + fileName;
	}

	public void reload() throws IOException, InvalidConfigurationException {
		saveOnUnload = false;
		loadUnSafe();
//		plugin.getTaskManager().runTaskAsynchronously(() -> plugin.getServer().getPluginManager().callEvent(new SpigotConfigReloadEvent(this)));
	}

	public void loadUnSafe() throws IOException, InvalidConfigurationException {
		if (isLoaded)
			unload();
		File folder = configFile.getParentFile();
		if (!folder.exists())
			folder.mkdirs();
		InputStream resource = getRessource();
		if (!configFile.exists() || Utils.isEmptyFile(configFile)) {
			configFile.createNewFile();
			if (resource != null)
				ByteStreams.copy(resource, new FileOutputStream(configFile));
			this.load(configFile);
		} else {
			this.load(configFile);
			ConfigSpigot resourceConfig = new ConfigSpigot();
			Double resourceConfigVersion = null;

			if (resource != null) {
				resourceConfig.load(new InputStreamReader(resource));
				resourceConfigVersion = resourceConfig.getVersion();

				Double version = getVersion();
				if (resourceConfigVersion != null && version != null)
					if (resourceConfigVersion > version) {
						configFile.renameTo(new File(folder, configFile.getName() + " V" + version));
						configFile = new File(folder, fileName);
						configFile.createNewFile();
						ByteStreams.copy(getRessource(), new FileOutputStream(configFile));
						this.load(configFile);
						Bukkit.getLogger().log(Level.INFO, ChatColor.GREEN + "Config updated: " + fileName);
					}
			}
		}
		isLoaded = true;
		loadTasks.values().forEach(task -> task.accept(this));
	}

	public void load() {
		try {
			loadUnSafe();
		} catch (IOException | InvalidConfigurationException e) {
			plugin.sendMessage("§cImpossible de charger la configuration %s", fileName);
			System.err.printf("Error while loading file %s in %s (is file loaded %s, plugin %s)%n", fileName, configFile.getPath(), isLoaded, plugin.getName());
			e.printStackTrace();
		}
	}

	public void saveUnSafe() throws IOException {
		this.save(configFile);
	}

	public void unload() {
		if (saveOnUnload)
			save();
		//unloadTasks.values().forEach(task -> task.accept(this));
		isLoaded = false;
		//configs.remove(this); // -> Need to be added but this trigger an ConcurrentModificationException at line 45
	}

	public void save() {
		try {
			saveUnSafe();
		} catch (IOException e) {
			e.initCause(new IllegalAccessError("Unable to save config: " + fileName));
			System.err.printf("Error while saving file %s in %s (is file loaded %s, plugin %s)%n", fileName, configFile.getPath(), isLoaded, plugin.getName());
			e.printStackTrace();
		}
	}

	public Double getVersion() {
		Object obj = this.get("version");
		if (obj instanceof Number)
			return ((Number) obj).doubleValue();
		else if (obj != null)
			try {
				return Double.valueOf(obj.toString());
			} catch (NumberFormatException e) {
				return null;
			}
		return null;
	}

	public void saveIfNotExists() {
		if (!configFile.exists())
			plugin.saveResource(fileName, true);
	}

//	public void set(String path, Location location) {
//		this.set(path, UtilsSpigot.convertLocationToString(location));
//	}

	public void setPlayer(CaviarPlayerSpigot uPlayer) {
		this.set("player." + uPlayer.getUuid() + ".name", uPlayer.getName());
		this.set("player." + uPlayer.getUuid() + ".group", uPlayer.getGroup());
	}

	@Override
	public Location getLocation(String path) {
//		return UtilsSpigot.convertStringToLocation(this.getString(path));
		return null;
	}

	public Material getMaterial(String path) {
		return Material.valueOf(this.getString(path));
	}

	@Override
	public String getString(String path) {
		return ColorUtils.color(super.getString(path));
	}

	@Nullable
	public CaviarPlayerSpigot getPlayer(UUID uuid) {
		CaviarPlayerSpigot uPlayer;
		if (this.get("player." + uuid) == null)
			return null;
		String name = this.getString("player." + uuid + ".name");
		String group = this.getString("player." + uuid + ".group");
	
		uPlayer = new CaviarPlayerSpigot(name, uuid);
		uPlayer.setGroup(group);
		return uPlayer;
	}

	public void set(String path, Material material) {
		this.set(path, material.name());
	}
}
