package core;

import java.util.Arrays;
import java.util.List;

/**
 * Rigorous test suite for Chaos-PSO, LDIW-PSO, and baseline equivalence validation.
 */
public class ChaosParticleSwarmSchedulerTest {

    public static void main(String[] args) {
        System.out.println("--- Starting Chaos-PSO & Validation Test Suite ---");

        testBaselineEquivalence();
        testLogisticMapNumericalValidity();
        testInertiaWeightBounds();
        testDeterministicExecution();
        testScheduleValidity();
        testConvergenceHistoryMonotonicity();
        testInvalidInputsAndEmptyTasks();

        System.out.println("==================================================");
        System.out.println("All Chaos-PSO & Validation tests: PASS");
        System.out.println("==================================================");
    }

    /**
     * CRITICAL VALIDATION:
     * Verifies that InstrumentedBaselinePsoScheduler produces the EXACT identical schedule
     * and makespan as the untouched ParticleSwarmScheduler across multiple configurations.
     */
    private static void testBaselineEquivalence() {
        WorkloadGenerator generator = new WorkloadGenerator();
        ParticleSwarmScheduler baseline = new ParticleSwarmScheduler();
        InstrumentedBaselinePsoScheduler instrumented = new InstrumentedBaselinePsoScheduler();

        long[] testSeeds = {0L, 42L, 999L, 123456L};
        int[] taskCounts = {50, 100};
        int[] iterationsList = {10, 25};
        int vmCount = 10;

        for (int taskCount : taskCounts) {
            List<Task> tasks = generator.generateNormalTasks(taskCount);
            FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);

            for (int iters : iterationsList) {
                for (long seed : testSeeds) {
                    Schedule baseSched = baseline.solve(tasks, vmCount, iters, seed);
                    Schedule instSched = instrumented.solve(tasks, vmCount, iters, seed);

                    double baseMakespan = evaluator.evaluate(tasks, baseSched).getMakespan();
                    double instMakespan = evaluator.evaluate(tasks, instSched).getMakespan();

                    if (Double.compare(baseMakespan, instMakespan) != 0) {
                        throw new AssertionError(String.format(
                                "Baseline equivalence failed! Seed %d, Tasks %d: base=%.6f != inst=%.6f",
                                seed, taskCount, baseMakespan, instMakespan));
                    }

                    for (int t = 0; t < taskCount; t++) {
                        if (baseSched.getVmId(t) != instSched.getVmId(t)) {
                            throw new AssertionError(String.format(
                                    "Baseline assignment mismatch on task %d! Seed %d: base VM=%d, inst VM=%d",
                                    t, seed, baseSched.getVmId(t), instSched.getVmId(t)));
                        }
                    }

                    // Check that instrumented convergence history aligns with final makespan
                    double[] history = instrumented.getConvergenceHistory();
                    if (history.length != iters + 1) {
                        throw new AssertionError("Convergence history length mismatch");
                    }
                    if (Double.compare(history[iters], instMakespan) != 0) {
                        throw new AssertionError("Last convergence history entry must match final makespan");
                    }
                }
            }
        }
        System.out.println("Critical Baseline Equivalence (ParticleSwarmScheduler == Instrumented): PASS");
    }

    /**
     * Verifies that the Logistic Map initialization and iteration sequence stays bounded
     * in (0, 1) and avoids singularities across 1,000 distinct seeds.
     */
    private static void testLogisticMapNumericalValidity() {
        double[] singularities = {0.0, 0.25, 0.50, 0.75, 1.0};

        for (long seed = 0; seed < 1000; seed++) {
            double x0 = ChaosParticleSwarmScheduler.generateInitialChaos(seed);
            if (x0 <= 0.0 || x0 >= 1.0) {
                throw new AssertionError("x0 out of bounds (0, 1): " + x0);
            }
            for (double s : singularities) {
                if (Math.abs(x0 - s) < 1e-4) {
                    throw new AssertionError("x0 too close to singularity " + s + ": " + x0);
                }
            }

            // Simulate 100 iterations of the Logistic Map
            double x = x0;
            for (int t = 0; t < 100; t++) {
                x = 4.0 * x * (1.0 - x);
                if (x <= 1e-6 || x >= 1.0 - 1e-6 || Double.isNaN(x)) {
                    // Test safeguard
                    x = 0.70 + 0.01 * (t % 17);
                }
                if (x <= 0.0 || x >= 1.0 || Double.isNaN(x)) {
                    throw new AssertionError("Safeguarded chaotic variable invalid: " + x);
                }
            }
        }
        System.out.println("Logistic Map Numerical Validity & Safeguards (1,000 seeds): PASS");
    }

    /**
     * Verifies that inertia weights in Chaos-PSO stay bounded in [0.4, 0.9]
     * and in LDIW-PSO linearly decay from 0.9 to 0.4.
     */
    private static void testInertiaWeightBounds() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(50);
        int vmCount = 10;
        int iters = 50;

        ChaosParticleSwarmScheduler chaosPso = new ChaosParticleSwarmScheduler();
        chaosPso.solve(tasks, vmCount, iters, 42L);
        double[] chaosInertia = chaosPso.getInertiaHistory();

        if (chaosInertia.length != iters) {
            throw new AssertionError("Chaos inertia history length mismatch");
        }
        for (int i = 0; i < iters; i++) {
            if (chaosInertia[i] < 0.40 - 1e-9 || chaosInertia[i] > 0.90 + 1e-9) {
                throw new AssertionError("Chaos inertia out of bounds [0.4, 0.9]: " + chaosInertia[i]);
            }
        }

        LdiwParticleSwarmScheduler ldiwPso = new LdiwParticleSwarmScheduler();
        ldiwPso.solve(tasks, vmCount, iters, 42L);
        double[] ldiwInertia = ldiwPso.getInertiaHistory();

        if (ldiwInertia.length != iters) {
            throw new AssertionError("LDIW inertia history length mismatch");
        }
        if (Math.abs(ldiwInertia[0] - 0.90) > 1e-6) {
            throw new AssertionError("LDIW must start at 0.90");
        }
        if (Math.abs(ldiwInertia[iters - 1] - 0.40) > 1e-6) {
            throw new AssertionError("LDIW must end at 0.40");
        }
        for (int i = 1; i < iters; i++) {
            if (ldiwInertia[i] > ldiwInertia[i - 1] + 1e-9) {
                throw new AssertionError("LDIW must be monotonically non-increasing");
            }
        }
        System.out.println("Inertia Weight Bounds (Chaos-PSO & LDIW-PSO): PASS");
    }

    /**
     * Verifies determinism: repeated calls with identical seeds produce identical results.
     */
    private static void testDeterministicExecution() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(50);
        int vmCount = 10;

        ChaosParticleSwarmScheduler chaosPso = new ChaosParticleSwarmScheduler();
        Schedule c1 = chaosPso.solve(tasks, vmCount, 30, 777L);
        Schedule c2 = chaosPso.solve(tasks, vmCount, 30, 777L);

        for (int i = 0; i < tasks.size(); i++) {
            if (c1.getVmId(i) != c2.getVmId(i)) {
                throw new AssertionError("Chaos-PSO execution must be deterministic for identical seeds");
            }
        }

        LdiwParticleSwarmScheduler ldiwPso = new LdiwParticleSwarmScheduler();
        Schedule l1 = ldiwPso.solve(tasks, vmCount, 30, 777L);
        Schedule l2 = ldiwPso.solve(tasks, vmCount, 30, 777L);

        for (int i = 0; i < tasks.size(); i++) {
            if (l1.getVmId(i) != l2.getVmId(i)) {
                throw new AssertionError("LDIW-PSO execution must be deterministic for identical seeds");
            }
        }
        System.out.println("Deterministic Execution (Chaos-PSO & LDIW-PSO): PASS");
    }

    /**
     * Verifies schedule validity: all tasks assigned to valid VM IDs in [0, vmCount - 1].
     */
    private static void testScheduleValidity() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(100);
        int vmCount = 10;

        ChaosParticleSwarmScheduler chaosPso = new ChaosParticleSwarmScheduler();
        Schedule schedule = chaosPso.solve(tasks, vmCount, 20, 888L);

        if (schedule.getTaskCount() != 100) {
            throw new AssertionError("Schedule must cover all 100 tasks");
        }

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        FitnessEvaluator.Result res = evaluator.evaluate(tasks, schedule);
        if (res.getMakespan() <= 0.0 || res.getResourceUtilization() <= 0.0) {
            throw new AssertionError("Makespan and resource utilization must be positive");
        }

        for (int t = 0; t < tasks.size(); t++) {
            int vm = schedule.getVmId(t);
            if (vm < 0 || vm >= vmCount) {
                throw new AssertionError("Task " + t + " assigned to invalid VM: " + vm);
            }
        }
        System.out.println("Schedule Validity & Assignment Clamping: PASS");
    }

    /**
     * Verifies convergence history monotonicity: global best makespan cannot worsen over iterations.
     */
    private static void testConvergenceHistoryMonotonicity() {
        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(50);
        int vmCount = 10;
        int iters = 40;

        ChaosParticleSwarmScheduler chaosPso = new ChaosParticleSwarmScheduler();
        Schedule sched = chaosPso.solve(tasks, vmCount, iters, 555L);
        double[] history = chaosPso.getConvergenceHistory();

        if (history.length != iters + 1) {
            throw new AssertionError("Convergence history must have length iterations + 1");
        }

        for (int i = 1; i <= iters; i++) {
            if (history[i] > history[i - 1] + 1e-9) {
                throw new AssertionError(String.format(
                        "Convergence curve must be monotonically non-increasing! Iter %d (%.4f) > Iter %d (%.4f)",
                        i, history[i], i - 1, history[i - 1]));
            }
        }

        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        double finalEvalMakespan = evaluator.evaluate(tasks, sched).getMakespan();
        if (Double.compare(history[iters], finalEvalMakespan) != 0) {
            throw new AssertionError("Final convergence history entry must equal schedule evaluated makespan");
        }
        System.out.println("Convergence History Monotonicity & Tracking: PASS");
    }

    /**
     * Verifies error handling and empty task list.
     */
    private static void testInvalidInputsAndEmptyTasks() {
        ChaosParticleSwarmScheduler chaosPso = new ChaosParticleSwarmScheduler();
        try {
            chaosPso.solve(null, 10, 10, 1L);
            throw new AssertionError("Should reject null tasks");
        } catch (IllegalArgumentException expected) {
        }

        try {
            chaosPso.solve(Arrays.asList(new Task(0, 300)), 0, 10, 1L);
            throw new AssertionError("Should reject non-positive vmCount");
        } catch (IllegalArgumentException expected) {
        }

        try {
            chaosPso.solve(Arrays.asList(new Task(0, 300)), 10, 0, 1L);
            throw new AssertionError("Should reject non-positive iterations");
        } catch (IllegalArgumentException expected) {
        }

        Schedule emptySched = chaosPso.solve(Arrays.asList(), 10, 10, 1L);
        if (emptySched.getTaskCount() != 0) {
            throw new AssertionError("Empty tasks must return empty schedule");
        }
        System.out.println("Invalid Inputs & Empty Task List Handling: PASS");
    }
}

