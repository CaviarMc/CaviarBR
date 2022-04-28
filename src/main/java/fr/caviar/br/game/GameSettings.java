package fr.caviar.br.game;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import fr.caviar.br.utils.observable.AbstractObservable;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class GameSettings {

	private final GameSettingInt minPlayers = new GameSettingInt(2, "minPlayers", 1, Material.WATER_BUCKET);
	private final GameSettingInt maxPlayers = new GameSettingInt(100, "maxPlayers", 1, Material.LAVA_BUCKET);

	private final GameSettingInt waitingTimeLong = new GameSettingInt(60, "waitingTimeLong", 0, Material.GOLD_INGOT);
	private final GameSettingInt waitingTimeShort = new GameSettingInt(10, "waitingTimeShort", 0, Material.IRON_INGOT);

	private final GameSettingInt playersRadius = new GameSettingInt(2000, "playersRadius", 1, Material.STICK);

	private final GameSettingInt mapSize = new GameSettingInt(2500, "mapSize", 1, Material.DIRT);
	private final GameSettingInt treasureRaduis = new GameSettingInt(1000, "treasureRaduis", 1, Material.TRAPPED_CHEST);
	private final GameSettingMinute waitTreasure = new GameSettingMinute(5, "waitTreasure", Material.HONEYCOMB);
	private final GameSettingMinute waitCompass = new GameSettingMinute(3, "waitCompass", Material.PINK_DYE);
	private final GameSettingSecond compassDuration = new GameSettingSecond(30, "compassDuration", Material.BLUE_DYE);
	private final GameSettingSecond countdownStart = new GameSettingSecond(60, "countdownStart", Material.NETHER_STAR);
	private final GameSettingMinute maxTimeGame = new GameSettingMinute(10, "maxTimeGame", Material.STONE_SWORD);
	private final GameSettingInt finalSize = new GameSettingInt(200, "finalSize", 1, Material.COBBLESTONE);
	private final GameSettingInt minPlayerToWin = new GameSettingInt(3, "minPlayerToWin", 1, Material.PLAYER_HEAD);
	private final GameSettingInt endingDuration = new GameSettingInt(30, "endingDuration", 1, Material.REDSTONE_TORCH);
	private final GameSettingBoolean allowSpectator = new GameSettingBoolean(true, "allowSpectator", Material.MINECART);
	private final GameSettingBoolean chunkGenerateAsync = new GameSettingBoolean(true, "chunkGenerateAsync", Material.COMPARATOR);
	private final GameSettingBoolean debug = new GameSettingBoolean(true, "debug", Material.COMMAND_BLOCK);

	private final GameManager game;

	private List<GameSetting<?>> settings;

	public GameSettings(GameManager game) {
		this.game = game;
		this.settings = Arrays.asList(minPlayers, maxPlayers, waitingTimeLong, waitingTimeShort, playersRadius, mapSize, treasureRaduis, endingDuration, waitCompass,
				compassDuration, waitTreasure, countdownStart, chunkGenerateAsync, maxTimeGame, debug, finalSize, allowSpectator, minPlayerToWin);
		maxPlayers.observe("update_bukkit", () -> Bukkit.setMaxPlayers(maxPlayers.get()));
		Bukkit.setMaxPlayers(maxPlayers.get());
		treasureRaduis.observe("treasure_spawn", () -> game.calculateTreasureSpawnPoint(null));
	}

	public GameManager getGame() {
		return game;
	}

	public List<GameSetting<?>> getSettings() {
		return settings;
	}

	public GameSettingInt getMinPlayers() {
		return minPlayers;
	}

	public GameSettingInt getMaxPlayers() {
		return maxPlayers;
	}

	public GameSettingInt getWaitingTimeLong() {
		return waitingTimeLong;
	}

	public GameSettingInt getWaitingTimeShort() {
		return waitingTimeShort;
	}

	public GameSettingInt getPlayersRadius() {
		return playersRadius;
	}

	public GameSettingInt getEndingDuration() {
		return endingDuration;
	}

	public GameSettingMinute getWaitCompass() {
		return waitCompass;
	}

	public GameSettingSecond getCompassDuration() {
		return compassDuration;
	}

	public GameSettingMinute getWaitTreasure() {
		return waitTreasure;
	}

	public GameSettingInt getMapSize() {
		return mapSize;
	}

	public GameSettingSecond getCountdownStart() {
		return countdownStart;
	}

	public GameSettingInt getTreasureRaduis() {
		return treasureRaduis;
	}

	public GameSettingBoolean isChunkGenerateAsync() {
		return chunkGenerateAsync;
	}

	public GameSettingMinute getMaxTimeGame() {
		return maxTimeGame;
	}

	public GameSettingBoolean isDebug() {
		return debug;
	}

	public GameSettingBoolean isAllowedSpectator() {
		return allowSpectator;
	}

	public GameSettingInt getFinalSize() {
		return finalSize;
	}

	public GameSettingInt getMinPlayerToWin() {
		return minPlayerToWin;
	}

	public abstract class GameSetting<T> extends AbstractObservable {

		protected T value;
		protected String key;
		protected Material material;
		protected String description;
		
		protected GameSetting(T value, String key, Material material) {
			this.value = value;
			this.key = key;
			this.material = material;
		}

		public void set(T value) {
			this.value = value;
			update();
		}

		public T get() {
			return value;
		}

		public abstract boolean smallIncrement();
		public abstract boolean bigIncrement();
		public abstract boolean smallDecrement();
		public abstract boolean bigDecrement();
		
		public String getKey() {
			return key;
		}
			
		public String getDescription() {
			return description;
		}

		public Material getMaterial() {
			return material;
		}

		public abstract Argument[] getArguments();

		public abstract T getValueFromArguments(Object[] args);


	}

	public class GameSettingInt extends GameSetting<Integer> {

		private int min, max;

		public GameSettingInt(int value, String key, int min, Material material) {
			this(value, key, min, Integer.MAX_VALUE, material);
		}

		public GameSettingInt(int value, String key, int min, int max, Material material) {
			super(value, key, material);

			this.min = min;
			this.max = max;
		}

		@Override
		public Argument[] getArguments() {
			return new Argument[] { new IntegerArgument(getKey(), min, max) };
		}

		@Override
		public Integer getValueFromArguments(Object[] args) {
			return (Integer) args[0];
		}

		public int getMin() {
			return min;
		}

		public int getMax() {
			return max;
		}

		@Override
		public boolean smallIncrement() {
			if (value == max)
				return false;
			++value;
			return true;
		}

		@Override
		public boolean bigIncrement() {
			if (value == max)
				return false;
			value += 10;
			return true;
		}

		@Override
		public boolean smallDecrement() {
			if (value == min)
				return false;
			--value;
			return true;
		}

		@Override
		public boolean bigDecrement() {
			if (value == min)
				return false;
			value -= 10;
			return true;
		}
	}

	public class GameSettingSecond extends GameSettingInt {

		private int min, max;

		public GameSettingSecond(int value, String key, Material material) {
			super(value, key, 1,  Integer.MAX_VALUE, material);
		}

		public int getInSecond() {
			return super.get();
		}

		public int getInMinute() {
			return Math.round(super.get() / 60f);
		}

		@Override
		public Argument[] getArguments() {
			return new Argument[] { new IntegerArgument(getKey() + "inSeconds", min, max) };
		}

		@Override
		public Integer getValueFromArguments(Object[] args) {
			return (Integer) args[0];
		}
	}

	public class GameSettingMinute extends GameSettingInt {

		private int min, max;

		public GameSettingMinute(int value, String key, Material material) {
			super(value, key, 1,  Integer.MAX_VALUE, material);
		}

		@Override
		public Argument[] getArguments() {
			return new Argument[] { new IntegerArgument(getKey() + "inMinutes", min, max) };
		}

		public int getInSecond() {
			return super.get() * 60;
		}

		public int getInMinute() {
			return super.get();
		}

		@Override
		public Integer getValueFromArguments(Object[] args) {
			return (Integer) args[0];
		}
	}

	public class GameSettingBoolean extends GameSetting<Boolean> {

		public GameSettingBoolean(boolean value, String key, Material material) {
			super(value, key, material);
		}

		@Override
		public Argument[] getArguments() {
			return new Argument[] { new BooleanArgument(getKey()) };
		}

		@Override
		public Boolean getValueFromArguments(Object[] args) {
			return (boolean) args[0];
		}

		@Override
		public boolean smallIncrement() {
			value = !value;
			return true;
		}

		@Override
		public boolean bigIncrement() {
			return smallIncrement();
		}

		@Override
		public boolean smallDecrement() {
			return smallIncrement();
		}

		@Override
		public boolean bigDecrement() {
			return smallIncrement();
		}
	}
}
