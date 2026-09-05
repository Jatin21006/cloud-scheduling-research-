package core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cloudsim.CloudSimScheduleAdapter;
import org.cloudbus.cloudsim.Cloudlet;

public class EndToEndIntegrationTest {
	private static final double TOLERANCE = 0.0000001;

	public static void main(String[] args) throws Exception {
		List<Task> tasks = createTasks();
		Schedule schedule = createSchedule();
		FitnessEvaluator.Result fitness = new FitnessEvaluator(2).evaluate(tasks, schedule);
		pass("FitnessEvaluator accepts the deterministic schedule");

		ScenarioConfiguration scenario = new ScenarioConfiguration(1, 10, 50, 10);
		List<ExperimentRunner.TrialResult> runnerResults = new ExperimentRunner(
				new DummyAlgorithm(), scenario, 2).run();
		check(runnerResults.size() == 10, "ExperimentRunner produced 10 results");
		pass("ExperimentRunner produced 10 results");

		Path csv = Files.createTempDirectory("end-to-end-").resolve("results.csv");
		ExperimentResultWriter.write(csv, runnerResults);
		List<String> csvLines = Files.readAllLines(csv);
		check(csvLines.size() == 11 && csvLines.get(0).equals("algorithm,scenario,trial,seed,makespan"),
				"ExperimentResultWriter wrote the expected CSV");
		pass("ExperimentResultWriter wrote the expected CSV");

		CloudSimScheduleAdapter.Result cloudSim = new CloudSimScheduleAdapter().execute(tasks, schedule, 2);
		check(cloudSim.getExecutions().size() == tasks.size(), "CloudSim executed all five Cloudlets");
		for (CloudSimScheduleAdapter.Execution execution : cloudSim.getExecutions()) {
			check(execution.getStatus() == Cloudlet.SUCCESS, "CloudSim Cloudlet completed successfully");
		}
		pass("CloudSim executed all five Cloudlets successfully");

		double mathematicalMakespan = fitness.getMakespan();
		double cloudSimMakespan = cloudSim.getObservedMakespan();
		check(Math.abs(mathematicalMakespan - cloudSimMakespan) <= TOLERANCE,
				"Mathematical and CloudSim makespans agree within tolerance");
		System.out.println("Mathematical makespan: " + mathematicalMakespan);
		System.out.println("CloudSim observed makespan: " + cloudSimMakespan);
		pass("Complete pipeline passed");
	}

	private static List<Task> createTasks() {
		List<Task> tasks = new ArrayList<Task>();
		for (int taskId = 0; taskId < 5; taskId++) {
			tasks.add(new Task(taskId, 300));
		}
		return tasks;
	}

	private static Schedule createSchedule() {
		Schedule schedule = new Schedule(5);
		schedule.assign(0, 0);
		schedule.assign(1, 0);
		schedule.assign(2, 1);
		schedule.assign(3, 1);
		schedule.assign(4, 1);
		return schedule;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			System.out.println("FAIL: " + message);
			throw new AssertionError(message);
		}
	}

	private static void pass(String message) {
		System.out.println("PASS: " + message);
	}

	private static class DummyAlgorithm implements SchedulingAlgorithm {
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