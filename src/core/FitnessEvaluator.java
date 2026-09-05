package core;

import java.util.Arrays;
import java.util.List;

public class FitnessEvaluator {
	public static final double VM_MIPS = 1000.0;

	private final int vmCount;

	public FitnessEvaluator(int vmCount) {
		if (vmCount <= 0) {
			throw new IllegalArgumentException("VM count must be positive");
		}
		this.vmCount = vmCount;
	}

	public Result evaluate(List<Task> tasks, Schedule schedule) {
		if (tasks.size() != schedule.getTaskCount()) {
			throw new IllegalArgumentException("Every task must have one schedule entry");
		}

		double[] vmTotalTimes = new double[vmCount];
		boolean[] seenTaskIds = new boolean[tasks.size()];
		for (Task task : tasks) {
			int taskId = task.getId();
			if (taskId < 0 || taskId >= tasks.size() || seenTaskIds[taskId]) {
				throw new IllegalArgumentException("Task IDs must be unique and contiguous");
			}
			seenTaskIds[taskId] = true;

			int vmId = schedule.getVmId(taskId);
			if (vmId < 0 || vmId >= vmCount) {
				throw new IllegalArgumentException("Each task must have a valid VM assignment");
			}

			// ET(k,m) = MI(k) / MIPS(m), with VM MIPS fixed at 1000.
			vmTotalTimes[vmId] += task.getLengthMi() / VM_MIPS;
		}

		for (boolean seen : seenTaskIds) {
			if (!seen) {
				throw new IllegalArgumentException("Every task ID must be assigned exactly once");
			}
		}

		double makespan = 0.0;
		for (double totalTime : vmTotalTimes) {
			makespan = Math.max(makespan, totalTime);
		}
		return new Result(vmTotalTimes, makespan);
	}

	public static class Result {
		private final double[] vmTotalTimes;
		private final double makespan;

		private Result(double[] vmTotalTimes, double makespan) {
			this.vmTotalTimes = vmTotalTimes;
			this.makespan = makespan;
		}

		public double getVmTotalTime(int vmId) {
			if (vmId < 0 || vmId >= vmTotalTimes.length) {
				throw new IllegalArgumentException("VM ID is outside the evaluator");
			}
			return vmTotalTimes[vmId];
		}

		public double getMakespan() {
			return makespan;
		}

		@Override
		public String toString() {
			return "VM totals=" + Arrays.toString(vmTotalTimes) + ", makespan=" + makespan;
		}
	}
}