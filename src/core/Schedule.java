package core;

import java.util.Arrays;

public class Schedule {
	private final int[] vmByTask;

	public Schedule(int taskCount) {
		if (taskCount < 0) {
			throw new IllegalArgumentException("Task count must be non-negative");
		}
		vmByTask = new int[taskCount];
		Arrays.fill(vmByTask, -1);
	}

	public void assign(int taskId, int vmId) {
		if (taskId < 0 || taskId >= vmByTask.length) {
			throw new IllegalArgumentException("Task ID is outside the schedule");
		}
		vmByTask[taskId] = vmId;
	}

	public int getVmId(int taskId) {
		if (taskId < 0 || taskId >= vmByTask.length) {
			throw new IllegalArgumentException("Task ID is outside the schedule");
		}
		return vmByTask[taskId];
	}

	public int getTaskCount() {
		return vmByTask.length;
	}
}