package core;

import java.util.List;
import java.util.Collections;

public class WorkloadGeneratorTest {
	public static void main(String[] args) {
		WorkloadGenerator generator = new WorkloadGenerator();
		List<Task> fiftyTasks = generator.generateNormalTasks(50);
		List<Task> oneHundredTasks = generator.generateNormalTasks(100);
		assertTaskCountAndLength(fiftyTasks, 50);
		assertTaskCountAndLength(oneHundredTasks, 100);

		List<WorkloadTask> ddosWorkload = generator.generateDdosWorkload(
				fiftyTasks, Collections.singleton(7));
		WorkloadTask attackTask = ddosWorkload.get(7);
		if (attackTask.getLengthMi() != 30000
				|| attackTask.getInputSizeMb() != 3000
				|| attackTask.getOutputSizeMb() != 3000
				|| !attackTask.isDdosAttack()) {
			throw new AssertionError("DDoS task transformation is incorrect");
		}

		List<Task> repeatedWorkload = generator.generateNormalTasks(50);
		for (int taskId = 0; taskId < repeatedWorkload.size(); taskId++) {
			if (repeatedWorkload.get(taskId).getLengthMi() != fiftyTasks.get(taskId).getLengthMi()) {
				throw new AssertionError("Repeated configuration is not deterministic");
			}
		}

		int[][] expectedScenarios = {
				{1, 10, 50, 10},
				{2, 10, 100, 10},
				{3, 50, 50, 10},
				{4, 50, 100, 10},
				{5, 100, 50, 10},
				{6, 100, 100, 10}};
		List<ScenarioConfiguration> scenarios = PaperScenarios.all();
		if (scenarios.size() != expectedScenarios.length) {
			throw new AssertionError("Expected six paper scenarios");
		}
		for (int index = 0; index < expectedScenarios.length; index++) {
			ScenarioConfiguration scenario = scenarios.get(index);
			int[] expected = expectedScenarios[index];
			if (scenario.getScenarioId() != expected[0]
					|| scenario.getIterations() != expected[1]
					|| scenario.getTaskCount() != expected[2]
					|| scenario.getTrials() != expected[3]) {
				throw new AssertionError("Scenario " + (index + 1) + " does not match Table 3");
			}
		}

		System.out.println("Normal 50-task workload: PASS");
		System.out.println("Normal 100-task workload: PASS");
		System.out.println("All normal tasks are 300 MI: PASS");
		System.out.println("Explicit DDoS task is 30000 MI and 3000 MB input/output: PASS");
		System.out.println("Repeated configuration is deterministic: PASS");
		System.out.println("Six Table 3 scenarios match exactly: PASS");
	}

	private static void assertTaskCountAndLength(List<Task> tasks, int expectedCount) {
		if (tasks.size() != expectedCount) {
			throw new AssertionError("Expected " + expectedCount + " tasks");
		}
		for (Task task : tasks) {
			if (task.getLengthMi() != 300) {
				throw new AssertionError("Normal task length is not 300 MI");
			}
		}
	}
}