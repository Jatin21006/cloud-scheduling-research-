package core;

import java.util.ArrayList;
import java.util.List;

public class ExperimentRunnerTest {
	public static void main(String[] args) {
		ScenarioConfiguration fiftyTaskScenario = PaperScenarios.all().get(0);
		RecordingAlgorithm fiftyTaskAlgorithm = new RecordingAlgorithm();
		List<ExperimentRunner.TrialResult> fiftyTaskResults = new ExperimentRunner(
				fiftyTaskAlgorithm, fiftyTaskScenario, 2).run();
		assertResults(fiftyTaskResults, fiftyTaskScenario, fiftyTaskAlgorithm, 7.5);

		ScenarioConfiguration oneHundredTaskScenario = PaperScenarios.all().get(1);
		RecordingAlgorithm oneHundredTaskAlgorithm = new RecordingAlgorithm();
		List<ExperimentRunner.TrialResult> oneHundredTaskResults = new ExperimentRunner(
				oneHundredTaskAlgorithm, oneHundredTaskScenario, 2).run();
		assertResults(oneHundredTaskResults, oneHundredTaskScenario, oneHundredTaskAlgorithm, 15.0);

		System.out.println("50-task scenario runs 10 trials: PASS");
		System.out.println("100-task scenario runs 10 trials: PASS");
		System.out.println("Scenario, iterations, task counts, seeds, and makespans: PASS");
	}

	private static void assertResults(
			List<ExperimentRunner.TrialResult> results,
			ScenarioConfiguration scenario,
			RecordingAlgorithm algorithm,
			double expectedMakespan) {
		if (results.size() != scenario.getTrials() || algorithm.calls.size() != scenario.getTrials()) {
			throw new AssertionError("Runner did not execute the expected number of trials");
		}

		for (int trialIndex = 0; trialIndex < results.size(); trialIndex++) {
			Call call = algorithm.calls.get(trialIndex);
			ExperimentRunner.TrialResult result = results.get(trialIndex);
			if (call.taskCount != scenario.getTaskCount()
					|| call.iterations != scenario.getIterations()
					|| call.vmCount != 2
					|| call.seed != trialIndex
					|| result.getScenarioId() != scenario.getScenarioId()
					|| result.getTrialNumber() != trialIndex + 1
					|| result.getSeed() != trialIndex
					|| !result.getAlgorithmName().equals("RecordingAlgorithm")
					|| Math.abs(result.getMakespan() - expectedMakespan) > 0.0000001) {
				throw new AssertionError("Runner result does not match the scenario contract");
			}
		}
	}

	private static class RecordingAlgorithm implements SchedulingAlgorithm {
		private final List<Call> calls = new ArrayList<Call>();

		@Override
		public Schedule solve(List<Task> tasks, int vmCount, int iterations, long seed) {
			calls.add(new Call(tasks.size(), vmCount, iterations, seed));
			Schedule schedule = new Schedule(tasks.size());
			for (Task task : tasks) {
				schedule.assign(task.getId(), task.getId() % vmCount);
			}
			return schedule;
		}
	}

	private static class Call {
		private final int taskCount;
		private final int vmCount;
		private final int iterations;
		private final long seed;

		private Call(int taskCount, int vmCount, int iterations, long seed) {
			this.taskCount = taskCount;
			this.vmCount = vmCount;
			this.iterations = iterations;
			this.seed = seed;
		}
	}
}