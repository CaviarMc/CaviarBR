package fr.caviar.br.game;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.caviar.br.utils.observablel.Observable.Observer;

public class StateWait extends GameState implements Runnable {
	
	private static final String OBSERVER_KEY = "wait";
	
	private Lock lock = new ReentrantLock();
	private int left = -1;
	private int task = -1;
	
	public StateWait(GameManager game) {
		super(game);
	}
	
	@Override
	public void start() {
		super.start();
		task = game.getPlugin().getTaskManager().scheduleSyncRepeatingTask(this, 0L, 1L, TimeUnit.SECONDS);
		
		Observer observer = () -> updatePlayers(Bukkit.getOnlinePlayers().size());
		game.getSettings().getMinPlayers().observe(OBSERVER_KEY, observer);
		game.getSettings().getMaxPlayers().observe(OBSERVER_KEY, observer);
		game.getSettings().getWaitingTimeLong().observe(OBSERVER_KEY, observer);
		game.getSettings().getWaitingTimeShort().observe(OBSERVER_KEY, observer);
	}
	
	@Override
	public void end() {
		super.end();
		if (task != -1) game.getPlugin().getTaskManager().cancelTaskById(task);
		
		game.getSettings().getMinPlayers().unobserve(OBSERVER_KEY);
		game.getSettings().getMaxPlayers().unobserve(OBSERVER_KEY);
		game.getSettings().getWaitingTimeLong().unobserve(OBSERVER_KEY);
		game.getSettings().getWaitingTimeShort().unobserve(OBSERVER_KEY);
	}
	
	@Override
	public void run() {
		lock.lock();
		
		if (left != -1) {
			
			if (--left == 0) {
				game.setState(new StatePreparing(game));
			}else {
				if (left == 50 || left == 30 || left == 15 || left == 10 || left <= 5) {
					Bukkit.broadcastMessage("§aGame is launching in " + left + " seconds.");
				}
			}
		}
		
		lock.unlock();
	}
	
	@Override
	public boolean areNewPlayersAllowed() {
		return true;
	}
	
	protected void updatePlayers(int online) {
		lock.lock();
		int min = game.getSettings().getMinPlayers().get();
		if (online < min) {
			if (left != -1) {
				left = -1;
				Bukkit.broadcastMessage("§cNot enough players to start the game.");
			}
		}else {
			int max = game.getSettings().getMaxPlayers().get();
			int waitingTime = (online == max ? game.getSettings().getWaitingTimeShort() : game.getSettings().getWaitingTimeLong()).get();
			if (left == -1 || left > waitingTime) left = waitingTime;
		}
		lock.unlock();
	}
	
	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {
		updatePlayers(Bukkit.getOnlinePlayers().size());
		
		// TODO messages
	}
	
	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
		updatePlayers(Bukkit.getOnlinePlayers().size() - 1);
		
		return true;
	}
	
}
