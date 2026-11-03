package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Verification and validation tests for SimulatedAnnealingHybridGaPsoScheduler.
 */
public class SimulatedAnnealingHybridGaPsoSchedulerTest {

    public static void main(String[] args) {
        System.out.println("--- Starting SA-Hybrid GA-PSO Verification Suite ---");

        testMetropolisAcceptanceCriterion();
        testMetropolisNumericalSafeguards();
        testDeterministicExecution();
        testScheduleValidityAndBounds();
        testMonotonicConvergenceHistory();
        testInputValidation();
        testEmptyTaskListHandling();
        testCoolingScheduleMonotonicity();

        System.out.println("==================================================");
        System.out.println("All SA-Hybrid GA-PSO tests: PASS");
        System.out.println("==================================================");
    }

    private static void testMetropolisAcceptanceCriterion() {
        Random random = new Random(42);

        // 1. deltaE <= 0 must always accept regardless of temperature
        assertTrue(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(-10.0, 1.0, random),
                "Metropolis must accept strictly negative deltaE");
        assertTrue(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(0.0, 1.0, random),
                "Metropolis must accept zero deltaE");
        assertTrue(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(-5.0, 0.0001, random),
                "Metropolis must accept improvement even at near-zero temperature");

        // 2. High temperature acceptance rate validation
        // deltaE = 1.0, T = 100.0 => P = exp(-0.01) approx 0.99005
        int acceptedCount = 0;
        int trials = 10000;
        for (int i = 0; i < trials; i++) {
            if (SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(1.0, 100.0, random)) {
                acceptedCount++;
            }
        }
        double observedRatio = (double) acceptedCount / trials;
        double expectedRatio = Math.exp(-1.0 / 100.0); // ~0.99
        assertTrue(Math.abs(observedRatio - expectedRatio) < 0.02,
                "High-T acceptance ratio (" + observedRatio + ") must match theoretical (" + expectedRatio + ")");

        // 3. Low temperature acceptance rate validation
        // deltaE = 10.0, T = 1.0 => P = exp(-10) approx 4.54e-5
        acceptedCount = 0;
        for (int i = 0; i < trials; i++) {
            if (SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(10.0, 1.0, random)) {
                acceptedCount++;
            }
        }
        assertTrue(acceptedCount < 5,
                "Low-T acceptance should be practically zero (observed: " + acceptedCount + "/" + trials + ")");

        System.out.println("Metropolis Acceptance Criterion: PASS");
    }

    private static void testMetropolisNumericalSafeguards() {
        Random random = new Random(101);

        // Non-positive or invalid temperatures must never accept worse moves
        assertFalse(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(1.0, 0.0, random),
                "Zero temperature must reject deltaE > 0");
        assertFalse(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(1.0, -5.0, random),
                "Negative temperature must reject deltaE > 0");
        assertFalse(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(1.0, Double.NaN, random),
                "NaN temperature must reject deltaE > 0");
        assertFalse(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(1.0, Double.POSITIVE_INFINITY, random),
                "Infinite temperature must reject deltaE > 0");

        // Extreme deltaE underflow safeguard (exp(-100000) does not throw or crash)
        assertFalse(SimulatedAnnealingHybridGaPsoScheduler.acceptMetropolis(1_000_000.0, 1.0, random),
                "Extreme positive deltaE must safely reject without underflow exceptions");

        System.out.println("Metropolis Numerical Safeguards: PASS");
    }

    private static void testDeterministicExecution() {
        List<Task> tasks = sampleTasks(60);
        int vmCount = 10;
        int iterations = 20;
        long seed = 12345L;

        SimulatedAnnealingHybridGaPsoScheduler scheduler1 = new SimulatedAnnealingHybridGaPsoScheduler();
        Schedule schedule1 = scheduler1.solve(tasks, vmCount, iterations, seed);

        SimulatedAnnealingHybridGaPsoScheduler scheduler2 = new SimulatedAnnealingHybridGaPsoScheduler();
        Schedule schedule2 = scheduler2.solve(tasks, vmCount, iterations, seed);

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        double makespan1 = evaluator.evaluate(tasks, schedule1).getMakespan();
        double makespan2 = evaluator.evaluate(tasks, schedule2).getMakespan();

        assertEquals(makespan1, makespan2, 1e-9, "Makespan must be identical for identical seeds");

        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(schedule1.getVmId(i), schedule2.getVmId(i),
                    "Task " + i + " must be assigned to identical VM");
        }

        System.out.println("SA-Hybrid GA-PSO deterministic execution: PASS");
    }

    private static void testScheduleValidityAndBounds() {
        List<Task> tasks = sampleTasks(100);
        int vmCount = 10;
        int iterations = 30;
        long seed = 777L;

        SimulatedAnnealingHybridGaPsoScheduler scheduler = new SimulatedAnnealingHybridGaPsoScheduler();
        Schedule schedule = scheduler.solve(tasks, vmCount, iterations, seed);

        assertEquals(tasks.size(), schedule.getTaskCount(), "Schedule must contain assignment for every task");

        boolean[] vmUsed = new boolean[vmCount];
        for (int i = 0; i < tasks.size(); i++) {
            int vm = schedule.getVmId(i);
            assertTrue(vm >= 0 && vm < vmCount, "Assigned VM index " + vm + " out of bounds [0, " + (vmCount - 1) + "]");
            vmUsed[vm] = true;
        }

        System.out.println("Schedule validity and VM bounds: PASS");
    }

    private static void testMonotonicConvergenceHistory() {
        List<Task> tasks = sampleTasks(50);
        int vmCount = 10;
        int iterations = 40;
        long seed = 999L;

        SimulatedAnnealingHybridGaPsoScheduler scheduler = new SimulatedAnnealingHybridGaPsoScheduler();
        Schedule schedule = scheduler.solve(tasks, vmCount, iterations, seed);

        List<Double> history = scheduler.getConvergenceHistory();
        assertFalse(history.isEmpty(), "Convergence history must not be empty");

        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i) <= history.get(i - 1) + 1e-9,
                    "Convergence history must be monotonically non-increasing at step " + i +
                            " (step i: " + history.get(i) + " > step i-1: " + history.get(i - 1) + ")");
        }

        // Final makespan must match last entry of convergence history
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        double finalMakespan = evaluator.evaluate(tasks, schedule).getMakespan();
        assertEquals(finalMakespan, history.get(history.size() - 1), 1e-6,
                "Final schedule makespan must match final convergence history entry");

        System.out.println("Monotonic Convergence History: PASS");
    }

    private static void testCoolingScheduleMonotonicity() {
        List<Task> tasks = sampleTasks(50);
        int vmCount = 10;
        int iterations = 40;
        long seed = 888L;

        SimulatedAnnealingHybridGaPsoScheduler scheduler = new SimulatedAnnealingHybridGaPsoScheduler();
        scheduler.solve(tasks, vmCount, iterations, seed);

        List<Double> temps = scheduler.getTemperatureHistory();
        assertFalse(temps.isEmpty(), "Temperature history must not be empty");

        for (int i = 1; i < temps.size(); i++) {
            assertTrue(temps.get(i) <= temps.get(i - 1) + 1e-12,
                    "Temperature must not increase during geometric cooling");
            assertTrue(temps.get(i) >= scheduler.getMinTemperature() - 1e-12,
                    "Temperature must not drop below minTemperature");
        }

        System.out.println("Cooling Schedule Monotonicity: PASS");
    }

    private static void testInputValidation() {
        SimulatedAnnealingHybridGaPsoScheduler scheduler = new SimulatedAnnealingHybridGaPsoScheduler();

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
            new SimulatedAnnealingHybridGaPsoScheduler(-1.0, 0.95, 1e-4, true, true);
            throw new AssertionError("Should reject negative initial temperature");
        } catch (IllegalArgumentException expected) {
        }

        try {
            new SimulatedAnnealingHybridGaPsoScheduler(10.0, 1.5, 1e-4, true, true);
            throw new AssertionError("Should reject cooling rate >= 1.0");
        } catch (IllegalArgumentException expected) {
        }

        System.out.println("Input validation: PASS");
    }

    private static void testEmptyTaskListHandling() {
        SimulatedAnnealingHybridGaPsoScheduler scheduler = new SimulatedAnnealingHybridGaPsoScheduler();
        Schedule schedule = scheduler.solve(Collections.emptyList(), 10, 10, 1L);
        assertEquals(0, schedule.getTaskCount(), "Empty task list must produce empty schedule");

        System.out.println("Empty task list handling: PASS");
    }

    private static List<Task> sampleTasks(int count) {
        List<Task> tasks = new ArrayList<Task>();
        for (int i = 0; i < count; i++) {
            tasks.add(new Task(i, 100 + (i % 200)));
        }
        return tasks;
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
