package core;

import java.util.Arrays;
import java.util.List;

public class FitnessEvaluatorTest {
	public static void main(String[] args) {
		List<Task> tasks = Arrays.asList(
				new Task(0, 300),
				new Task(1, 300),
				new Task(2, 300),
				new Task(3, 300),
				new Task(4, 300));

		Schedule schedule = new Schedule(tasks.size());
		schedule.assign(0, 0);
		schedule.assign(1, 0);
		schedule.assign(2, 1);
		schedule.assign(3, 1);
		schedule.assign(4, 1);

		FitnessEvaluator.Result result = new FitnessEvaluator(2).evaluate(tasks, schedule);
		System.out.printf("VM 0 total time: %.1f seconds%n", result.getVmTotalTime(0));
		System.out.printf("VM 1 total time: %.1f seconds%n", result.getVmTotalTime(1));
		System.out.printf("Makespan: %.1f seconds%n", result.getMakespan());
	}
}