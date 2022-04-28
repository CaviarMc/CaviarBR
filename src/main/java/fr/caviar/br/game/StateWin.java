package fr.caviar.br.game;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import fr.caviar.br.CaviarStrings;
import fr.caviar.br.task.TaskManagerSpigot;

public class StateWin extends GameState {

	private GamePlayer winner;
	private TaskManagerSpigot taskManager;

	private int timer = 0;

	private Random random = new Random();

	public StateWin(GameManager game, GamePlayer winner) {
		super(game);
		this.winner = winner;
		taskManager = new TaskManagerSpigot(game.getPlugin(), this.getClass());
	}

	@Override
	public void start() {
		super.start();

		Bukkit.getOnlinePlayers().forEach(player -> {
			player.playSound(player, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 0.55f);
			player.playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.2f);
			if (winner == null || player.getUniqueId().equals(winner.player.getUuid())) {
				player.playSound(player, Sound.ENTITY_VILLAGER_YES, 0.6f, 1f);
				player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1f);
			}else {
				player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
			}
		});
		CaviarStrings.FORMAT_BARS.broadcast(CaviarStrings.STATE_WIN.format(winner == null ? "x" : winner.player.getName()));

		int endingDuration = game.getSettings().getEndingDuration().get();
		if (endingDuration == 0) {
			game.shutdown();
		}else {
			taskManager.scheduleSyncRepeatingTask(() -> {
				timer++;
				if (timer >= game.getSettings().getEndingDuration().get()) {
					game.shutdown();
				}else {
					Bukkit.getOnlinePlayers().forEach(player -> {
						if (random.nextBoolean()) spawnFirework(player.getLocation());
					});
				}
			}, 1, 1, TimeUnit.SECONDS);
		}
	}

	@Override
	public void end() {
		super.end();
		taskManager.cancelAllTasks();
	}

	private void spawnFirework(Location location) {
		location.add(random.nextInt(30) - 15, 15, random.nextInt(30) - 15);
		location.getWorld().spawn(location, Firework.class, fw -> {
			FireworkMeta meta = fw.getFireworkMeta();
			meta.setPower(0);
			meta.addEffect(FireworkEffect.builder()
					.flicker(random.nextBoolean())
					.withColor(Color.fromRGB(random.nextInt(0xFFFFFF)))
					.with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)])
					.build());
			fw.setFireworkMeta(meta);
		});
	}

	@Override
	public void handleLogin(PlayerLoginEvent event, GamePlayer player) {
		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, CaviarStrings.LOGIN_SCREEN_FINISHED_KICK.toComponent());
	}

	@Override
	public void onJoin(PlayerJoinEvent event, GamePlayer player) {}

	@Override
	public boolean onQuit(PlayerQuitEvent event, GamePlayer player) {
		return false;
	}

}
