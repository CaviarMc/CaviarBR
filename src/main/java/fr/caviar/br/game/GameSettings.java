package fr.caviar.br.game;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;

import fr.caviar.br.utils.observable.AbstractObservable;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class GameSettings {

	private final GameSettingInt minPlayers = new GameSettingInt(2, "minPlayers", 1);
	private final GameSettingInt maxPlayers = new GameSettingInt(100, "maxPlayers", 1);

	private final GameSettingInt waitingTimeLong = new GameSettingInt(60, "waitingTimeLong", 0);
	private final GameSettingInt waitingTimeShort = new GameSettingInt(10, "waitingTimeShort", 0);

	private final GameSettingInt playersRadius = new GameSettingInt(2000, "playersRadius", 1);

	private final GameSettingInt mapSize = new GameSettingInt(2500, "mapSize", 1);
	private final GameSettingInt treasureRaduis = new GameSettingInt(1000, "treasureRaduis", 1);
	private final GameSettingInt endingDuration = new GameSettingInt(30, "endingDuration", 1);
	private final GameSettingMinute waitCompass = new GameSettingMinute(3, "waitCompass");
	private final GameSettingSecond compassDuration = new GameSettingSecond(30, "compassDuration");
	private final GameSettingMinute waitTreasure = new GameSettingMinute(5, "waitTreasure");
	private final GameSettingSecond countdownStart = new GameSettingSecond(60, "countdownStart");
	private final GameSettingBoolean chunkGenerateAsync = new GameSettingBoolean(true, "chunkGenerateAsync");
	private final GameSettingBoolean debug = new GameSettingBoolean(true, "debug");
	private final GameSettingBoolean allowSpectator = new GameSettingBoolean(true, "allowSpectator");
	private final GameSettingMinute maxTimeGame = new GameSettingMinute(10, "maxTimeGame");
	private final GameSettingInt finalSize = new GameSettingInt(200, "finalSize", 1);
	private final GameSettingInt minPlayerToWin = new GameSettingInt(3, "minPlayerToWin", 1);

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

		private T value;
		private String key;

		protected GameSetting(T value, String key) {
			this.value = value;
			this.key = key;
		}

		public void set(T value) {
			this.value = value;
			update();
		}

		public T get() {
			return value;
		}

		public String getKey() {
			return key;
		}

		public abstract Argument[] getArguments();

		public abstract T getValueFromArguments(Object[] args);

	}

	public class GameSettingInt extends GameSetting<Integer> {

		private int min, max;

		public GameSettingInt(int value, String key, int min) {
			this(value, key, min, Integer.MAX_VALUE);
		}

		public GameSettingInt(int value, String key, int min, int max) {
			super(value, key);

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
	}

	public class GameSettingSecond extends GameSettingInt {

		private int min, max;

		public GameSettingSecond(int value, String key) {
			super(value, key, 1,  Integer.MAX_VALUE);
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

		public GameSettingMinute(int value, String key) {
			super(value, key, 1,  Integer.MAX_VALUE);
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

		public GameSettingBoolean(boolean value, String key) {
			super(value, key);
		}

		@Override
		public Argument[] getArguments() {
			return new Argument[] { new BooleanArgument(getKey()) };
		}

		@Override
		public Boolean getValueFromArguments(Object[] args) {
			return (boolean) args[0];
		}
	}
}
