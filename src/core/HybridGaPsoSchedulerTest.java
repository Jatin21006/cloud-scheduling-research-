package core;

import java.util.Arrays;
import java.util.List;

public class HybridGaPsoSchedulerTest {
    public static void main(String[] args) {
        testDeterministicExecution();
        testScheduleValidity();
        testInvalidInputs();
        testEmptyTaskList();
        System.out.println("All HybridGaPsoScheduler tests: PASS");
    }

    private static void testDeterministicExecution() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(50);
        HybridGaPsoScheduler hybrid = new HybridGaPsoScheduler();

        Schedule s1 = hybrid.solve(tasks, 5, 20, 42L);
        Schedule s2 = hybrid.solve(tasks, 5, 20, 42L);

        for (int i = 0; i < tasks.size(); i++) {
            if (s1.getVmId(i) != s2.getVmId(i)) {
                throw new AssertionError("Hybrid GA-PSO execution must be deterministic for identical seeds");
            }
        }
        System.out.println("Hybrid GA-PSO deterministic execution: PASS");
    }

    private static void testScheduleValidity() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(100);
        int vmCount = 5;
        HybridGaPsoScheduler hybrid = new HybridGaPsoScheduler();

        Schedule schedule = hybrid.solve(tasks, vmCount, 25, 999L);
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
        System.out.println("Hybrid GA-PSO schedule validity: PASS");
    }

    private static void testInvalidInputs() {
        HybridGaPsoScheduler hybrid = new HybridGaPsoScheduler();
        try {
            hybrid.solve(null, 5, 10, 1L);
            throw new AssertionError("Should reject null tasks");
        } catch (IllegalArgumentException expected) {
        }

        try {
            hybrid.solve(Arrays.asList(new Task(0, 300)), 0, 10, 1L);
            throw new AssertionError("Should reject non-positive vmCount");
        } catch (IllegalArgumentException expected) {
        }

        try {
            hybrid.solve(Arrays.asList(new Task(0, 300)), 5, 0, 1L);
            throw new AssertionError("Should reject non-positive iterations");
        } catch (IllegalArgumentException expected) {
        }
        System.out.println("Hybrid GA-PSO input validation: PASS");
    }

    private static void testEmptyTaskList() {
        HybridGaPsoScheduler hybrid = new HybridGaPsoScheduler();
        Schedule schedule = hybrid.solve(Arrays.asList(), 5, 10, 1L);
        if (schedule.getTaskCount() != 0) {
            throw new AssertionError("Empty task list must yield schedule with 0 tasks");
        }
        System.out.println("Hybrid GA-PSO empty task list handling: PASS");
    }
}
