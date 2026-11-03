package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WorkloadGenerator {
	public static final long NORMAL_TASK_LENGTH_MI = 300;
	public static final long NORMAL_FILE_SIZE_MB = 300;
	public static final long DDOS_TASK_LENGTH_MI = NORMAL_TASK_LENGTH_MI * 100; // 30,000 MI
	public static final long DDOS_FILE_SIZE_MB = 3000; // 3,000 MB
	public static final double DEFAULT_DDOS_ATTACK_RATIO = 1.0; // Volumetric flood: 100% cloudlets scaled

	public static final long UNIFORM_MIN_LENGTH_MI = 100;
	public static final long UNIFORM_MAX_LENGTH_MI = 300;

	public enum WorkloadType {
		NORMAL,
		DDOS
	}

	public enum Distribution {
		CONSTANT_300,
		UNIFORM_100_300
	}

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

	/**
	 * Generates normal tasks following the uniform distribution U[100, 300] MI
	 * as specified in Section 3.3, Line 829 of the research paper.
	 */
	public List<Task> generateUniformNormalTasks(int taskCount, long seed) {
		if (taskCount != 50 && taskCount != 100) {
			throw new IllegalArgumentException("Paper scenarios require exactly 50 or 100 tasks");
		}

		Random rng = new Random(seed);
		List<Task> tasks = new ArrayList<Task>();
		for (int taskId = 0; taskId < taskCount; taskId++) {
			long lengthMi = UNIFORM_MIN_LENGTH_MI + rng.nextInt((int) (UNIFORM_MAX_LENGTH_MI - UNIFORM_MIN_LENGTH_MI + 1));
			tasks.add(new Task(taskId, lengthMi));
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
			long taskLen = attack ? (task.getLengthMi() * 100L) : task.getLengthMi();
			workload.add(new WorkloadTask(
					task.getId(),
					taskLen,
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

	/**
	 * Generates a full WorkloadTask list for the given workload type, task count, and seed.
	 * Default preserves backward compatibility with constant 300 MI tasks.
	 */
	public List<WorkloadTask> generateWorkload(WorkloadType type, int taskCount, long seed) {
		return generateWorkload(type, taskCount, seed, DEFAULT_DDOS_ATTACK_RATIO, Distribution.CONSTANT_300);
	}

	/**
	 * Overloaded generator allowing a specific attack ratio if needed.
	 */
	public List<WorkloadTask> generateWorkload(WorkloadType type, int taskCount, long seed, double attackRatio) {
		return generateWorkload(type, taskCount, seed, attackRatio, Distribution.CONSTANT_300);
	}

	/**
	 * Generates uniform workload U[100, 300] MI faithful to Section 3.3, Line 829.
	 */
	public List<WorkloadTask> generateUniformWorkload(WorkloadType type, int taskCount, long seed) {
		return generateWorkload(type, taskCount, seed, DEFAULT_DDOS_ATTACK_RATIO, Distribution.UNIFORM_100_300);
	}

	/**
	 * Comprehensive workload generator supporting both constant 300 MI and uniform distributions.
	 */
	public List<WorkloadTask> generateWorkload(WorkloadType type, int taskCount, long seed, double attackRatio, Distribution distribution) {
		if (type == null) {
			throw new IllegalArgumentException("Workload type must not be null");
		}
		List<Task> base = (distribution == Distribution.UNIFORM_100_300)
				? generateUniformNormalTasks(taskCount, seed)
				: generateNormalTasks(taskCount);

		if (type == WorkloadType.NORMAL) {
			List<WorkloadTask> normalWorkload = new ArrayList<WorkloadTask>();
			for (Task t : base) {
				normalWorkload.add(new WorkloadTask(
						t.getId(),
						t.getLengthMi(),
						NORMAL_FILE_SIZE_MB,
						NORMAL_FILE_SIZE_MB,
						false));
			}
			return normalWorkload;
		} else {
			int attackCount = (int) Math.round(taskCount * attackRatio);
			if (attackCount >= taskCount) {
				Set<Integer> allAttackIds = new HashSet<Integer>();
				for (int i = 0; i < taskCount; i++) {
					allAttackIds.add(i);
				}
				return generateDdosWorkload(base, allAttackIds);
			}

			Random rng = new Random(seed);
			List<Integer> allIds = new ArrayList<Integer>();
			for (int i = 0; i < taskCount; i++) {
				allIds.add(i);
			}
			for (int i = taskCount - 1; i > 0; i--) {
				int j = rng.nextInt(i + 1);
				int temp = allIds.get(i);
				allIds.set(i, allIds.get(j));
				allIds.set(j, temp);
			}
			Set<Integer> attackIds = new HashSet<Integer>(allIds.subList(0, attackCount));
			return generateDdosWorkload(base, attackIds);
		}
	}

	/**
	 * Converts WorkloadTask objects to standard Task instances for scheduling algorithms.
	 */
	public List<Task> toTasks(List<WorkloadTask> workloadTasks) {
		if (workloadTasks == null) {
			throw new IllegalArgumentException("Workload tasks must not be null");
		}
		List<Task> tasks = new ArrayList<Task>();
		for (WorkloadTask wt : workloadTasks) {
			tasks.add(new Task(wt.getId(), wt.getLengthMi()));
		}
		return tasks;
	}
}
