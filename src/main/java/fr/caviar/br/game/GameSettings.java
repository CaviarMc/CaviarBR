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
	private final GameSettingInt maxPlayers = new GameSettingInt(10, "maxPlayers", 1);
	
	private final GameSettingInt waitingTimeLong = new GameSettingInt(7, "waitingTimeLong", 0);
	private final GameSettingInt waitingTimeShort = new GameSettingInt(10, "waitingTimeShort", 0);
	
	private final GameSettingInt playersRadius = new GameSettingInt(450, "playersRadius", 10);
	
	private final GameManager game;
	
	private List<GameSetting> settings;
	
	public GameSettings(@NotNull GameManager game) {
		this.game = game;
		this.settings = Arrays.asList(minPlayers, maxPlayers, waitingTimeLong, waitingTimeShort, playersRadius);
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
