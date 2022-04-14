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
	
	private final GameSettingInt waitingTimeLong = new GameSettingInt(7, "waitingTimeLong", 0);
	private final GameSettingInt waitingTimeShort = new GameSettingInt(10, "waitingTimeShort", 0);
	
	private final GameSettingInt playersRadius = new GameSettingInt(450, "playersRadius", 10);
	
	private final GameSettingInt mapSize = new GameSettingInt(5000, "mapSize", 10);
	private final GameSettingInt treasureRaduis = new GameSettingInt(300, "treasureRaduis", 10);
	private final GameSettingInt endingDuration = new GameSettingInt(30, "endingDuration", 1);
	private final GameSettingMinute waitCompass = new GameSettingMinute(3, "waitCompass");
	private final GameSettingSecond compassDuration = new GameSettingSecond(10, "compassDuration");
	private final GameSettingMinute waitTreasure = new GameSettingMinute(1, "waitTreasure");
	private final GameSettingSecond countdownStart = new GameSettingSecond(60, "countdownStart");
	private final GameSettingBoolean chunkGenerateAsync = new GameSettingBoolean(true, "chunkGenerateAsync");
	
	private final GameManager game;
	
	private List<GameSetting<?>> settings;
	
	public GameSettings(GameManager game) {
		this.game = game;
		this.settings = Arrays.asList(minPlayers, maxPlayers, waitingTimeLong, waitingTimeShort, playersRadius, endingDuration, mapSize, waitCompass, compassDuration, waitTreasure);
		maxPlayers.observe("update_bukkit", () -> Bukkit.setMaxPlayers(maxPlayers.get()));
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

	public GameSettingBoolean getChunkGenerateAsync() {
		return chunkGenerateAsync;
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
	}
	
	public class GameSettingSecond extends GameSetting<Integer> {
		
		private int min, max;
		
		public GameSettingSecond(int value, String key) {
			super(value, key);
			this.min = 1;
			this.max = Integer.MAX_VALUE;
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
	
	public class GameSettingMinute extends GameSetting<Integer> {
		
		private int min, max;
		
		public GameSettingMinute(int value, String key) {
			super(value, key);
			this.min = 1;
			this.max = Integer.MAX_VALUE;
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
