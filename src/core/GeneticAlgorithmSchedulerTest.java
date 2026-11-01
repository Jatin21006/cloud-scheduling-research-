package core;

import java.util.Arrays;
import java.util.List;

public class GeneticAlgorithmSchedulerTest {
    public static void main(String[] args) {
        testDeterministicExecution();
        testScheduleValidity();
        testInvalidInputs();
        testEmptyTaskList();
        System.out.println("All GeneticAlgorithmScheduler tests: PASS");
    }

    private static void testDeterministicExecution() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(50);
        GeneticAlgorithmScheduler ga = new GeneticAlgorithmScheduler();

        Schedule s1 = ga.solve(tasks, 5, 20, 42L);
        Schedule s2 = ga.solve(tasks, 5, 20, 42L);

        for (int i = 0; i < tasks.size(); i++) {
            if (s1.getVmId(i) != s2.getVmId(i)) {
                throw new AssertionError("GA execution must be deterministic for identical seeds");
            }
        }
        System.out.println("GA deterministic execution: PASS");
    }

    private static void testScheduleValidity() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(100);
        int vmCount = 5;
        GeneticAlgorithmScheduler ga = new GeneticAlgorithmScheduler();

        Schedule schedule = ga.solve(tasks, vmCount, 25, 999L);
        if (schedule.getTaskCount() != 100) {
            throw new AssertionError("Schedule must cover all 100 tasks");
        }

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        FitnessEvaluator.Result result = evaluator.evaluate(tasks, schedule);
        if (result.getMakespan() <= 0.0) {
            throw new AssertionError("Makespan must be strictly positive");
        }

        for (int i = 0; i < tasks.size(); i++) {
            int vm = schedule.getVmId(i);
            if (vm < 0 || vm >= vmCount) {
                throw new AssertionError("Task " + i + " assigned to invalid VM: " + vm);
            }
        }
        System.out.println("GA schedule validity: PASS");
    }

    private static void testInvalidInputs() {
        GeneticAlgorithmScheduler ga = new GeneticAlgorithmScheduler();
        try {
            ga.solve(null, 5, 10, 1L);
            throw new AssertionError("Should reject null tasks");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ga.solve(Arrays.asList(new Task(0, 300)), 0, 10, 1L);
            throw new AssertionError("Should reject non-positive vmCount");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ga.solve(Arrays.asList(new Task(0, 300)), 5, 0, 1L);
            throw new AssertionError("Should reject non-positive iterations");
        } catch (IllegalArgumentException expected) {
        }
        System.out.println("GA input validation: PASS");
    }

    private static void testEmptyTaskList() {
        GeneticAlgorithmScheduler ga = new GeneticAlgorithmScheduler();
        Schedule schedule = ga.solve(Arrays.asList(), 5, 10, 1L);
        if (schedule.getTaskCount() != 0) {
            throw new AssertionError("Empty task list must yield schedule with 0 tasks");
        }
        System.out.println("GA empty task list handling: PASS");
    }
}
