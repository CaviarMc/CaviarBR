package fr.caviar.br.task;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import fr.caviar.br.CaviarBR;

public class NativeTask extends AUniversalTask<NativeTask.TaskLaunch> {

	private static final NativeTask INSTANCE = new NativeTask();

	public static NativeTask getInstance() {
		return INSTANCE;
	}

	private int taskId = 1;

	@Override
	public void cancelAllTasks() {
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
	public boolean cancelTask(TaskLaunch task) {
		boolean bool = false;
		if (task != null)
			bool = task.cancel(false);
		return bool;
	}

	public boolean terminateTaskByName(String taskName) {
		return terminateTask(getTaskByName(taskName));
	}

	@Override
	public boolean terminateTask(int id) {
		return terminateTask(getTask(id));
	}
	
	@Override
	public boolean terminateTask(TaskLaunch task) {
		boolean bool = false;
		if (task != null) {
			bool = task.cancel(true);
		}
		return bool;
	}

	public void runTaskNewThread(Runnable runnable) {
		Executors.newSingleThreadScheduledExecutor().schedule(() -> runnable.run(), 0, TimeUnit.SECONDS);
	}

	@Override
	public TaskLaunch runTask(Runnable runnable) {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
					removeTaskById(taskId);
				}
			}
		};
		TaskLaunch taskLaunch = addTask(null, new TaskLaunch(task, taskId++));
		timer.schedule(task, 0);
		return taskLaunch;
	}

	@Override
	public TaskLaunch runTaskLater(Runnable runnable, long delay, TimeUnit timeUnit) {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
					removeTaskById(taskId);
				}
			}
		};
		TaskLaunch taskLaunch = addTask(null, new TaskLaunch(task, taskId++));
		timer.schedule(task, timeUnit.toMillis(delay));
		return taskLaunch;
	}

	@Override
	public TaskLaunch runTaskLater(String taskName, Runnable runnable, long delay, TimeUnit timeUnit) {
		cancelTask(taskName);
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
					removeTaskByName(taskName);
				}
			}
		};
		TaskLaunch taskLaunch = addTask(taskName, new TaskLaunch(task, taskId++));
		timer.schedule(task, timeUnit.toMillis(delay));
		return taskLaunch;
	}

	@Override
	public TaskLaunch runTaskAsynchronously(Runnable runnable) {
		return runTaskAsynchronously(UUID.randomUUID().toString(), runnable);
	}

	@Override
	public TaskLaunch runTaskAsynchronously(String taskName, Runnable runnable) {
		cancelTask(taskName);
		return addTask(null, new TaskLaunch(CompletableFuture.runAsync(() -> {
			try {
				runnable.run();
			} finally {
				removeTaskById(taskId);
			}
		}, Executors.newSingleThreadScheduledExecutor()), taskId++));
	}

	@Override
	public TaskLaunch runTaskAsynchronously(String taskName, Runnable runnable, long delay, TimeUnit timeUnit) {
		cancelTask(taskName);
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					runTaskAsynchronously(taskName + ".subAsync", runnable);
				} finally {
					removeTaskByName(taskName);
				}
			}
		};
		TaskLaunch taskLaunch = addTask(taskName, new TaskLaunch(task, taskId++));
		timer.schedule(task, timeUnit.toMillis(delay));
		return taskLaunch;
	}

	@Override
	public TaskLaunch scheduleSyncRepeatingTask(Runnable runnable, long delay, long refresh, TimeUnit timeUnit) {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
					removeTaskById(taskId);
				}
			}
		};
		TaskLaunch tasklaunch = addTask(null, new TaskLaunch(task, taskId++));
		timer.schedule(task, timeUnit.toMillis(delay), timeUnit.toMillis(refresh));
		return tasklaunch;
	}

	@Override
	public TaskLaunch scheduleSyncRepeatingTask(String taskName, Runnable runnable, long delay, long refresh, TimeUnit timeUnit) {
		return this.scheduleSyncRepeatingTask(taskName, runnable, timeUnit.toMillis(delay), timeUnit.toMillis(refresh));
	}

	@Override
	public TaskLaunch scheduleSyncRepeatingTask(String taskName, Runnable runnable, long delay, long refresh) {
		cancelTask(taskName);
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
					removeTaskByName(taskName);
				}
			}
		};
		TaskLaunch tasklaunch = addTask(taskName, new TaskLaunch(task, taskId++));
		timer.schedule(task, delay, refresh);
		return tasklaunch;
	}
	
	static class TaskLaunch {
		CompletableFuture<?> completableFuture;
		TimerTask timerTask;
		int id;

		public TaskLaunch(CompletableFuture<?> completableFuture, int id) {
			this.completableFuture = completableFuture;
			this.id = id;
		}

		public TaskLaunch(TimerTask timerTask, int id) {
			this.timerTask = timerTask;
			this.id = id;
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			if (completableFuture != null)
				return completableFuture.cancel(mayInterruptIfRunning);
			else if (timerTask != null) {
				CaviarBR.getInstance().getLogger().log(Level.WARNING, String.format("Can't terminate sync task n°%d in %s. Just cancel it", id, this.getClass().getSimpleName()));
				return timerTask.cancel();
				
			}
			return false;

		}
		
	}

	protected TaskLaunch addTask(String name, TaskLaunch task) {
		addTask(name, task.id, task);
		return task;
	}
}
