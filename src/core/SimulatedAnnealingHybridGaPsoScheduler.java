package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Simulated Annealing Enhanced Hybrid GA-PSO Scheduler for Cloud Task Scheduling.
 *
 * Implements a research improvement over the co-evolutionary Hybrid GA-PSO algorithm
 * (Kaplan & Babalik, Electronics 2025, 14, 1988), inspired by the paper's recommendation
 * of incorporating probabilistic acceptance mechanisms to escape local optima.
 *
 * In standard Hybrid GA-PSO, particles update their personal best (pBest) strictly greedily:
 * only candidates with strictly lower makespan are accepted. Consequently, particles
 * rapidly lose diversity and can get trapped in suboptimal schedule configurations.
 *
 * This scheduler integrates the Metropolis Acceptance Criterion into the particle trajectory
 * search and global best refinement:
 *   - Any candidate schedule with lower or equal makespan (deltaE <= 0) is ALWAYS accepted.
 *   - Any candidate schedule with higher makespan (deltaE > 0) is accepted PROBABILISTICALLY
 *     with probability P = exp(-deltaE / T), where T is the current system temperature.
 *
 * System temperature cools geometrically according to T(t+1) = max(T_min, alpha * T(t)).
 * At high initial temperatures, particles explore diverse VM assignment configurations by
 * occasionally accepting worse schedules. As temperature gradually drops, the acceptance
 * probability of worse moves decays towards zero, converging into strict local exploitation.
 *
 * Safeguards:
 * - Deterministic reproducibility when given the same random seed.
 * - Handles T <= 0, NaN, and Inf safely without division-by-zero or crashes.
 * - Underflow protection for large negative exponents in Math.exp().
 * - Task-to-VM assignment clamping guarantees valid VM bounds [0, vmCount - 1].
 * - Incumbent global-best tracking guarantees that the final returned schedule is the absolute
 *   best valid schedule encountered across all iterations.
 */
public class SimulatedAnnealingHybridGaPsoScheduler implements SchedulingAlgorithm {

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

    // Simulated Annealing Parameters
    public static final double DEFAULT_INITIAL_TEMPERATURE = 10.0;
    public static final double DEFAULT_COOLING_RATE = 0.95;
    public static final double DEFAULT_MIN_TEMPERATURE = 1e-4;
    public static final double DEFAULT_ADAPTIVE_TEMP_FRACTION = 0.02; // 2% of initial makespan
    public static final boolean DEFAULT_USE_ADAPTIVE_TEMPERATURE = true;
    public static final boolean DEFAULT_ENABLE_LOCAL_SEARCH = true;

    private final double configuredInitialTemperature;
    private final double coolingRate;
    private final double minTemperature;
    private final double adaptiveTempFraction;
    private final boolean adaptiveTemperature;
    private final boolean enableLocalSearch;

    // Telemetry fields for experiment analysis
    private final List<Double> convergenceHistory = new ArrayList<Double>();
    private final List<Double> temperatureHistory = new ArrayList<Double>();
    private int acceptedWorseMovesCount = 0;
    private int totalEvaluationsCount = 0;

    /**
     * Default constructor with research-recommended parameters.
     */
    public SimulatedAnnealingHybridGaPsoScheduler() {
        this(DEFAULT_INITIAL_TEMPERATURE, DEFAULT_COOLING_RATE, DEFAULT_MIN_TEMPERATURE,
                DEFAULT_ADAPTIVE_TEMP_FRACTION, DEFAULT_USE_ADAPTIVE_TEMPERATURE, DEFAULT_ENABLE_LOCAL_SEARCH);
    }

    /**
     * Parameterized constructor for custom cooling configurations.
     *
     * @param initialTemperature initial temperature T0 (used when adaptiveTemperature is false)
     * @param coolingRate        geometric cooling rate alpha in (0, 1)
     * @param minTemperature     minimum temperature floor T_min > 0
     * @param adaptiveTempFraction fraction of initial makespan to set T0 when adaptive
     * @param adaptiveTemperature whether to scale T0 proportionally to initial workload makespan
     * @param enableLocalSearch  whether to apply SA-guided neighborhood perturbation to gBest
     */
    public SimulatedAnnealingHybridGaPsoScheduler(
            double initialTemperature,
            double coolingRate,
            double minTemperature,
            double adaptiveTempFraction,
            boolean adaptiveTemperature,
            boolean enableLocalSearch) {

        if (initialTemperature <= 0.0 || Double.isNaN(initialTemperature)) {
            throw new IllegalArgumentException("Initial temperature must be positive and finite");
        }
        if (coolingRate <= 0.0 || coolingRate >= 1.0 || Double.isNaN(coolingRate)) {
            throw new IllegalArgumentException("Cooling rate must be in the open interval (0, 1)");
        }
        if (minTemperature <= 0.0 || Double.isNaN(minTemperature)) {
            throw new IllegalArgumentException("Minimum temperature must be positive and finite");
        }
        if (adaptiveTempFraction <= 0.0 || Double.isNaN(adaptiveTempFraction)) {
            throw new IllegalArgumentException("Adaptive temperature fraction must be positive");
        }

        this.configuredInitialTemperature = initialTemperature;
        this.coolingRate = coolingRate;
        this.minTemperature = minTemperature;
        this.adaptiveTempFraction = adaptiveTempFraction;
        this.adaptiveTemperature = adaptiveTemperature;
        this.enableLocalSearch = enableLocalSearch;
    }

    public SimulatedAnnealingHybridGaPsoScheduler(
            double initialTemperature,
            double coolingRate,
            double minTemperature,
            boolean adaptiveTemperature,
            boolean enableLocalSearch) {
        this(initialTemperature, coolingRate, minTemperature,
                DEFAULT_ADAPTIVE_TEMP_FRACTION, adaptiveTemperature, enableLocalSearch);
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
        temperatureHistory.clear();
        acceptedWorseMovesCount = 0;
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

        // Initialize Temperature
        double currentTemperature;
        if (adaptiveTemperature) {
            currentTemperature = Math.max(minTemperature, globalBestFitness * adaptiveTempFraction);
        } else {
            currentTemperature = configuredInitialTemperature;
        }

        // Incumbent best schedule: monotonically tracks the absolute best valid schedule found
        int[] incumbentBest = gBest.clone();
        double incumbentFitness = globalBestFitness;

        convergenceHistory.add(incumbentFitness);
        temperatureHistory.add(currentTemperature);

        double lastPsoBestFitness = globalBestFitness;
        int stagnationCounter = 0;

        // 2. Iteration Loop with Simulated Annealing
        for (int iter = 0; iter < hybridIterations; iter++) {

            // Step a: PSO Swarm Update with Metropolis Acceptance Criterion
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
                    // Clamp to valid VM indices [0, vmCount - 1]
                    psoPositions[p][t] = Math.max(0, Math.min(vmCount - 1, newVm));
                }

                psoCurrentFitness[p] = evaluate(tasks, psoPositions[p], evaluator);
                totalEvaluationsCount++;

                // Metropolis Acceptance Criterion for Personal Best update
                double deltaE = psoCurrentFitness[p] - psoPersonalBestFitness[p];
                if (acceptMetropolis(deltaE, currentTemperature, random)) {
                    if (deltaE > 0.0) {
                        acceptedWorseMovesCount++;
                    }
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

            // Step c: Hybridization & Stagnation Injection
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

            // Step d: Optional SA Local Neighborhood Search on gBest
            if (enableLocalSearch && taskCount > 0 && vmCount > 1) {
                int[] neighbor = gBest.clone();
                // Randomly perturb 1 task assignment to an alternate VM
                int targetTask = random.nextInt(taskCount);
                int currentVm = neighbor[targetTask];
                int newVm = random.nextInt(vmCount - 1);
                if (newVm >= currentVm) {
                    newVm++;
                }
                neighbor[targetTask] = newVm;

                double neighborFitness = evaluate(tasks, neighbor, evaluator);
                totalEvaluationsCount++;

                double deltaE = neighborFitness - globalBestFitness;
                if (acceptMetropolis(deltaE, currentTemperature, random)) {
                    if (deltaE > 0.0) {
                        acceptedWorseMovesCount++;
                    }
                    gBest = neighbor;
                    globalBestFitness = neighborFitness;
                }
            }

            // Monotonically update incumbent best (best valid schedule encountered so far)
            if (globalBestFitness < incumbentFitness) {
                incumbentFitness = globalBestFitness;
                incumbentBest = gBest.clone();
            }

            // Step e: Simulated Annealing Cooling Schedule
            currentTemperature = Math.max(minTemperature, currentTemperature * coolingRate);

            // Record telemetry
            convergenceHistory.add(incumbentFitness);
            temperatureHistory.add(currentTemperature);
        }

        return toSchedule(incumbentBest);
    }

    /**
     * Metropolis acceptance criterion for minimization.
     *
     * @param deltaE      change in energy: candidateFitness - currentFitness
     * @param temperature current system temperature T
     * @param random      random number generator
     * @return true if accepted, false otherwise
     */
    public static boolean acceptMetropolis(double deltaE, double temperature, Random random) {
        // Rule 1: Always accept improvements or identical fitness
        if (deltaE <= 0.0) {
            return true;
        }

        // Rule 2: Safeguards against invalid, zero, or negative temperatures
        if (temperature <= 0.0 || Double.isNaN(temperature) || Double.isInfinite(temperature)) {
            return false;
        }

        // Rule 3: Compute acceptance probability P = exp(-deltaE / T)
        double exponent = -deltaE / temperature;
        if (Double.isNaN(exponent)) {
            return false;
        }

        // Underflow protection: exp(-700) is ~1e-304; beyond that Math.exp rounds to 0.0
        if (exponent < -700.0) {
            return false;
        }

        double acceptanceProbability = Math.exp(exponent);
        return random.nextDouble() < acceptanceProbability;
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

    public List<Double> getTemperatureHistory() {
        return Collections.unmodifiableList(temperatureHistory);
    }

    public int getAcceptedWorseMovesCount() {
        return acceptedWorseMovesCount;
    }

    public int getTotalEvaluationsCount() {
        return totalEvaluationsCount;
    }

    public double getConfiguredInitialTemperature() {
        return configuredInitialTemperature;
    }

    public double getCoolingRate() {
        return coolingRate;
    }

    public double getMinTemperature() {
        return minTemperature;
    }

    public boolean isAdaptiveTemperature() {
        return adaptiveTemperature;
    }

    public boolean isEnableLocalSearch() {
        return enableLocalSearch;
    }
}
