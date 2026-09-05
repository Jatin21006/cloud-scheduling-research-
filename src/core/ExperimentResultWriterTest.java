package core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExperimentResultWriterTest {
	public static void main(String[] args) throws Exception {
		ScenarioConfiguration scenario = new ScenarioConfiguration(1, 10, 50, 3);
		List<ExperimentRunner.TrialResult> results = new ExperimentRunner(
				new CsvTestAlgorithm(), scenario, 2).run();
		Path output = Files.createTempDirectory("experiment-results-").resolve("nested/results.csv");

		ExperimentResultWriter.write(output, results);
		List<String> lines = Files.readAllLines(output);
		if (!lines.get(0).equals("algorithm,scenario,trial,seed,makespan")) {
			throw new AssertionError("CSV header is incorrect");
		}
		if (lines.size() != 4) {
			throw new AssertionError("CSV data row count is incorrect");
		}
		for (int index = 0; index < results.size(); index++) {
			ExperimentRunner.TrialResult result = results.get(index);
			String expected = result.getAlgorithmName() + "," + result.getScenarioId()
					+ "," + result.getTrialNumber() + "," + result.getSeed()
					+ "," + result.getMakespan();
			if (!lines.get(index + 1).equals(expected)) {
				throw new AssertionError("CSV values were not preserved");
			}
		}

		System.out.println("CSV header: PASS");
		System.out.println("CSV row count: PASS");
		System.out.println("CSV result values preserved: PASS");
		System.out.println("Generated CSV: " + output);
		for (String line : lines) {
			System.out.println(line);
		}
	}

	private static class CsvTestAlgorithm implements SchedulingAlgorithm {
		@Override
		public Schedule solve(List<Task> tasks, int vmCount, int iterations, long seed) {
			Schedule schedule = new Schedule(tasks.size());
			for (Task task : tasks) {
				schedule.assign(task.getId(), task.getId() % vmCount);
			}
			return schedule;
		}
	}
}