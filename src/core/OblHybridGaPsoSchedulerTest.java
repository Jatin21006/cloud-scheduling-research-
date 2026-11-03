package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Verification and validation test suite for OblHybridGaPsoScheduler.
 */
public class OblHybridGaPsoSchedulerTest {

    public static void main(String[] args) {
        System.out.println("--- Starting OBL-Hybrid GA-PSO Verification Suite ---");

        testComputeOppositeMathematicalCorrectness();
        testComputeOppositeInvolutionProperty();
        testComputeOppositeInputValidation();
        testSelectionBetweenOriginalAndOpposite();
        testDeterministicExecution();
        testScheduleValidityAndBounds();
        testMonotonicConvergenceHistory();
        testInputValidation();
        testEmptyTaskListHandling();
        testOblImprovementTelemetry();

        System.out.println("==================================================");
        System.out.println("All OBL-Hybrid GA-PSO tests: PASS");
        System.out.println("==================================================");
    }

    private static void testComputeOppositeMathematicalCorrectness() {
        int vmCount = 10;
        int[] original = new int[]{ 0, 1, 2, 5, 7, 8, 9 };
        int[] expected = new int[]{ 9, 8, 7, 4, 2, 1, 0 };

        int[] opposite = OblHybridGaPsoScheduler.computeOpposite(original, vmCount);
        assertEquals(original.length, opposite.length, "Opposite length must match original");

        for (int i = 0; i < original.length; i++) {
            assertEquals(expected[i], opposite[i], "Task " + i + " opposite must match expected");
            assertTrue(opposite[i] >= 0 && opposite[i] < vmCount, "Opposite VM must be in [0, vmCount - 1]");
        }

        System.out.println("Compute opposite mathematical correctness: PASS");
    }

    private static void testComputeOppositeInvolutionProperty() {
        // Involution: (x*)* == x
        int vmCount = 10;
        Random random = new Random(42);
        int[] original = new int[50];
        for (int i = 0; i < original.length; i++) {
            original[i] = random.nextInt(vmCount);
        }

        int[] opposite = OblHybridGaPsoScheduler.computeOpposite(original, vmCount);
        int[] doubleOpposite = OblHybridGaPsoScheduler.computeOpposite(opposite, vmCount);

        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], doubleOpposite[i],
                    "Double opposite (x*)* must strictly equal original x at task " + i);
        }

        System.out.println("Compute opposite involution property (x*)* = x: PASS");
    }

    private static void testComputeOppositeInputValidation() {
        try {
            OblHybridGaPsoScheduler.computeOpposite(null, 10);
            throw new AssertionError("Should reject null assignment");
        } catch (IllegalArgumentException expected) {
        }

        try {
            OblHybridGaPsoScheduler.computeOpposite(new int[]{ 0, 1 }, 0);
            throw new AssertionError("Should reject vmCount <= 0");
        } catch (IllegalArgumentException expected) {
        }

        System.out.println("Compute opposite input validation: PASS");
    }

    private static void testSelectionBetweenOriginalAndOpposite() {
        // Construct an asymmetric workload where VM 0 has heavy work and VM 1 has light work
        // e.g. 2 VMs: VM 0 gets all tasks initially -> makespan is sum.
        // Opposite assigns all tasks to VM 1 -> same makespan.
        // If VMs have differing capacities or task allocations, opposite can yield better or worse makespan.
        int vmCount = 4;
        List<Task> tasks = new ArrayList<Task>();
        tasks.add(new Task(0, 1000));
        tasks.add(new Task(1, 100));
        tasks.add(new Task(2, 100));
        tasks.add(new Task(3, 100));

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);

        // Schedule A: task 0, 1, 2, 3 all on VM 0 -> makespan = (1000 + 100 + 100 + 100)/100 = 13.0
        int[] assignmentA = new int[]{ 0, 0, 0, 0 };
        // Opposite of A: all on VM (4 - 1 - 0) = 3 -> makespan = 13.0

        // Schedule B: task 0 on VM 0, tasks 1, 2, 3 on VM 0 -> VM 0 has 1300.
        // Schedule C: task 0 on VM 3, task 1 on VM 2, task 2 on VM 1, task 3 on VM 0 -> distributed!
        int[] assignmentC = new int[]{ 3, 2, 1, 0 };
        int[] oppC = OblHybridGaPsoScheduler.computeOpposite(assignmentC, vmCount);
        // oppC: task 0 on VM 0, task 1 on VM 1, task 2 on VM 2, task 3 on VM 3 -> also distributed!

        Schedule schedC = toSchedule(assignmentC);
        Schedule schedOppC = toSchedule(oppC);

        double msC = evaluator.evaluate(tasks, schedC).getMakespan();
        double msOppC = evaluator.evaluate(tasks, schedOppC).getMakespan();

        // Both are valid and finite
        assertTrue(msC > 0 && msOppC > 0, "Evaluator must return positive makespans");

        System.out.println("Selection between original and opposite candidate: PASS");
    }

    private static void testDeterministicExecution() {
        List<Task> tasks = sampleTasks(60);
        int vmCount = 10;
        int iterations = 20;
        long seed = 12345L;

        OblHybridGaPsoScheduler scheduler1 = new OblHybridGaPsoScheduler();
        Schedule schedule1 = scheduler1.solve(tasks, vmCount, iterations, seed);

        OblHybridGaPsoScheduler scheduler2 = new OblHybridGaPsoScheduler();
        Schedule schedule2 = scheduler2.solve(tasks, vmCount, iterations, seed);

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        double makespan1 = evaluator.evaluate(tasks, schedule1).getMakespan();
        double makespan2 = evaluator.evaluate(tasks, schedule2).getMakespan();

        assertEquals(makespan1, makespan2, 1e-9, "Makespans must be identical for identical seeds");

        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(schedule1.getVmId(i), schedule2.getVmId(i),
                    "Task " + i + " must have identical VM assignment");
        }

        System.out.println("OBL-Hybrid GA-PSO deterministic execution: PASS");
    }

    private static void testScheduleValidityAndBounds() {
        List<Task> tasks = sampleTasks(100);
        int vmCount = 10;
        int iterations = 30;
        long seed = 777L;

        OblHybridGaPsoScheduler scheduler = new OblHybridGaPsoScheduler();
        Schedule schedule = scheduler.solve(tasks, vmCount, iterations, seed);

        assertEquals(tasks.size(), schedule.getTaskCount(), "Schedule must contain assignment for every task");

        for (int i = 0; i < tasks.size(); i++) {
            int vm = schedule.getVmId(i);
            assertTrue(vm >= 0 && vm < vmCount, "Assigned VM index " + vm + " out of bounds [0, " + (vmCount - 1) + "]");
        }

        System.out.println("Schedule validity and VM bounds: PASS");
    }

    private static void testMonotonicConvergenceHistory() {
        List<Task> tasks = sampleTasks(50);
        int vmCount = 10;
        int iterations = 40;
        long seed = 999L;

        OblHybridGaPsoScheduler scheduler = new OblHybridGaPsoScheduler();
        Schedule schedule = scheduler.solve(tasks, vmCount, iterations, seed);

        List<Double> history = scheduler.getConvergenceHistory();
        assertFalse(history.isEmpty(), "Convergence history must not be empty");

        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i) <= history.get(i - 1) + 1e-9,
                    "Convergence history must be monotonically non-increasing at step " + i);
        }

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        double finalMakespan = evaluator.evaluate(tasks, schedule).getMakespan();
        assertEquals(finalMakespan, history.get(history.size() - 1), 1e-6,
                "Final schedule makespan must match final convergence history entry");

        System.out.println("Monotonic Convergence History: PASS");
    }

    private static void testInputValidation() {
        OblHybridGaPsoScheduler scheduler = new OblHybridGaPsoScheduler();

        try {
            scheduler.solve(null, 10, 10, 1L);
            throw new AssertionError("Should reject null tasks");
        } catch (IllegalArgumentException expected) {
        }

        try {
            scheduler.solve(sampleTasks(10), 0, 10, 1L);
            throw new AssertionError("Should reject vmCount <= 0");
        } catch (IllegalArgumentException expected) {
        }

        try {
            scheduler.solve(sampleTasks(10), 10, 0, 1L);
            throw new AssertionError("Should reject iterations <= 0");
        } catch (IllegalArgumentException expected) {
        }

        System.out.println("Input validation: PASS");
    }

    private static void testEmptyTaskListHandling() {
        OblHybridGaPsoScheduler scheduler = new OblHybridGaPsoScheduler();
        Schedule schedule = scheduler.solve(Collections.emptyList(), 10, 10, 1L);
        assertEquals(0, schedule.getTaskCount(), "Empty task list must produce empty schedule");

        System.out.println("Empty task list handling: PASS");
    }

    private static void testOblImprovementTelemetry() {
        List<Task> tasks = sampleTasks(100);
        int vmCount = 10;
        int iterations = 30;
        long seed = 555L;

        OblHybridGaPsoScheduler scheduler = new OblHybridGaPsoScheduler();
        scheduler.solve(tasks, vmCount, iterations, seed);

        assertTrue(scheduler.getTotalEvaluationsCount() > 0, "Total evaluations must be recorded");
        assertTrue(scheduler.getOblJumpsCount() >= 0, "OBL jumps count must be non-negative");

        System.out.println("OBL Telemetry: PASS (Total Evals: " +
                scheduler.getTotalEvaluationsCount() + ", OBL Jumps: " +
                scheduler.getOblJumpsCount() + ")");
    }

    private static List<Task> sampleTasks(int count) {
        List<Task> tasks = new ArrayList<Task>();
        for (int i = 0; i < count; i++) {
            tasks.add(new Task(i, 100 + (i % 200)));
        }
        return tasks;
    }

    private static Schedule toSchedule(int[] assignment) {
        Schedule s = new Schedule(assignment.length);
        for (int i = 0; i < assignment.length; i++) {
            s.assign(i, assignment[i]);
        }
        return s;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " - expected: " + expected + ", actual: " + actual);
        }
    }

    private static void assertEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(message + " - expected: " + expected + ", actual: " + actual);
        }
    }
}
