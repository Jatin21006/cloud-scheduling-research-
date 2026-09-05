package core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorkloadGenerator {
	public static final long NORMAL_TASK_LENGTH_MI = 300;
	public static final long NORMAL_FILE_SIZE_MB = 300;
	public static final long DDOS_TASK_LENGTH_MI = NORMAL_TASK_LENGTH_MI * 100;
	public static final long DDOS_FILE_SIZE_MB = 3000;

	public List<Task> generateNormalTasks(int taskCount) {
		if (taskCount != 50 && taskCount != 100) {
			throw new IllegalArgumentException("Paper scenarios require exactly 50 or 100 tasks");
		}

		List<Task> tasks = new ArrayList<Task>();
		for (int taskId = 0; taskId < taskCount; taskId++) {
			tasks.add(new Task(taskId, NORMAL_TASK_LENGTH_MI));
		}
		return tasks;
	}

	public List<WorkloadTask> generateDdosWorkload(List<Task> baseTasks, Set<Integer> attackTaskIds) {
		if (baseTasks == null || attackTaskIds == null) {
			throw new IllegalArgumentException("Base tasks and attack IDs must not be null");
		}

		Set<Integer> knownTaskIds = new HashSet<Integer>();
		for (Task task : baseTasks) {
			if (!knownTaskIds.add(task.getId())) {
				throw new IllegalArgumentException("Base task IDs must be unique");
			}
		}
		for (Integer attackTaskId : attackTaskIds) {
			if (attackTaskId == null || !knownTaskIds.contains(attackTaskId)) {
				throw new IllegalArgumentException("Every attack ID must identify a base task");
			}
		}

		List<WorkloadTask> workload = new ArrayList<WorkloadTask>();
		for (Task task : baseTasks) {
			boolean attack = attackTaskIds.contains(task.getId());
			workload.add(new WorkloadTask(
					task.getId(),
					attack ? DDOS_TASK_LENGTH_MI : task.getLengthMi(),
					attack ? DDOS_FILE_SIZE_MB : NORMAL_FILE_SIZE_MB,
					attack ? DDOS_FILE_SIZE_MB : NORMAL_FILE_SIZE_MB,
					attack));
		}
		return workload;
	}

	public List<Task> generateFromLengths(List<Long> taskLengthsMi) {
		if (taskLengthsMi == null) {
			throw new IllegalArgumentException("Task lengths must not be null");
		}

		List<Task> tasks = new ArrayList<Task>();
		for (int taskId = 0; taskId < taskLengthsMi.size(); taskId++) {
			Long lengthMi = taskLengthsMi.get(taskId);
			if (lengthMi == null) {
				throw new IllegalArgumentException("Task length must not be null");
			}
			tasks.add(new Task(taskId, lengthMi));
		}
		return tasks;
	}
}