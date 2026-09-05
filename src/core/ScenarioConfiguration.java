package core;

public class ScenarioConfiguration {
	private final int scenarioId;
	private final int iterations;
	private final int taskCount;
	private final int trials;

	public ScenarioConfiguration(int scenarioId, int iterations, int taskCount, int trials) {
		if (scenarioId <= 0 || iterations <= 0 || trials <= 0) {
			throw new IllegalArgumentException("Scenario values must be positive");
		}
		if (taskCount != 50 && taskCount != 100) {
			throw new IllegalArgumentException("Paper scenarios require exactly 50 or 100 tasks");
		}
		this.scenarioId = scenarioId;
		this.iterations = iterations;
		this.taskCount = taskCount;
		this.trials = trials;
	}

	public int getScenarioId() {
		return scenarioId;
	}

	public int getIterations() {
		return iterations;
	}

	public int getTaskCount() {
		return taskCount;
	}

	public int getTrials() {
		return trials;
	}
}