package core;

import java.util.Arrays;
import java.util.List;

public class FitnessEvaluator {
	public static final double VM_MIPS = 100.0;

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

			// ET(k,m) = MI(k) / MIPS(m), with VM MIPS fixed at 100.0 (1000 DC MIPS / 10 VMs)
			vmTotalTimes[vmId] += task.getLengthMi() / VM_MIPS;
		}

		for (boolean seen : seenTaskIds) {
			if (!seen) {
				throw new IllegalArgumentException("Every task ID must be assigned exactly once");
			}
		}

		double makespan = 0.0;
		double sumExecutionTime = 0.0;
		for (double totalTime : vmTotalTimes) {
			makespan = Math.max(makespan, totalTime);
			sumExecutionTime += totalTime;
		}

		// Resource Utilization (%) = sum(VM time to life) / (Makespan * total VM number) * 100
		double resourceUtilization = (makespan > 0.0 && vmCount > 0)
				? (sumExecutionTime / (makespan * vmCount)) * 100.0
				: 0.0;

		return new Result(vmTotalTimes, makespan, resourceUtilization);
	}

	public static class Result {
		private final double[] vmTotalTimes;
		private final double makespan;
		private final double resourceUtilization;

		private Result(double[] vmTotalTimes, double makespan, double resourceUtilization) {
			this.vmTotalTimes = vmTotalTimes;
			this.makespan = makespan;
			this.resourceUtilization = resourceUtilization;
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

		public double getResourceUtilization() {
			return resourceUtilization;
		}

		@Override
		public String toString() {
			return String.format("VM totals=%s, makespan=%.4f, resourceUtilization=%.2f%%",
					Arrays.toString(vmTotalTimes), makespan, resourceUtilization);
		}
	}
}
