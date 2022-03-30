package fr.caviar.br.task;

import java.util.concurrent.TimeUnit;

import fr.caviar.br.task.NativeTask.TaskLaunch;

public interface UniversalTask<T> {

	boolean taskExist(String taskName);

	boolean taskExist(int id);

	/**
	 * Non recommandé, supprime l'entrée de la task, ne l'arrête pas
	 * @param id
	 */
	Integer removeTaskByName(String taskName);

	/**
	 * Non recommandé, supprime l'entrée de la task, ne l'arrête pas
	 * @param id
	 */
	T removeTaskById(int id);

	T getTask(int id);

	T getTask(String taskName);

	String getTaskName(int id);

	Integer getTaskId(String taskName);

	String getUniqueTaskName(String string);

	boolean terminateTask(T task);
	
	boolean terminateTask(int id);
	
	void terminateAllTasks();
	
	boolean cancelTask(T task);

	boolean cancelTask(int id);

	boolean cancelTask(String taskName);

	boolean cancelTasksByPrefix(String taskPrefix);

	void cancelAllTasks();

	T runTask(Runnable runnable);

	T runTaskAsynchronously(Runnable runnable);

	T runTaskAsynchronously(String taskName, Runnable runnable);

	T runTaskAsynchronously(String taskName, Runnable runnable, long delay, TimeUnit timeUnit);

	T runTaskLater(Runnable runnable, long tick);

	T runTaskLater(String taskName, Runnable runnable, long tick);

	T runTaskLater(Runnable runnable, long delay, TimeUnit timeUnit);

	T runTaskLater(String taskName, Runnable runnable, long delay, TimeUnit timeUnit);

	T scheduleSyncRepeatingTask(Runnable runnable, long delay, long refresh);

	T scheduleSyncRepeatingTask(Runnable runnable, long delay, long refresh, TimeUnit timeUnit);

	T scheduleSyncRepeatingTask(String taskName, Runnable runnable, long delay, long refresh, TimeUnit timeUnit);



}