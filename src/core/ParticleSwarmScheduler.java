package core;

import java.util.List;
import java.util.Random;

/**
 * Particle Swarm Optimization (PSO) scheduler for cloud task scheduling.
 *
 * Implements discrete mapping from continuous velocity dynamics:
 * v_{p, t}^{(k+1)} = w * v_{p, t}^{(k)} + c_1 * r_1 * (pbest_{p, t} - x_{p, t}^{(k)}) + c_2 * r_2 * (gbest_t - x_{p, t}^{(k)})
 * x_{p, t}^{(k+1)} = clamp(round(x_{p, t}^{(k)} + v_{p, t}^{(k+1)}), 0, vmCount - 1)
 */
public class ParticleSwarmScheduler implements SchedulingAlgorithm {

    public static final int SWARM_SIZE = 75;
    public static final double INERTIA = 0.9;
    public static final double C1 = 1.5;
    public static final double C2 = 1.5;

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
            return new Schedule(0);
        }

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

        // Optimization iterations
        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int p = 0; p < SWARM_SIZE; p++) {
                for (int t = 0; t < taskCount; t++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();

                    // Standard PSO velocity update equation with persistent inertia
                    velocities[p][t] = (INERTIA * velocities[p][t])
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
        }

        return toSchedule(globalBest);
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
