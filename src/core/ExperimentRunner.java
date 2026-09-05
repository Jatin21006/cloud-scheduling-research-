package core;

import java.util.ArrayList;
import java.util.List;

public class ExperimentRunner {
	private static final long BASE_SEED = 0L;

	private final SchedulingAlgorithm algorithm;
	private final ScenarioConfiguration scenario;
	private final int vmCount;

	public ExperimentRunner(SchedulingAlgorithm algorithm, ScenarioConfiguration scenario, int vmCount) {
		if (algorithm == null || scenario == null) {
			throw new IllegalArgumentException("Algorithm and scenario must not be null");
		}
		if (vmCount <= 0) {
			throw new IllegalArgumentException("VM count must be positive");
		}
		this.algorithm = algorithm;
		this.scenario = scenario;
		this.vmCount = vmCount;
	}

	public List<TrialResult> run() {
		List<Task> tasks = new WorkloadGenerator().generateNormalTasks(scenario.getTaskCount());
		FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
		List<TrialResult> results = new ArrayList<TrialResult>();

		for (int trialIndex = 0; trialIndex < scenario.getTrials(); trialIndex++) {
			long seed = BASE_SEED + trialIndex;
			Schedule schedule = algorithm.solve(tasks, vmCount, scenario.getIterations(), seed);
			FitnessEvaluator.Result fitness = evaluator.evaluate(tasks, schedule);
			results.add(new TrialResult(
					scenario.getScenarioId(),
					trialIndex + 1,
					algorithm.getClass().getSimpleName(),
					seed,
					fitness.getMakespan()));
		}

		return results;
	}

	public static class TrialResult {
		private final int scenarioId;
		private final int trialNumber;
		private final String algorithmName;
		private final long seed;
		private final double makespan;

		private TrialResult(int scenarioId, int trialNumber, String algorithmName, long seed, double makespan) {
			this.scenarioId = scenarioId;
			this.trialNumber = trialNumber;
			this.algorithmName = algorithmName;
			this.seed = seed;
			this.makespan = makespan;
		}

		public int getScenarioId() {
			return scenarioId;
		}

		public int getTrialNumber() {
			return trialNumber;
		}

		public String getAlgorithmName() {
			return algorithmName;
		}

		public long getSeed() {
			return seed;
		}

		public double getMakespan() {
			return makespan;
		}
	}
}