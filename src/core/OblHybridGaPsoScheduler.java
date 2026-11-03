package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Opposition-Based Learning (OBL) Enhanced Hybrid GA-PSO Scheduler for Cloud Task Scheduling.
 *
 * Implements a research extension to the co-evolutionary Hybrid GA-PSO algorithm
 * (Kaplan & Babalik, Electronics 2025, 14, 1988), inspired by the paper's discussion
 * in Section 4 on diversity-enhancement techniques:
 *
 *   "Diversity enhancement techniques: periodically generate long jumps to explore new regions.
 *    It is observed that our study focuses on the intensification aspect of the GA-PSO hybrid.
 *    If diversity strategies such as random restart or opposition-based learning are added,
 *    the discovery of new solution sets during stagnation periods can increase, and the
 *    deviation in the worst-case makespan can be further reduced, according to our findings."
 *
 * Mathematical Formulation for Discrete Cloud Scheduling:
 * In continuous optimization, the opposite of x in [a, b] is x* = a + b - x (Tizhoosh, 2005).
 * In cloud task scheduling, a schedule maps each task t in [0, taskCount - 1] to a discrete VM
 * index in [0, vmCount - 1]. The lower bound is a = 0 and upper bound is b = vmCount - 1.
 * Thus, the discrete opposite assignment is defined as:
 *     x*_t = (vmCount - 1) - x_t
 *
 * OBL Integration Architecture:
 * 1. Opposition-Based Population Initialization (OBPI):
 *    In standard PSO, particles are initialized randomly. In OBPI, half the swarm (particles
 *    p in [38, 74]) is initialized as the diametrical discrete opposite of the first half
 *    (particles p in [0, 36]): x_{p, t} = (vmCount - 1) - x_{p - 38, t}.
 *    This ensures that the initial swarm uniformly populates both opposing sectors of the
 *    discrete search space, maximizing initial directional diversity.
 *
 * 2. Opposition-Based Stagnation Breakthrough (OBSB):
 *    When stagnation is detected (stagnationCounter >= STAGNATION_COUNT, or periodically every
 *    5 iterations when stagnationCounter >= 3), the scheduler generates a long jump by
 *    computing the opposite of the fittest GA individual:
 *        oppGA[t] = (vmCount - 1) - fittestGA[t]
 *    This opposite individual is evaluated and injected into the worst particle of the PSO
 *    swarm, resetting its velocity. This pushes the swarm away from local convergence basins
 *    into unexplored regions of the solution space.
 *
 * Safeguards:
 * - Deterministic reproducibility with identical seeds.
 * - Monotonic incumbent best tracking ensures the returned schedule is the absolute best encountered.
 * - Strict VM index bounds [0, vmCount - 1].
 * - Avoids redundant evaluations: opposite solutions are only evaluated once.
 */
public class OblHybridGaPsoScheduler implements SchedulingAlgorithm {

    // Inherited Hybrid GA-PSO Architecture Parameters
    public static final int SWARM_SIZE = 75;
    public static final int GA_POPULATION_SIZE = 75;

    public static final double INERTIA = 0.9;
    public static final double C1 = 1.5;
    public static final double C2 = 1.5;

    public static final int TOURNAMENT_SIZE = 5;
    public static final double CROSSOVER_PROBABILITY = 0.5;
    public static final double MUTATION_PROBABILITY = 0.0015;

    public static final double STAGNATION_THRESHOLD = 0.5;
    public static final int STAGNATION_COUNT = 15;
    public static final int PERIODIC_OBL_INTERVAL = 5;
    public static final int PERIODIC_STAGNATION_THRESHOLD = 3;

    // Configuration flags
    private final boolean enableInitializationObl;
    private final boolean enableStagnationObl;

    // Telemetry fields for research analysis
    private final List<Double> convergenceHistory = new ArrayList<Double>();
    private int oblJumpsCount = 0;
    private int totalEvaluationsCount = 0;

    /**
     * Default constructor enabling all standard OBL integration points.
     */
    public OblHybridGaPsoScheduler() {
        this(true, true);
    }

    /**
     * Parameterized constructor for ablation analysis.
     *
     * @param enableInitializationObl whether to apply OBL during initial swarm generation
     * @param enableStagnationObl     whether to apply OBL during stagnation / periodic long jumps
     */
    public OblHybridGaPsoScheduler(boolean enableInitializationObl, boolean enableStagnationObl) {
        this.enableInitializationObl = enableInitializationObl;
        this.enableStagnationObl = enableStagnationObl;
    }

    @Override
    public Schedule solve(List<Task> tasks, int vmCount, int iterations, long seed) {
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

        // Reset telemetry
        convergenceHistory.clear();
        oblJumpsCount = 0;
        totalEvaluationsCount = 0;

        Random random = new Random(seed);
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);

        // Hybrid GA-PSO adopts n/2 iterations (Kaplan & Babalik 2025, lines 420-423)
        int hybridIterations = Math.max(1, iterations / 2);

        // 1. Initialization (GA Population and PSO Swarm with OBPI)
        // GA Population
        int[][] gaPopulation = new int[GA_POPULATION_SIZE][taskCount];
        double[] gaFitness = new double[GA_POPULATION_SIZE];
        for (int i = 0; i < GA_POPULATION_SIZE; i++) {
            for (int t = 0; t < taskCount; t++) {
                gaPopulation[i][t] = random.nextInt(vmCount);
            }
            gaFitness[i] = evaluate(tasks, gaPopulation[i], evaluator);
            totalEvaluationsCount++;
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

        int halfSwarm = SWARM_SIZE / 2; // 37
        for (int p = 0; p < SWARM_SIZE; p++) {
            for (int t = 0; t < taskCount; t++) {
                if (enableInitializationObl && vmCount > 1 && p >= halfSwarm) {
                    // OBPI: Initialize second half as discrete opposite of first half
                    psoPositions[p][t] = (vmCount - 1) - psoPositions[p - halfSwarm][t];
                } else {
                    psoPositions[p][t] = random.nextInt(vmCount);
                }
                psoVelocities[p][t] = (random.nextDouble() * 2.0 - 1.0);
            }

            psoPersonalBest[p] = psoPositions[p].clone();
            psoCurrentFitness[p] = evaluate(tasks, psoPositions[p], evaluator);
            totalEvaluationsCount++;
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

        // Incumbent best tracking
        int[] incumbentBest = gBest.clone();
        double incumbentFitness = globalBestFitness;
        convergenceHistory.add(incumbentFitness);

        double lastPsoBestFitness = globalBestFitness;
        int stagnationCounter = 0;

        // 2. Iteration Loop (Algorithm 4 & 5 enhanced with OBL)
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
                totalEvaluationsCount++;

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
                totalEvaluationsCount++;
            }

            int fittestGaIdx = findBestIndex(gaFitness);
            int[] fittestGA = gaPopulation[fittestGaIdx];
            double fittestGaFitness = gaFitness[fittestGaIdx];

            // Step c: Hybridization & Stagnation Injection with Opposition-Based Learning
            boolean triggerPeriodicObl = enableStagnationObl && vmCount > 1 && iter > 0
                    && (iter % PERIODIC_OBL_INTERVAL == 0) && (stagnationCounter >= PERIODIC_STAGNATION_THRESHOLD);
            boolean triggerStandardStagnation = stagnationCounter >= STAGNATION_COUNT;

            if (triggerPeriodicObl || triggerStandardStagnation) {
                int[] injectionCandidate;
                double injectionFitness;

                if (enableStagnationObl && vmCount > 1) {
                    // OBL Long Jump: Generate opposite of fittest GA individual
                    int[] oppositeGA = computeOpposite(fittestGA, vmCount);
                    double oppositeGaFitness = evaluate(tasks, oppositeGA, evaluator);
                    totalEvaluationsCount++;

                    // Inject opposite candidate to explore opposing region of search space
                    injectionCandidate = oppositeGA;
                    injectionFitness = oppositeGaFitness;
                    oblJumpsCount++;
                } else {
                    // Baseline injection fallback
                    injectionCandidate = fittestGA.clone();
                    injectionFitness = fittestGaFitness;
                }

                // Inject into worst particle in PSO swarm
                psoPositions[worstParticleIdx] = injectionCandidate.clone();
                psoCurrentFitness[worstParticleIdx] = injectionFitness;
                psoPersonalBest[worstParticleIdx] = injectionCandidate.clone();
                psoPersonalBestFitness[worstParticleIdx] = injectionFitness;

                for (int t = 0; t < taskCount; t++) {
                    psoVelocities[worstParticleIdx][t] = (random.nextDouble() * 2.0 - 1.0);
                }

                stagnationCounter = 0;
                lastPsoBestFitness = Math.min(lastPsoBestFitness, injectionFitness);
            }

            // Update swarm and global best
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

            // Monotonically track incumbent best
            if (globalBestFitness < incumbentFitness) {
                incumbentFitness = globalBestFitness;
                incumbentBest = gBest.clone();
            }

            convergenceHistory.add(incumbentFitness);
        }

        return toSchedule(incumbentBest);
    }

    /**
     * Computes the discrete opposite vector for a scheduling solution.
     *
     * In discrete scheduling where each task is mapped to a VM in [0, vmCount - 1],
     * the opposite assignment for task t is:
     *     opposite[t] = (vmCount - 1) - assignment[t]
     *
     * @param assignment original task-to-VM assignment array
     * @param vmCount    total number of virtual machines
     * @return new array containing discrete opposite assignments
     */
    public static int[] computeOpposite(int[] assignment, int vmCount) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment array must not be null");
        }
        if (vmCount <= 0) {
            throw new IllegalArgumentException("VM count must be positive");
        }

        int[] opposite = new int[assignment.length];
        int maxVmIdx = vmCount - 1;
        for (int t = 0; t < assignment.length; t++) {
            int currentVm = assignment[t];
            if (currentVm < 0) currentVm = 0;
            if (currentVm > maxVmIdx) currentVm = maxVmIdx;
            opposite[t] = maxVmIdx - currentVm;
        }
        return opposite;
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

    // Telemetry getters
    public List<Double> getConvergenceHistory() {
        return Collections.unmodifiableList(convergenceHistory);
    }

    public int getOblJumpsCount() {
        return oblJumpsCount;
    }

    public int getTotalEvaluationsCount() {
        return totalEvaluationsCount;
    }

    public boolean isEnableInitializationObl() {
        return enableInitializationObl;
    }

    public boolean isEnableStagnationObl() {
        return enableStagnationObl;
    }
}
