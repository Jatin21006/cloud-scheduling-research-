package core;

import java.util.List;
import java.util.Random;

/**
 * Hybrid GA-PSO scheduler for cloud task scheduling.
 *
 * Implements the co-evolutionary dual-population architecture defined in the paper:
 * - Kaplan & Babalik, Electronics 2025, 14, 1988 (Algorithm 1, 2, 3, 4, 5 & Table 2).
 *
 * Key Architectural Components:
 * 1. Dual Populations: GA population (N=75) and PSO swarm (N=75) initialized independently.
 * 2. Iteration Reduction: Employs n/2 iterations (Lines 420-423) for computational efficiency.
 * 3. Independent Iterative Step:
 *    - PSO updates velocities, positions, personal bests, and swarm best.
 *    - GA evolves through tournament selection, single-point crossover, and mutation (pm=0.0015).
 * 4. Stagnation-Driven Hybridization:
 *    - Tracks PSO swarm stagnation (threshold = 0.5, count = 15).
 *    - When PSO stagnates, the fittest GA individual replaces the worst particle in the PSO swarm.
 * 5. Global Best Tracking: gBest = min(gBest, fittestGA).
 */
public class HybridGaPsoScheduler implements SchedulingAlgorithm {

    public static final int SWARM_SIZE = 75;
    public static final int GA_POPULATION_SIZE = 75;

    public static final double INERTIA = 0.9;
    public static final double C1 = 1.5;
    public static final double C2 = 1.5;

    public static final int TOURNAMENT_SIZE = 5;
    public static final double CROSSOVER_PROBABILITY = 0.5;
    public static final double MUTATION_PROBABILITY = 0.0015; // Table 2: 0.0015 for Hybrid

    public static final double STAGNATION_THRESHOLD = 0.5;
    public static final int STAGNATION_COUNT = 15;

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

        // Section 3.1, Lines 420-423: Hybrid GA-PSO adopts n/2 iterations
        int hybridIterations = Math.max(1, iterations / 2);

        // 1. Initialization (Algorithm 2)
        // GA Population
        int[][] gaPopulation = new int[GA_POPULATION_SIZE][taskCount];
        double[] gaFitness = new double[GA_POPULATION_SIZE];
        for (int i = 0; i < GA_POPULATION_SIZE; i++) {
            for (int t = 0; t < taskCount; t++) {
                gaPopulation[i][t] = random.nextInt(vmCount);
            }
            gaFitness[i] = evaluate(tasks, gaPopulation[i], evaluator);
        }

        // PSO Swarm
        int[][] psoPositions = new int[SWARM_SIZE][taskCount];
        double[][] psoVelocities = new double[SWARM_SIZE][taskCount];
        int[][] psoPersonalBest = new int[SWARM_SIZE][taskCount];
        double[] psoPersonalBestFitness = new double[SWARM_SIZE];
        double[] psoCurrentFitness = new double[SWARM_SIZE];

        double maxVelocity = vmCount - 1;

        int[] gBest = null;
        double globalBestFitness = Double.POSITIVE_INFINITY;

        for (int p = 0; p < SWARM_SIZE; p++) {
            for (int t = 0; t < taskCount; t++) {
                psoPositions[p][t] = random.nextInt(vmCount);
                psoVelocities[p][t] = (random.nextDouble() * 2.0 - 1.0);
            }
            psoPersonalBest[p] = psoPositions[p].clone();
            psoCurrentFitness[p] = evaluate(tasks, psoPositions[p], evaluator);
            psoPersonalBestFitness[p] = psoCurrentFitness[p];

            if (psoPersonalBestFitness[p] < globalBestFitness) {
                globalBestFitness = psoPersonalBestFitness[p];
                gBest = psoPersonalBest[p].clone();
            }
        }

        // Check GA best initially
        int initialFittestGaIdx = findBestIndex(gaFitness);
        if (gaFitness[initialFittestGaIdx] < globalBestFitness) {
            globalBestFitness = gaFitness[initialFittestGaIdx];
            gBest = gaPopulation[initialFittestGaIdx].clone();
        }

        double lastPsoBestFitness = globalBestFitness;
        int stagnationCounter = 0;

        // 2. Iteration Loop (Algorithm 4 & 5)
        for (int iter = 0; iter < hybridIterations; iter++) {

            // Step a: PSO Swarm Update
            double currentPsoBestInSwarm = Double.POSITIVE_INFINITY;
            int worstParticleIdx = 0;
            double worstParticleFitness = Double.NEGATIVE_INFINITY;

            for (int p = 0; p < SWARM_SIZE; p++) {
                for (int t = 0; t < taskCount; t++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();

                    psoVelocities[p][t] = (INERTIA * psoVelocities[p][t])
                            + (C1 * r1 * (psoPersonalBest[p][t] - psoPositions[p][t]))
                            + (C2 * r2 * (gBest[t] - psoPositions[p][t]));

                    if (psoVelocities[p][t] > maxVelocity) {
                        psoVelocities[p][t] = maxVelocity;
                    } else if (psoVelocities[p][t] < -maxVelocity) {
                        psoVelocities[p][t] = -maxVelocity;
                    }

                    int step = (int) Math.round(psoVelocities[p][t]);
                    int newVm = psoPositions[p][t] + step;
                    psoPositions[p][t] = Math.max(0, Math.min(vmCount - 1, newVm));
                }

                psoCurrentFitness[p] = evaluate(tasks, psoPositions[p], evaluator);

                if (psoCurrentFitness[p] < psoPersonalBestFitness[p]) {
                    psoPersonalBest[p] = psoPositions[p].clone();
                    psoPersonalBestFitness[p] = psoCurrentFitness[p];
                }

                if (psoPersonalBestFitness[p] < currentPsoBestInSwarm) {
                    currentPsoBestInSwarm = psoPersonalBestFitness[p];
                }

                if (psoCurrentFitness[p] > worstParticleFitness) {
                    worstParticleFitness = psoCurrentFitness[p];
                    worstParticleIdx = p;
                }
            }

            // PSO Stagnation Evaluation
            if (Math.abs(lastPsoBestFitness - currentPsoBestInSwarm) < STAGNATION_THRESHOLD) {
                stagnationCounter++;
            } else {
                stagnationCounter = 0;
                lastPsoBestFitness = currentPsoBestInSwarm;
            }

            // Step b: GA Population Evolution
            int[][] nextGaPopulation = new int[GA_POPULATION_SIZE][taskCount];
            int eliteGaIdx = findBestIndex(gaFitness);
            nextGaPopulation[0] = gaPopulation[eliteGaIdx].clone(); // Elitism

            int nextIndex = 1;
            while (nextIndex < GA_POPULATION_SIZE) {
                int p1 = tournamentSelect(gaFitness, random);
                int p2 = tournamentSelect(gaFitness, random);

                int[] child1 = gaPopulation[p1].clone();
                int[] child2 = gaPopulation[p2].clone();

                if (taskCount > 1 && random.nextDouble() < CROSSOVER_PROBABILITY) {
                    int point = 1 + random.nextInt(taskCount - 1);
                    for (int t = point; t < taskCount; t++) {
                        child1[t] = gaPopulation[p2][t];
                        child2[t] = gaPopulation[p1][t];
                    }
                }

                mutate(child1, vmCount, random, MUTATION_PROBABILITY);
                mutate(child2, vmCount, random, MUTATION_PROBABILITY);

                nextGaPopulation[nextIndex++] = child1;
                if (nextIndex < GA_POPULATION_SIZE) {
                    nextGaPopulation[nextIndex++] = child2;
                }
            }

            gaPopulation = nextGaPopulation;
            for (int i = 0; i < GA_POPULATION_SIZE; i++) {
                gaFitness[i] = evaluate(tasks, gaPopulation[i], evaluator);
            }

            int fittestGaIdx = findBestIndex(gaFitness);
            int[] fittestGA = gaPopulation[fittestGaIdx];
            double fittestGaFitness = gaFitness[fittestGaIdx];

            // Step c: Hybridization & Stagnation Injection (Algorithm 3 & 5)
            if (stagnationCounter >= STAGNATION_COUNT) {
                // Replace worst particle in PSO swarm with fittest GA individual
                psoPositions[worstParticleIdx] = fittestGA.clone();
                psoCurrentFitness[worstParticleIdx] = fittestGaFitness;
                psoPersonalBest[worstParticleIdx] = fittestGA.clone();
                psoPersonalBestFitness[worstParticleIdx] = fittestGaFitness;

                // Reset velocity for injected particle
                for (int t = 0; t < taskCount; t++) {
                    psoVelocities[worstParticleIdx][t] = (random.nextDouble() * 2.0 - 1.0);
                }

                stagnationCounter = 0;
                lastPsoBestFitness = Math.min(lastPsoBestFitness, fittestGaFitness);
            }

            // Update overall global best
            if (currentPsoBestInSwarm < globalBestFitness) {
                globalBestFitness = currentPsoBestInSwarm;
                for (int p = 0; p < SWARM_SIZE; p++) {
                    if (psoPersonalBestFitness[p] == globalBestFitness) {
                        gBest = psoPersonalBest[p].clone();
                        break;
                    }
                }
            }
            if (fittestGaFitness < globalBestFitness) {
                globalBestFitness = fittestGaFitness;
                gBest = fittestGA.clone();
            }
        }

        return toSchedule(gBest);
    }

    private int tournamentSelect(double[] fitness, Random random) {
        int best = random.nextInt(GA_POPULATION_SIZE);
        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            int candidate = random.nextInt(GA_POPULATION_SIZE);
            if (fitness[candidate] < fitness[best]) {
                best = candidate;
            }
        }
        return best;
    }

    private int findBestIndex(double[] fitness) {
        int bestIndex = 0;
        for (int i = 1; i < fitness.length; i++) {
            if (fitness[i] < fitness[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void mutate(int[] chromosome, int vmCount, Random random, double rate) {
        for (int t = 0; t < chromosome.length; t++) {
            if (random.nextDouble() < rate) {
                chromosome[t] = random.nextInt(vmCount);
            }
        }
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
