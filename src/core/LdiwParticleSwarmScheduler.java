package core;

import java.util.List;
import java.util.Random;

/**
 * Linearly Decreasing Inertia Weight Particle Swarm Optimization (LDIW-PSO) scheduler.
 *
 * Implements the standard Shi & Eberhart (1998) linear decay dynamic inertia weight:
 * w(t) = W_MAX - (t / T) * (W_MAX - W_MIN)
 *
 * Serves as an experimental control to isolate the effect of dynamic/decreasing inertia
 * from the effect of chaotic ergodicity.
 */
public class LdiwParticleSwarmScheduler implements SchedulingAlgorithm {

    public static final int SWARM_SIZE = 75;
    public static final double W_MAX = 0.9;
    public static final double W_MIN = 0.4;
    public static final double C1 = 1.5;
    public static final double C2 = 1.5;

    private double[] convergenceHistory;
    private double[] inertiaHistory;

    @Override
    public Schedule solve(
            List<Task> tasks,
            int vmCount,
            int iterations,
            long seed) {

        if (tasks == null) {
            throw new IllegalArgumentException("Tasks must not be null");
        }
        if (vmCount <= 0 || iterations <= 0) {
            throw new IllegalArgumentException(
                    "VM count and iterations must be positive");
        }

        int taskCount = tasks.size();
        if (taskCount == 0) {
            convergenceHistory = new double[]{0.0};
            inertiaHistory = new double[]{W_MAX};
            return new Schedule(0);
        }

        convergenceHistory = new double[iterations + 1];
        inertiaHistory = new double[iterations];

        Random random = new Random(seed);
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);

        int[][] positions = new int[SWARM_SIZE][taskCount];
        double[][] velocities = new double[SWARM_SIZE][taskCount];
        int[][] personalBest = new int[SWARM_SIZE][taskCount];
        double[] personalBestFitness = new double[SWARM_SIZE];

        int[] globalBest = null;
        double globalBestFitness = Double.POSITIVE_INFINITY;

        double maxVelocity = vmCount - 1;

        // Swarm initialization
        for (int p = 0; p < SWARM_SIZE; p++) {
            for (int t = 0; t < taskCount; t++) {
                positions[p][t] = random.nextInt(vmCount);
                // Initialize velocity in [-1.0, 1.0]
                velocities[p][t] = (random.nextDouble() * 2.0 - 1.0);
            }

            personalBest[p] = positions[p].clone();
            personalBestFitness[p] = evaluate(tasks, positions[p], evaluator);

            if (personalBestFitness[p] < globalBestFitness) {
                globalBestFitness = personalBestFitness[p];
                globalBest = personalBest[p].clone();
            }
        }

        // Record initial best fitness before iterations begin
        convergenceHistory[0] = globalBestFitness;

        // Optimization iterations
        for (int iteration = 0; iteration < iterations; iteration++) {
            // Linearly decreasing inertia weight
            double progress = (iterations > 1) ? ((double) iteration / (iterations - 1)) : 0.0;
            double w = W_MAX - progress * (W_MAX - W_MIN);
            inertiaHistory[iteration] = w;

            for (int p = 0; p < SWARM_SIZE; p++) {
                for (int t = 0; t < taskCount; t++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();

                    // Velocity update with linear decreasing inertia
                    velocities[p][t] = (w * velocities[p][t])
                            + (C1 * r1 * (personalBest[p][t] - positions[p][t]))
                            + (C2 * r2 * (globalBest[t] - positions[p][t]));

                    // Velocity clamping
                    if (velocities[p][t] > maxVelocity) {
                        velocities[p][t] = maxVelocity;
                    } else if (velocities[p][t] < -maxVelocity) {
                        velocities[p][t] = -maxVelocity;
                    }

                    // Position update mapped to discrete VM index
                    int step = (int) Math.round(velocities[p][t]);
                    int newVm = positions[p][t] + step;
                    positions[p][t] = Math.max(0, Math.min(vmCount - 1, newVm));
                }

                double fitness = evaluate(tasks, positions[p], evaluator);

                // Update personal best
                if (fitness < personalBestFitness[p]) {
                    personalBest[p] = positions[p].clone();
                    personalBestFitness[p] = fitness;

                    // Update global best
                    if (fitness < globalBestFitness) {
                        globalBestFitness = fitness;
                        globalBest = personalBest[p].clone();
                    }
                }
            }

            // Record iteration telemetry
            convergenceHistory[iteration + 1] = globalBestFitness;
        }

        return toSchedule(globalBest);
    }

    public double[] getConvergenceHistory() {
        return convergenceHistory != null ? convergenceHistory.clone() : new double[0];
    }

    public double[] getInertiaHistory() {
        return inertiaHistory != null ? inertiaHistory.clone() : new double[0];
    }

    private double evaluate(
            List<Task> tasks,
            int[] assignment,
            FitnessEvaluator evaluator) {

        Schedule schedule = toSchedule(assignment);
        return evaluator.evaluate(tasks, schedule).getMakespan();
    }

    private Schedule toSchedule(int[] assignment) {
        Schedule schedule = new Schedule(assignment.length);

        for (int task = 0; task < assignment.length; task++) {
            schedule.assign(task, assignment[task]);
        }

        return schedule;
    }
}

