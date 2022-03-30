package fr.caviar.br.task;

import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.caviar.br.task.NativeTask.TaskLaunch;

public abstract class AUniversalTask<T> implements UniversalTask<T> {
	
	private Map<String, Integer> taskList = new ConcurrentHashMap<>();
	private Map<Integer, T> taskListTask = new ConcurrentHashMap<>();

	/**
	 * Non recommandé, supprime l'entrée de la task, ne l'arrête pas
	 * @param id
	 */
	public Integer removeTaskByName(String taskName) {
		Integer taskId = taskList.remove(taskName);
		if (taskId != null)
			taskListTask.remove(taskId);
		return taskId;
	}

	/**
	 * Non recommandé, supprime l'entrée de la task, ne l'arrête pas
	 * @param id
	 */
	public T removeTaskById(int id) {
		T task = taskListTask.remove(id);
		taskList.entrySet().removeIf(e -> e.getValue() == id);
		return task;
	}

	public void removeTask(T task) {
		Entry<Integer, T> result = taskListTask.entrySet().stream().filter(e -> e.getValue() == task).findFirst().orElse(null);
		taskList.entrySet().removeIf(e -> e.getValue() == result.getKey());
		taskListTask.remove(result.getKey());
	}
	
	public boolean cancelTasksByPrefix(String taskPrefix) {
		Set<Integer> tasks = getTasksMap().entrySet().stream().filter(e -> e.getKey().startsWith(taskPrefix)).map(Entry::getValue).collect(Collectors.toSet());
		if (tasks.isEmpty())
			return false;
		tasks.forEach(this::cancelTask);
		return true;
	}

	public void terminateAllTasks() {
		taskListTask.values().forEach(this::terminateTask);
		taskList.clear();
		taskListTask.clear();
	}
	
	public void cancelAllTasks() {
		taskListTask.values().forEach(this::cancelTask);
		taskList.clear();
		taskListTask.clear();
	}

	public boolean taskExist(String taskName) {
		return taskList.containsKey(taskName);
	}

	public boolean taskExist(int id) {
		return taskList.entrySet().stream().anyMatch(e -> e.getValue() == id);
	}
	
	public String getTaskName(int id) {
		return taskList.entrySet().stream().filter(e -> e.getValue() == id).map(Entry::getKey).findFirst().orElse(null);
	}
	
	public T getTask(int id) {
		return taskListTask.get(id);
	}

	public T getTaskByName(String taskName) {
		Integer taskId = getTaskId(taskName);
		if (taskId == null)
			return null;
		return taskListTask.get(taskId);
	}
	
	public Integer getTaskId(String taskName) {
		return taskList.get(taskName);
	}

	public Map<String, Integer> getTasksMap() {
		return taskList;
	}

	public String getUniqueTaskName(String string) {
		String taskName;
		do
			taskName = string + "_" + new Random().nextInt(99999);
		while (taskExist(taskName));
		return taskName;
	}

	public T getTask(String taskName) {
		Integer taskId = getTaskId(taskName);
		if (taskId != null)
			return getTask(taskId);
		return null;
	}
	
	protected void addTask(String name, int id, T task) {
		if (name != null && !name.isBlank())
			taskList.put(name, id);
		taskListTask.put(id, task);
	}

	protected String getTaskNameFromInt(String taskName) {
		return getTasksMap().entrySet().stream().filter(e -> e.getKey().equals((taskName))).map(Entry::getKey).findFirst().orElse(null);
	}
	
	public T runTaskLater(Runnable runnable, long delay) {
		return runTaskLater(runnable, delay * 50l, TimeUnit.MILLISECONDS);
	}

	public T runTaskLater(String taskName, Runnable runnable, long delay) {
		return runTaskLater(taskName, runnable, delay * 50l, TimeUnit.MILLISECONDS);
	}

	public T scheduleSyncRepeatingTask(Runnable runnable, long delay, long refresh) {
		return scheduleSyncRepeatingTask(runnable, delay * 50l, refresh * 50l,  TimeUnit.MILLISECONDS);
	}
	
}
