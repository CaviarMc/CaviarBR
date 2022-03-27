package fr.caviar.br.game;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import fr.caviar.br.utils.observable.AbstractObservable;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;

public class GameSettings {
	
	private final GameSettingInt minPlayers = new GameSettingInt(2, "minPlayers", 1);
	private final GameSettingInt maxPlayers = new GameSettingInt(100, "maxPlayers", 1);
	
	private final GameSettingInt waitingTimeLong = new GameSettingInt(7, "waitingTimeLong", 0);
	private final GameSettingInt waitingTimeShort = new GameSettingInt(10, "waitingTimeShort", 0);
	
	private final GameSettingInt playersRadius = new GameSettingInt(450, "playersRadius", 10);
	
	private final GameSettingInt mapSize = new GameSettingInt(20, "mapSize", 50);
	
	private final GameSettingInt endingDuration = new GameSettingInt(30, "endingDuration", 0);
	
	private final GameSettingInt waitCompass = new GameSettingInt(10, "waitCompass", 0);
	
	private final GameSettingInt compassDuration = new GameSettingInt(10, "compassDuration", 0);
	
	private final GameSettingInt waitTreasure = new GameSettingInt(20, "waitTreasure", 0);
	
	private final GameManager game;
	
	private List<GameSetting> settings;
	
	public GameSettings(@NotNull GameManager game) {
		this.game = game;
		this.settings = Arrays.asList(minPlayers, maxPlayers, waitingTimeLong, waitingTimeShort, playersRadius, endingDuration, mapSize, waitCompass, compassDuration, waitTreasure);
		maxPlayers.observe("update_bukkit", () -> Bukkit.setMaxPlayers(maxPlayers.get()));
	}
	
	public GameManager getGame() {
		return game;
	}
	
	public List<GameSetting> getSettings() {
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
	
	public GameSettingInt getWaitCompass() {
		return waitCompass;
	}

	public GameSettingInt getCompassDuration() {
		return compassDuration;
	}

	public GameSettingInt getWaitTreasure() {
		return waitTreasure;
	}

	public GameSettingInt getMapSize() {
		return mapSize;
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
	
}
