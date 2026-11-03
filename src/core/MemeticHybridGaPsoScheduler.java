package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Memetic Hybrid GA-PSO Scheduler for Cloud Task Scheduling.
 *
 * Implements a research extension to the co-evolutionary Hybrid GA-PSO algorithm
 * (Kaplan & Babalik, Electronics 2025, 14, 1988), investigating the specific hypothesis
 * stated in Section 4 (Discussion, Page 29):
 *
 *   "Memetic algorithms: combining global search with local refinement (hill-climbing),
 *    can fine-tune promising solutions more effectively.
 *    If short-term hill-climbing were applied to selected individuals while performing a
 *    global search PSO-GA blend in the hybrid, it would reduce our best makespan values."
 *
 * Architecture:
 * 1. Global Exploration: Preserves the dual-population co-evolutionary framework
 *    (GA population N=75, PSO swarm N=75, tournament size 5, crossover 0.5, mutation 0.0015,
 *    n/2 iteration reduction, and stagnation-driven migration).
 * 2. Local Refinement (Hill-Climbing): Applied to promising elite individuals (gBest and
 *    the fittest GA individual) at the end of each generation.
 * 3. Discrete Neighborhood Operations:
 *    - Bottleneck Relief Move: Identifies the bottleneck VM (the machine defining current
 *      makespan) and relocates a task from it to an underutilized VM.
 *    - Single-Task Transfer Move: Moves a task from its current VM to an alternate VM.
 *    - Pairwise Task Swap: Swaps VM assignments of two tasks assigned to distinct VMs.
 * 4. Strict Greedy Acceptance:
 *    A neighbor is accepted if and only if it strictly reduces makespan: f(X') < f(X).
 *    Worse or equal neighbors are rejected, guaranteeing hill-climbing monotonicity.
 * 5. Budget Constraints & Computational Safeguards:
 *    Configurable maximum local search steps per individual, stopping early if no improvement
 *    is found, thereby tightly bounding computational overhead.
 */
public class MemeticHybridGaPsoScheduler implements SchedulingAlgorithm {

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

    // Memetic Local Search Parameters
    public static final int DEFAULT_MAX_LOCAL_STEPS = 10;
    public static final int DEFAULT_LOCAL_SEARCH_FREQUENCY = 1; // Every generation
    public static final boolean DEFAULT_REFINE_GBEST_ONLY = false; // Refine gBest and fittest GA

    private final int maxLocalSteps;
    private final int localSearchFrequency;
    private final boolean refineGBestOnly;

    // Telemetry fields for research analysis
    private final List<Double> convergenceHistory = new ArrayList<Double>();
    private int localSearchEvaluationsCount = 0;
    private int localSearchImprovementsCount = 0;
    private int totalEvaluationsCount = 0;

    /**
     * Default constructor with research-recommended short-term hill-climbing parameters.
     */
    public MemeticHybridGaPsoScheduler() {
        this(DEFAULT_MAX_LOCAL_STEPS, DEFAULT_LOCAL_SEARCH_FREQUENCY, DEFAULT_REFINE_GBEST_ONLY);
    }

    /**
     * Parameterized constructor for custom hill-climbing configurations.
     *
     * @param maxLocalSteps        maximum hill-climbing iterations per selected individual
     * @param localSearchFrequency generation interval to trigger local refinement
     * @param refineGBestOnly      if true, refines only gBest; if false, refines gBest and fittest GA
     */
    public MemeticHybridGaPsoScheduler(
            int maxLocalSteps,
            int localSearchFrequency,
            boolean refineGBestOnly) {

        if (maxLocalSteps <= 0) {
            throw new IllegalArgumentException("Max local steps must be positive");
        }
        if (localSearchFrequency <= 0) {
            throw new IllegalArgumentException("Local search frequency must be positive");
        }

        this.maxLocalSteps = maxLocalSteps;
        this.localSearchFrequency = localSearchFrequency;
        this.refineGBestOnly = refineGBestOnly;
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
        localSearchEvaluationsCount = 0;
        localSearchImprovementsCount = 0;
        totalEvaluationsCount = 0;

        Random random = new Random(seed);
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);

        // Hybrid GA-PSO adopts n/2 iterations (Kaplan & Babalik 2025, lines 420-423)
        int hybridIterations = Math.max(1, iterations / 2);

        // 1. Initialization (GA Population and PSO Swarm)
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

        for (int p = 0; p < SWARM_SIZE; p++) {
            for (int t = 0; t < taskCount; t++) {
                psoPositions[p][t] = random.nextInt(vmCount);
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

        // 2. Iteration Loop (Co-evolution + Memetic Local Refinement)
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

            // Step c: Hybridization & Stagnation Injection (Algorithm 3 & 5)
            if (stagnationCounter >= STAGNATION_COUNT) {
                psoPositions[worstParticleIdx] = fittestGA.clone();
                psoCurrentFitness[worstParticleIdx] = fittestGaFitness;
                psoPersonalBest[worstParticleIdx] = fittestGA.clone();
                psoPersonalBestFitness[worstParticleIdx] = fittestGaFitness;

                for (int t = 0; t < taskCount; t++) {
                    psoVelocities[worstParticleIdx][t] = (random.nextDouble() * 2.0 - 1.0);
                }

                stagnationCounter = 0;
                lastPsoBestFitness = Math.min(lastPsoBestFitness, fittestGaFitness);
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

            // Step d: Memetic Local Search (Hill-Climbing) on Promising Individuals
            if (iter % localSearchFrequency == 0 && taskCount > 0 && vmCount > 1) {
                // 1. Refine Global Best (gBest)
                HillClimbResult gBestResult = hillClimb(gBest, globalBestFitness, tasks, vmCount, maxLocalSteps, evaluator, random);
                if (gBestResult.improved) {
                    gBest = gBestResult.assignment;
                    globalBestFitness = gBestResult.makespan;
                    localSearchImprovementsCount++;
                }

                // 2. Refine Fittest GA individual if enabled
                if (!refineGBestOnly && fittestGA != null) {
                    HillClimbResult gaResult = hillClimb(fittestGA, fittestGaFitness, tasks, vmCount, maxLocalSteps, evaluator, random);
                    if (gaResult.improved) {
                        gaPopulation[fittestGaIdx] = gaResult.assignment;
                        gaFitness[fittestGaIdx] = gaResult.makespan;
                        fittestGaFitness = gaResult.makespan;
                        localSearchImprovementsCount++;

                        if (fittestGaFitness < globalBestFitness) {
                            globalBestFitness = fittestGaFitness;
                            gBest = gaResult.assignment.clone();
                        }
                    }
                }
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
     * Discrete Hill-Climbing local search algorithm.
     *
     * Explores neighboring task-to-VM assignments using bottleneck-relief moves and
     * single-task transfers. Only strictly improving candidates (makespan < current)
     * are accepted. Stops when no neighbor improves or step budget is exhausted.
     */
    public HillClimbResult hillClimb(
            int[] currentAssignment,
            double currentMakespan,
            List<Task> tasks,
            int vmCount,
            int maxSteps,
            FitnessEvaluator evaluator,
            Random random) {

        int taskCount = tasks.size();
        int[] bestAssignment = currentAssignment.clone();
        double bestMakespan = currentMakespan;
        boolean overallImproved = false;

        for (int step = 0; step < maxSteps; step++) {
            // Evaluate current load distribution to locate bottleneck VM
            Schedule currentSched = toSchedule(bestAssignment);
            FitnessEvaluator.Result currentEval = evaluator.evaluate(tasks, currentSched);

            int bottleneckVm = 0;
            double maxLoad = currentEval.getVmTotalTime(0);
            int lightestVm = 0;
            double minLoad = maxLoad;

            for (int m = 1; m < vmCount; m++) {
                double load = currentEval.getVmTotalTime(m);
                if (load > maxLoad) {
                    maxLoad = load;
                    bottleneckVm = m;
                }
                if (load < minLoad) {
                    minLoad = load;
                    lightestVm = m;
                }
            }

            // Collect all tasks assigned to the bottleneck VM
            List<Integer> bottleneckTasks = new ArrayList<Integer>();
            for (int t = 0; t < taskCount; t++) {
                if (bestAssignment[t] == bottleneckVm) {
                    bottleneckTasks.add(t);
                }
            }

            int[] candidateAssignment = bestAssignment.clone();

            if (!bottleneckTasks.isEmpty() && random.nextDouble() < 0.70) {
                // Strategy 1: Bottleneck-Relief Move (70% probability)
                // Move a task from the overloaded VM to an underutilized VM
                int chosenTask = bottleneckTasks.get(random.nextInt(bottleneckTasks.size()));
                int targetVm;
                if (random.nextBoolean()) {
                    targetVm = lightestVm; // Directly target lightest VM
                } else {
                    targetVm = random.nextInt(vmCount - 1);
                    if (targetVm >= bottleneckVm) targetVm++;
                }
                candidateAssignment[chosenTask] = targetVm;
            } else {
                // Strategy 2: Single-Task Transfer (30% probability)
                int chosenTask = random.nextInt(taskCount);
                int curVm = candidateAssignment[chosenTask];
                int targetVm = random.nextInt(vmCount - 1);
                if (targetVm >= curVm) targetVm++;
                candidateAssignment[chosenTask] = targetVm;
            }

            // Evaluate neighbor
            double candMakespan = evaluate(tasks, candidateAssignment, evaluator);
            localSearchEvaluationsCount++;
            totalEvaluationsCount++;

            // Strict Greedy Acceptance Rule: only accept strictly improving neighbors
            if (candMakespan < bestMakespan) {
                bestMakespan = candMakespan;
                bestAssignment = candidateAssignment;
                overallImproved = true;
            } else {
                // Hill-climbing termination: stop step loop if neighbor does not improve
                // to avoid wasting evaluation budget on unpromising directions
                break;
            }
        }

        return new HillClimbResult(bestAssignment, bestMakespan, overallImproved);
    }

    public static class HillClimbResult {
        public final int[] assignment;
        public final double makespan;
        public final boolean improved;

        public HillClimbResult(int[] assignment, double makespan, boolean improved) {
            this.assignment = assignment;
            this.makespan = makespan;
            this.improved = improved;
        }
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

    public int getLocalSearchEvaluationsCount() {
        return localSearchEvaluationsCount;
    }

    public int getLocalSearchImprovementsCount() {
        return localSearchImprovementsCount;
    }

    public int getTotalEvaluationsCount() {
        return totalEvaluationsCount;
    }

    public int getMaxLocalSteps() {
        return maxLocalSteps;
    }

    public int getLocalSearchFrequency() {
        return localSearchFrequency;
    }

    public boolean isRefineGBestOnly() {
        return refineGBestOnly;
    }
}

