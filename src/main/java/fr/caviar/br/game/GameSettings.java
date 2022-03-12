package fr.caviar.br.game;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import fr.caviar.br.utils.observable.ObservableInt;

public class GameSettings {
	
	private final ObservableInt minPlayers = new ObservableInt(2);
	private final ObservableInt maxPlayers = new ObservableInt(10);
	
	private final ObservableInt waitingTimeLong = new ObservableInt(60);
	private final ObservableInt waitingTimeShort = new ObservableInt(10);
	
	private final ObservableInt playersRadius = new ObservableInt(200);
	
	private final GameManager game;
	
	public GameSettings(@NotNull GameManager game) {
		this.game = game;
		maxPlayers.observe("update_bukkit", () -> Bukkit.setMaxPlayers(maxPlayers.get()));
	}
	
	public GameManager getGame() {
		return game;
	}
	
	public ObservableInt getMinPlayers() {
		return minPlayers;
	}
	
	public ObservableInt getMaxPlayers() {
		return maxPlayers;
	}
	
	public ObservableInt getWaitingTimeLong() {
		return waitingTimeLong;
	}
	
	public ObservableInt getWaitingTimeShort() {
		return waitingTimeShort;
	}
	
	public ObservableInt getPlayersRadius() {
		return playersRadius;
	}
	
}
