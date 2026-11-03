package core;

import java.util.List;
import java.util.Random;

/**
 * Hybrid GA-PSO scheduler for cloud task scheduling.
 *
 * Integrates Particle Swarm Optimization (PSO) velocity momentum and memory
 * with Genetic Algorithm (GA) tournament selection, crossover, and mutation:
 *
 * Phase 1 (PSO Momentum): Particles update continuous velocities and step towards
 * personal and global best positions.
 * Phase 2 (GA Genetic Recombination): Sub-optimal particles undergo tournament selection
 * and one-point crossover to escape local optima, followed by uniform mutation.
 *
 * Clearly labeled as an optimization-engineered hybrid synthesis combining the verified
 * GA (pop=75, cross=0.5, mut=0.015) and PSO (w=0.9, c1=1.5, c2=1.5) parameters.
 */
public class HybridGaPsoScheduler implements SchedulingAlgorithm {

    public static final int SWARM_SIZE = 75;
    public static final double INERTIA = 0.9;
    public static final double C1 = 1.5;
    public static final double C2 = 1.5;
    public static final int TOURNAMENT_SIZE = 5;
    public static final double CROSSOVER_PROBABILITY = 0.5;
    public static final double MUTATION_PROBABILITY = 0.015;

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
            throw new IllegalArgumentException("VM count and iterations must be positive");
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
        double[] currentFitness = new double[SWARM_SIZE];

        int[] globalBest = null;
        double globalBestFitness = Double.POSITIVE_INFINITY;
        double maxVelocity = vmCount - 1;

        // Initialize swarm
        for (int p = 0; p < SWARM_SIZE; p++) {
            for (int t = 0; t < taskCount; t++) {
                positions[p][t] = random.nextInt(vmCount);
                velocities[p][t] = (random.nextDouble() * 2.0 - 1.0);
            }

            personalBest[p] = positions[p].clone();
            currentFitness[p] = evaluate(tasks, positions[p], evaluator);
            personalBestFitness[p] = currentFitness[p];

            if (personalBestFitness[p] < globalBestFitness) {
                globalBestFitness = personalBestFitness[p];
                globalBest = personalBest[p].clone();
            }
        }

        // Iteration loop
        for (int iteration = 0; iteration < iterations; iteration++) {

            // Step 1: PSO velocity and position updates
            for (int p = 0; p < SWARM_SIZE; p++) {
                for (int t = 0; t < taskCount; t++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();

                    velocities[p][t] = (INERTIA * velocities[p][t])
                            + (C1 * r1 * (personalBest[p][t] - positions[p][t]))
                            + (C2 * r2 * (globalBest[t] - positions[p][t]));

                    if (velocities[p][t] > maxVelocity) {
                        velocities[p][t] = maxVelocity;
                    } else if (velocities[p][t] < -maxVelocity) {
                        velocities[p][t] = -maxVelocity;
                    }

                    int step = (int) Math.round(velocities[p][t]);
                    int newVm = positions[p][t] + step;
                    positions[p][t] = Math.max(0, Math.min(vmCount - 1, newVm));
                }

                currentFitness[p] = evaluate(tasks, positions[p], evaluator);

                if (currentFitness[p] < personalBestFitness[p]) {
                    personalBest[p] = positions[p].clone();
                    personalBestFitness[p] = currentFitness[p];
                    if (currentFitness[p] < globalBestFitness) {
                        globalBestFitness = currentFitness[p];
                        globalBest = personalBest[p].clone();
                    }
                }
            }

            // Step 2: GA Crossover & Mutation phase with greedy acceptance to escape local optima
            // without corrupting particle trajectory or accepting worse offspring.
            if (taskCount > 1) {
                for (int p = 1; p < SWARM_SIZE; p++) {
                    int p1 = tournamentSelect(currentFitness, random);
                    int p2 = tournamentSelect(currentFitness, random);

                    int[] child = positions[p1].clone();

                    if (random.nextDouble() < CROSSOVER_PROBABILITY) {
                        int point = 1 + random.nextInt(taskCount - 1);
                        for (int t = point; t < taskCount; t++) {
                            child[t] = positions[p2][t];
                        }
                    }

                    mutate(child, vmCount, random);

                    double childFitness = evaluate(tasks, child, evaluator);

                    // Greedy acceptance: only accept offspring if it improves or equals current particle fitness
                    if (childFitness <= currentFitness[p]) {
                        positions[p] = child;
                        currentFitness[p] = childFitness;

                        if (childFitness < personalBestFitness[p]) {
                            personalBest[p] = child.clone();
                            personalBestFitness[p] = childFitness;

                            if (childFitness < globalBestFitness) {
                                globalBestFitness = childFitness;
                                globalBest = personalBest[p].clone();
                            }
                        }
                    }
                }
            }
        }

        return toSchedule(globalBest);
    }

    private int tournamentSelect(double[] fitness, Random random) {
        int bestIdx = random.nextInt(SWARM_SIZE);
        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            int candidate = random.nextInt(SWARM_SIZE);
            if (fitness[candidate] < fitness[bestIdx]) {
                bestIdx = candidate;
            }
        }
        return bestIdx;
    }

    private void mutate(int[] chromosome, int vmCount, Random random) {
        for (int gene = 0; gene < chromosome.length; gene++) {
            if (random.nextDouble() < MUTATION_PROBABILITY) {
                chromosome[gene] = random.nextInt(vmCount);
            }
        }
    }

    private double evaluate(List<Task> tasks, int[] assignment, FitnessEvaluator evaluator) {
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
