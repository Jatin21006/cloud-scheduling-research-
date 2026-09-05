package core;

import java.util.Arrays;
import java.util.List;

public class SchedulingAlgorithmTest {
	public static void main(String[] args) {
		List<Task> tasks = Arrays.asList(new Task(0, 300), new Task(1, 300));
		SchedulingAlgorithm algorithm = new DummyAlgorithm();
		Schedule schedule = algorithm.solve(tasks, 2, 10, 12345L);

		if (schedule.getTaskCount() != tasks.size()
				|| schedule.getVmId(0) != 0
				|| schedule.getVmId(1) != 0) {
			throw new AssertionError("Dummy algorithm did not return the expected Schedule");
		}

		System.out.println("SchedulingAlgorithm dummy implementation: PASS");
	}

	private static class DummyAlgorithm implements SchedulingAlgorithm {
		@Override
		public Schedule solve(List<Task> tasks, int vmCount, int iterations, long seed) {
			Schedule schedule = new Schedule(tasks.size());
			for (Task task : tasks) {
				schedule.assign(task.getId(), 0);
			}
			return schedule;
		}
	}
}