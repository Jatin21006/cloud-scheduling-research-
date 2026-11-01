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

		// Verify generateWorkload and toTasks
		List<WorkloadTask> normalGen = generator.generateWorkload(WorkloadGenerator.WorkloadType.NORMAL, 50, 123L);
		if (normalGen.size() != 50) {
			throw new AssertionError("Expected 50 normal workload tasks");
		}
		for (WorkloadTask wt : normalGen) {
			if (wt.isDdosAttack() || wt.getLengthMi() != 300) {
				throw new AssertionError("Normal workload task should not be an attack");
			}
		}

		List<WorkloadTask> ddosGen = generator.generateWorkload(WorkloadGenerator.WorkloadType.DDOS, 50, 123L);
		if (ddosGen.size() != 50) {
			throw new AssertionError("Expected 50 DDoS workload tasks");
		}
		int attackCount = 0;
		for (WorkloadTask wt : ddosGen) {
			if (wt.isDdosAttack()) {
				attackCount++;
				if (wt.getLengthMi() != 30000 || wt.getInputSizeMb() != 3000) {
					throw new AssertionError("DDoS attack task parameters incorrect");
				}
			}
		}
		if (attackCount != 5) {
			throw new AssertionError("Expected exactly 5 attack tasks (10% of 50), found: " + attackCount);
		}

		List<Task> convertedTasks = generator.toTasks(ddosGen);
		if (convertedTasks.size() != 50) {
			throw new AssertionError("Converted tasks size mismatch");
		}

		System.out.println("Normal 50-task workload: PASS");
		System.out.println("Normal 100-task workload: PASS");
		System.out.println("All normal tasks are 300 MI: PASS");
		System.out.println("Explicit DDoS task is 30000 MI and 3000 MB input/output: PASS");
		System.out.println("Repeated configuration is deterministic: PASS");
		System.out.println("Six Table 3 scenarios match exactly: PASS");
		System.out.println("WorkloadType enum and generateWorkload: PASS");
		System.out.println("WorkloadTask to Task conversion: PASS");
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
