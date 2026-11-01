package core;

import java.util.Arrays;
import java.util.List;

public class ArtificialBeeColonySchedulerTest {
    public static void main(String[] args) {
        testDeterministicExecution();
        testScheduleValidity();
        testInvalidInputs();
        testEmptyTaskList();
        System.out.println("All ArtificialBeeColonyScheduler tests: PASS");
    }

    private static void testDeterministicExecution() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(50);
        ArtificialBeeColonyScheduler abc = new ArtificialBeeColonyScheduler();

        Schedule s1 = abc.solve(tasks, 5, 20, 42L);
        Schedule s2 = abc.solve(tasks, 5, 20, 42L);

        for (int i = 0; i < tasks.size(); i++) {
            if (s1.getVmId(i) != s2.getVmId(i)) {
                throw new AssertionError("ABC execution must be deterministic for identical seeds");
            }
        }
        System.out.println("ABC deterministic execution: PASS");
    }

    private static void testScheduleValidity() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(100);
        int vmCount = 5;
        ArtificialBeeColonyScheduler abc = new ArtificialBeeColonyScheduler();

        Schedule schedule = abc.solve(tasks, vmCount, 25, 999L);
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
        System.out.println("ABC schedule validity: PASS");
    }

    private static void testInvalidInputs() {
        ArtificialBeeColonyScheduler abc = new ArtificialBeeColonyScheduler();
        try {
            abc.solve(null, 5, 10, 1L);
            throw new AssertionError("Should reject null tasks");
        } catch (IllegalArgumentException expected) {
        }

        try {
            abc.solve(Arrays.asList(new Task(0, 300)), 0, 10, 1L);
            throw new AssertionError("Should reject non-positive vmCount");
        } catch (IllegalArgumentException expected) {
        }

        try {
            abc.solve(Arrays.asList(new Task(0, 300)), 5, 0, 1L);
            throw new AssertionError("Should reject non-positive iterations");
        } catch (IllegalArgumentException expected) {
        }
        System.out.println("ABC input validation: PASS");
    }

    private static void testEmptyTaskList() {
        ArtificialBeeColonyScheduler abc = new ArtificialBeeColonyScheduler();
        Schedule schedule = abc.solve(Arrays.asList(), 5, 10, 1L);
        if (schedule.getTaskCount() != 0) {
            throw new AssertionError("Empty task list must yield schedule with 0 tasks");
        }
        System.out.println("ABC empty task list handling: PASS");
    }
}
