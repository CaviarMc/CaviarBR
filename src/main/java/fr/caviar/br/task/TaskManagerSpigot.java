package fr.caviar.br.task;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.caviar.br.CaviarBR;

public class TaskManagerSpigot extends AUniversalTask<BukkitTask> {

	protected Plugin plugin;
	private Class<?> clazz = null;

	public TaskManagerSpigot(Plugin plugin, Class<?> clazz) {
		this.plugin = plugin;
		this.clazz = clazz;
	}

	public TaskManagerSpigot(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean terminateTask(String taskName) {
		cancelTask(taskName);
		CaviarBR.getInstance().getLogger().log(Level.WARNING, String.format("Can't terminate sync task %s in %s. Just cancel it", taskName, this.getClass().getSimpleName()));
		return false;
	}
	@Override
	public boolean terminateTask(BukkitTask task) {
		cancelTask(task);
		CaviarBR.getInstance().getLogger().log(Level.WARNING, String.format("Can't terminate sync task n°%d in %s. Just cancel it", task.getTaskId(), this.getClass().getSimpleName()));
		return false;
	}

	@Override
	public boolean terminateTask(int id) {
		cancelTask(id);
		CaviarBR.getInstance().getLogger().log(Level.WARNING, String.format("Can't terminate sync task n°%d in %s. Just cancel it", id, this.getClass().getSimpleName()));
		return false;
	}

	@Override
	public void cancelAllTasks() {
		if (clazz == null) {
			getScheduler().cancelTasks(plugin);
		}
		super.cancelAllTasks();
	}

	@Override
	public boolean cancelTask(String taskName) {
		return cancelTask(getTaskByName(taskName));
	}

	@Override
	public boolean cancelTask(int id) {
		return cancelTask(getTask(id));
	}

	@Override
	public boolean cancelTask(BukkitTask task) {
		if (task != null) {
			task.cancel();
			return true;
		}
		return false;
	}

	@Override
	public BukkitTask runTask(Runnable runnable) {
		return getScheduler().runTask(plugin, runnable);
	}

	@Override
	public BukkitTask runTaskAsynchronously(Runnable runnable) {
		return getScheduler().runTaskAsynchronously(plugin, runnable);
	}

	@Override
	public BukkitTask runTaskAsynchronously(String taskName, Runnable runnable) {
		cancelTask(taskName);
		BukkitTask bukkitTask = getScheduler().runTaskAsynchronously(plugin, runnable);
		addTask(taskName, bukkitTask);
		return bukkitTask;
	}

	@Override
	public BukkitTask runTaskAsynchronously(String taskName, Runnable runnable, long delay, TimeUnit timeUnit) {
		return runTaskLater(taskName, () -> {
			runTaskAsynchronously(taskName + ".subAsync", runnable);
		}, delay, timeUnit);
	}

	@Override
	public BukkitTask runTaskLater(Runnable runnable, long delay, TimeUnit timeUnit) {
		return runTaskLater(runnable, timeUnit.toMillis(delay) / 50l);
	}

	@Override
	public BukkitTask runTaskLater(Runnable runnable, long delay) {
		return runTaskLater(UUID.randomUUID().toString(), runnable, delay);
	}

	@Override
	public BukkitTask runTaskLater(String taskName, Runnable runnable, long delay, TimeUnit timeUnit) {
		return runTaskLater(taskName, runnable, timeUnit.toMillis(delay) / 50l);
	}

	@Override
	public BukkitTask runTaskLater(String taskName, Runnable runnable, long delay) {
		cancelTask(taskName);
		BukkitTask task = getScheduler().runTaskLater(plugin, () -> {
			try {
				runnable.run();
			} finally {
				removeTaskByName(taskName);
			}
		}, delay);
		addTask(taskName, task);
		return task;
	}

	@Override
	public BukkitTask scheduleSyncRepeatingTask(Runnable runnable, long delay, long refresh, TimeUnit timeUnit) {
		return this.scheduleSyncRepeatingTask(runnable, timeUnit.toMillis(delay) / 50l, timeUnit.toMillis(refresh) / 50l);
	}

	@Override
	public BukkitTask scheduleSyncRepeatingTask(Runnable runnable, long delay, long refresh) {
		return this.scheduleSyncRepeatingTask(UUID.randomUUID().toString(), runnable, delay, refresh);
	}

	@Override
	public BukkitTask scheduleSyncRepeatingTask(String taskName, Runnable runnable, long delay, long refresh, TimeUnit timeUnit) {
		return this.scheduleSyncRepeatingTask(taskName, runnable, timeUnit.toMillis(delay) / 50l, timeUnit.toMillis(refresh) / 50l);
	}

	@Override
	public BukkitTask scheduleSyncRepeatingTask(String taskName, Runnable runnable, long delay, long refresh) {
		int taskId = getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
			try {
				runnable.run();
			} catch (Exception e) {
				cancelTask(taskName);
				e.printStackTrace();
			}
		}, delay, refresh);
		@Nullable
		BukkitTask task = getScheduler().getPendingTasks().stream().filter(t -> t.getTaskId() == taskId).findFirst().orElse(null);
		addTask(taskName, task);
		return task;
	}

	protected void addTask(String name, BukkitTask task) {
		addTask(name, task.getTaskId(), task);
	}

	private BukkitScheduler getScheduler() {
		return plugin.getServer().getScheduler();
	}
}
