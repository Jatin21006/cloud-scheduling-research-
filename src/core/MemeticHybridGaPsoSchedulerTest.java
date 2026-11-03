package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Verification and validation test suite for MemeticHybridGaPsoScheduler.
 */
public class MemeticHybridGaPsoSchedulerTest {

    public static void main(String[] args) {
        System.out.println("--- Starting Memetic Hybrid GA-PSO Verification Suite ---");

        testHillClimbNeighborValidity();
        testHillClimbStrictImprovementAcceptance();
        testHillClimbRejectionOfWorseCandidates();
        testDeterministicExecution();
        testScheduleValidityAndBounds();
        testMonotonicConvergenceHistory();
        testLocalSearchBudgetConstraint();
        testInputValidation();
        testEmptyTaskListHandling();
        testTelemetryTracking();

        System.out.println("==================================================");
        System.out.println("All Memetic Hybrid GA-PSO tests: PASS");
        System.out.println("==================================================");
    }

    private static void testHillClimbNeighborValidity() {
        int vmCount = 10;
        List<Task> tasks = sampleTasks(60);
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        Random random = new Random(42);

        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();

        int[] initialAssignment = new int[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            initialAssignment[i] = random.nextInt(vmCount);
        }

        Schedule s = toSchedule(initialAssignment);
        double initialMakespan = evaluator.evaluate(tasks, s).getMakespan();

        MemeticHybridGaPsoScheduler.HillClimbResult result =
                scheduler.hillClimb(initialAssignment, initialMakespan, tasks, vmCount, 15, evaluator, random);

        // Result assignment must be valid
        assertEquals(tasks.size(), result.assignment.length, "Assignment length must match task count");
        for (int i = 0; i < tasks.size(); i++) {
            int vm = result.assignment[i];
            assertTrue(vm >= 0 && vm < vmCount, "VM index " + vm + " must be in [0, vmCount - 1]");
        }

        // Makespan must be equal or better
        assertTrue(result.makespan <= initialMakespan + 1e-9, "Hill climb must never return worse makespan");

        System.out.println("Hill-climb neighbor validity: PASS");
    }

    private static void testHillClimbStrictImprovementAcceptance() {
        // Construct a severely unbalanced schedule where all tasks are placed on VM 0
        int vmCount = 5;
        List<Task> tasks = sampleTasks(20);
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        Random random = new Random(101);

        int[] unbalancedAssignment = new int[tasks.size()]; // all on VM 0
        Schedule s = toSchedule(unbalancedAssignment);
        double initialMakespan = evaluator.evaluate(tasks, s).getMakespan();

        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();
        MemeticHybridGaPsoScheduler.HillClimbResult result =
                scheduler.hillClimb(unbalancedAssignment, initialMakespan, tasks, vmCount, 10, evaluator, random);

        assertTrue(result.improved, "Hill climb must improve an unbalanced bottleneck schedule");
        assertTrue(result.makespan < initialMakespan, "Improved makespan must be strictly less than initial");

        System.out.println("Strict improvement acceptance: PASS");
    }

    private static void testHillClimbRejectionOfWorseCandidates() {
        // When maxSteps is 0 (or already optimal), no worse candidate should ever be accepted
        int vmCount = 10;
        List<Task> tasks = sampleTasks(10);
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        Random random = new Random(999);

        // Put 1 task per VM
        int[] balanced = new int[10];
        for (int i = 0; i < 10; i++) balanced[i] = i;

        Schedule s = toSchedule(balanced);
        double baseMakespan = evaluator.evaluate(tasks, s).getMakespan();

        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();
        MemeticHybridGaPsoScheduler.HillClimbResult result =
                scheduler.hillClimb(balanced, baseMakespan, tasks, vmCount, 5, evaluator, random);

        // Result makespan must be <= baseMakespan (never degraded)
        assertTrue(result.makespan <= baseMakespan, "Hill climb must never accept worse candidate");

        System.out.println("Rejection of worse candidates: PASS");
    }

    private static void testDeterministicExecution() {
        List<Task> tasks = sampleTasks(60);
        int vmCount = 10;
        int iterations = 20;
        long seed = 12345L;

        MemeticHybridGaPsoScheduler scheduler1 = new MemeticHybridGaPsoScheduler();
        Schedule schedule1 = scheduler1.solve(tasks, vmCount, iterations, seed);

        MemeticHybridGaPsoScheduler scheduler2 = new MemeticHybridGaPsoScheduler();
        Schedule schedule2 = scheduler2.solve(tasks, vmCount, iterations, seed);

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        double makespan1 = evaluator.evaluate(tasks, schedule1).getMakespan();
        double makespan2 = evaluator.evaluate(tasks, schedule2).getMakespan();

        assertEquals(makespan1, makespan2, 1e-9, "Makespans must be identical for identical seeds");

        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(schedule1.getVmId(i), schedule2.getVmId(i),
                    "Task " + i + " must have identical VM assignment");
        }

        System.out.println("Memetic Hybrid GA-PSO deterministic execution: PASS");
    }

    private static void testScheduleValidityAndBounds() {
        List<Task> tasks = sampleTasks(100);
        int vmCount = 10;
        int iterations = 30;
        long seed = 777L;

        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();
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

        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();
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

    private static void testLocalSearchBudgetConstraint() {
        List<Task> tasks = sampleTasks(50);
        int vmCount = 10;
        int iterations = 20; // 10 hybrid generations
        int maxSteps = 5;

        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler(maxSteps, 1, true);
        scheduler.solve(tasks, vmCount, iterations, 42L);

        // In 10 generations with max 5 steps on 1 individual: max possible local evals = 10 * 5 = 50
        assertTrue(scheduler.getLocalSearchEvaluationsCount() <= 10 * maxSteps,
                "Local search evaluations must not exceed step budget");

        System.out.println("Local search budget constraints: PASS (Actual Local Evals: " +
                scheduler.getLocalSearchEvaluationsCount() + " <= " + (10 * maxSteps) + ")");
    }

    private static void testInputValidation() {
        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();

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

        // Test constructor validation
        try {
            new MemeticHybridGaPsoScheduler(0, 1, false);
            throw new AssertionError("Should reject maxLocalSteps <= 0");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new MemeticHybridGaPsoScheduler(10, 0, false);
            throw new AssertionError("Should reject localSearchFrequency <= 0");
        } catch (IllegalArgumentException expected) {
        }

        System.out.println("Input validation: PASS");
    }

    private static void testEmptyTaskListHandling() {
        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();
        Schedule schedule = scheduler.solve(Collections.emptyList(), 10, 10, 1L);
        assertEquals(0, schedule.getTaskCount(), "Empty task list must produce empty schedule");

        System.out.println("Empty task list handling: PASS");
    }

    private static void testTelemetryTracking() {
        List<Task> tasks = sampleTasks(60);
        int vmCount = 10;
        int iterations = 20;
        long seed = 333L;

        MemeticHybridGaPsoScheduler scheduler = new MemeticHybridGaPsoScheduler();
        scheduler.solve(tasks, vmCount, iterations, seed);

        assertTrue(scheduler.getTotalEvaluationsCount() > 0, "Total evaluations must be recorded");
        assertTrue(scheduler.getLocalSearchEvaluationsCount() > 0, "Local search evaluations must be recorded");
        assertTrue(scheduler.getLocalSearchImprovementsCount() >= 0, "Improvements count must be non-negative");

        System.out.println("Telemetry Tracking: PASS (Total: " + scheduler.getTotalEvaluationsCount() +
                ", Local Evals: " + scheduler.getLocalSearchEvaluationsCount() +
                ", Improvements: " + scheduler.getLocalSearchImprovementsCount() + ")");
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

