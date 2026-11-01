package core;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithmScheduler implements SchedulingAlgorithm {

    private static final int POPULATION_SIZE = 75;
    private static final int TOURNAMENT_SIZE = 5;
    private static final double CROSSOVER_PROBABILITY = 0.5;
    private static final double MUTATION_PROBABILITY = 0.015;

    @Override
    public Schedule solve(
            List<Task> tasks,
            int vmCount,
            int iterations,
            long seed) {

        if (tasks == null) {
            throw new IllegalArgumentException("Tasks must not be null");
        }
        if (vmCount <= 0) {
            throw new IllegalArgumentException("VM count must be positive");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be positive");
        }

        Random random = new Random(seed);
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);

        int taskCount = tasks.size();

        // Each chromosome stores one VM assignment per task.
        int[][] population = new int[POPULATION_SIZE][taskCount];

        // Initialize the population with random valid VM assignments.
        for (int i = 0; i < POPULATION_SIZE; i++) {
            for (int gene = 0; gene < taskCount; gene++) {
                population[i][gene] = random.nextInt(vmCount);
            }
        }

        int[] bestChromosome = null;
        double bestMakespan = Double.POSITIVE_INFINITY;

        // Run the genetic algorithm for the requested iterations.
        for (int generation = 0; generation < iterations; generation++) {

            double[] fitness = new double[POPULATION_SIZE];

            // Evaluate every candidate using the existing makespan evaluator.
            for (int i = 0; i < POPULATION_SIZE; i++) {
                Schedule candidate = toSchedule(population[i]);
                fitness[i] = evaluator.evaluate(tasks, candidate).getMakespan();

                if (fitness[i] < bestMakespan) {
                    bestMakespan = fitness[i];
                    bestChromosome = population[i].clone();
                }
            }

            // No need to create another generation after the final evaluation.
            if (generation == iterations - 1) {
                break;
            }

            int[][] nextPopulation =
                    new int[POPULATION_SIZE][taskCount];

            // Elitism: carry the best chromosome into the next generation.
            int eliteIndex = findBestIndex(fitness);
            nextPopulation[0] = population[eliteIndex].clone();

            int nextIndex = 1;

            while (nextIndex < POPULATION_SIZE) {

                // Select parents using tournament selection.
                int parent1Index = tournamentSelection(fitness, random);
                int parent2Index = tournamentSelection(fitness, random);

                int[] parent1 = population[parent1Index];
                int[] parent2 = population[parent2Index];

                int[] child1 = parent1.clone();
                int[] child2 = parent2.clone();

                // Apply one-point crossover with the configured probability.
                if (taskCount > 1
                        && random.nextDouble() < CROSSOVER_PROBABILITY) {

                    int crossoverPoint = 1
                            + random.nextInt(taskCount - 1);

                    for (int gene = crossoverPoint;
                            gene < taskCount;
                            gene++) {

                        child1[gene] = parent2[gene];
                        child2[gene] = parent1[gene];
                    }
                }

                // Mutate each gene independently.
                mutate(child1, vmCount, random);
                mutate(child2, vmCount, random);

                nextPopulation[nextIndex++] = child1;

                if (nextIndex < POPULATION_SIZE) {
                    nextPopulation[nextIndex++] = child2;
                }
            }

            population = nextPopulation;
        }

        // Convert the best chromosome into the framework's Schedule object.
        return toSchedule(bestChromosome);
    }

    private int tournamentSelection(double[] fitness, Random random) {

        int bestIndex = random.nextInt(POPULATION_SIZE);

        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            int candidateIndex = random.nextInt(POPULATION_SIZE);

            if (fitness[candidateIndex] < fitness[bestIndex]) {
                bestIndex = candidateIndex;
            }
        }

        return bestIndex;
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

    private void mutate(
            int[] chromosome,
            int vmCount,
            Random random) {

        for (int gene = 0; gene < chromosome.length; gene++) {

            if (random.nextDouble() < MUTATION_PROBABILITY) {
                chromosome[gene] = random.nextInt(vmCount);
            }
        }
    }

    private Schedule toSchedule(int[] chromosome) {

        Schedule schedule = new Schedule(chromosome.length);

        for (int taskId = 0; taskId < chromosome.length; taskId++) {
            schedule.assign(taskId, chromosome[taskId]);
        }

        return schedule;
    }
}
