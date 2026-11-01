package core;

import java.util.List;
import java.util.Random;

/**
 * Artificial Bee Colony (ABC) scheduler for cloud task scheduling.
 *
 * Implements the standard ABC algorithmic framework adapted for discrete VM assignment:
 * 1. Employed Bee Phase: Exploration around food sources via neighborhood search.
 * 2. Onlooker Bee Phase: Probability-proportional selection (roulette wheel) based on makespan fitness.
 * 3. Scout Bee Phase: Abandonment of exhausted food sources exceeding trial limit.
 *
 * Note: Clearly designated as a proposed baseline implementation adhering to
 * standard ABC literature parameters for fair benchmark comparison against GA and PSO.
 */
public class ArtificialBeeColonyScheduler implements SchedulingAlgorithm {

    public static final int COLONY_SIZE = 75;
    public static final int FOOD_SOURCES = 75;
    public static final int LIMIT = 20;

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

        int[][] foodSources = new int[FOOD_SOURCES][taskCount];
        double[] makespans = new double[FOOD_SOURCES];
        int[] trialCounts = new int[FOOD_SOURCES];

        int[] globalBest = null;
        double globalBestMakespan = Double.POSITIVE_INFINITY;

        // Initialize food sources randomly
        for (int i = 0; i < FOOD_SOURCES; i++) {
            for (int t = 0; t < taskCount; t++) {
                foodSources[i][t] = random.nextInt(vmCount);
            }
            makespans[i] = evaluate(tasks, foodSources[i], evaluator);
            trialCounts[i] = 0;

            if (makespans[i] < globalBestMakespan) {
                globalBestMakespan = makespans[i];
                globalBest = foodSources[i].clone();
            }
        }

        // Iteration cycles
        for (int cycle = 0; cycle < iterations; cycle++) {

            // 1. Employed Bee Phase
            for (int i = 0; i < FOOD_SOURCES; i++) {
                int partner = random.nextInt(FOOD_SOURCES);
                while (partner == i && FOOD_SOURCES > 1) {
                    partner = random.nextInt(FOOD_SOURCES);
                }

                int[] candidate = foodSources[i].clone();
                // Perturb one or more task assignments using difference vector
                int paramToChange = random.nextInt(taskCount);
                double phi = (random.nextDouble() * 2.0) - 1.0;
                int diff = (int) Math.round(phi * (foodSources[i][paramToChange] - foodSources[partner][paramToChange]));
                int newVm = foodSources[i][paramToChange] + (diff == 0 ? (random.nextBoolean() ? 1 : -1) : diff);
                candidate[paramToChange] = Math.max(0, Math.min(vmCount - 1, newVm));

                double candidateMakespan = evaluate(tasks, candidate, evaluator);
                if (candidateMakespan <= makespans[i]) {
                    foodSources[i] = candidate;
                    makespans[i] = candidateMakespan;
                    trialCounts[i] = 0;
                    if (candidateMakespan < globalBestMakespan) {
                        globalBestMakespan = candidateMakespan;
                        globalBest = candidate.clone();
                    }
                } else {
                    trialCounts[i]++;
                }
            }

            // 2. Onlooker Bee Phase
            // Calculate probabilities based on fitness = 1 / (1 + makespan)
            double[] fitness = new double[FOOD_SOURCES];
            double totalFitness = 0.0;
            for (int i = 0; i < FOOD_SOURCES; i++) {
                fitness[i] = 1.0 / (1.0 + makespans[i]);
                totalFitness += fitness[i];
            }

            for (int i = 0; i < FOOD_SOURCES; i++) {
                // Roulette wheel selection
                double threshold = random.nextDouble() * totalFitness;
                double accumulated = 0.0;
                int selected = 0;
                for (int j = 0; j < FOOD_SOURCES; j++) {
                    accumulated += fitness[j];
                    if (accumulated >= threshold) {
                        selected = j;
                        break;
                    }
                }

                int partner = random.nextInt(FOOD_SOURCES);
                while (partner == selected && FOOD_SOURCES > 1) {
                    partner = random.nextInt(FOOD_SOURCES);
                }

                int[] candidate = foodSources[selected].clone();
                int paramToChange = random.nextInt(taskCount);
                double phi = (random.nextDouble() * 2.0) - 1.0;
                int diff = (int) Math.round(phi * (foodSources[selected][paramToChange] - foodSources[partner][paramToChange]));
                int newVm = foodSources[selected][paramToChange] + (diff == 0 ? (random.nextBoolean() ? 1 : -1) : diff);
                candidate[paramToChange] = Math.max(0, Math.min(vmCount - 1, newVm));

                double candidateMakespan = evaluate(tasks, candidate, evaluator);
                if (candidateMakespan <= makespans[selected]) {
                    foodSources[selected] = candidate;
                    makespans[selected] = candidateMakespan;
                    trialCounts[selected] = 0;
                    if (candidateMakespan < globalBestMakespan) {
                        globalBestMakespan = candidateMakespan;
                        globalBest = candidate.clone();
                    }
                } else {
                    trialCounts[selected]++;
                }
            }

            // 3. Scout Bee Phase
            for (int i = 0; i < FOOD_SOURCES; i++) {
                if (trialCounts[i] >= LIMIT) {
                    // Reinitialize food source
                    for (int t = 0; t < taskCount; t++) {
                        foodSources[i][t] = random.nextInt(vmCount);
                    }
                    makespans[i] = evaluate(tasks, foodSources[i], evaluator);
                    trialCounts[i] = 0;
                    if (makespans[i] < globalBestMakespan) {
                        globalBestMakespan = makespans[i];
                        globalBest = foodSources[i].clone();
                    }
                }
            }
        }

        return toSchedule(globalBest);
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
