package core;

import java.util.List;

public class GATest {
    public static void main(String[] args) {

        List<Task> tasks = new WorkloadGenerator().generateNormalTasks(50);

        SchedulingAlgorithm ga = new GeneticAlgorithmScheduler();

        Schedule schedule = ga.solve(tasks, 5, 50, 42L);

        FitnessEvaluator evaluator = new FitnessEvaluator(5);

        FitnessEvaluator.Result result = evaluator.evaluate(tasks, schedule);

        System.out.println("Algorithm: Genetic Algorithm");
        System.out.println("Tasks: " + tasks.size());
        System.out.println("VMs: 5");
        System.out.println("Iterations: 50");
        System.out.println("Makespan: " + result.getMakespan());
        System.out.println("Schedule is valid: " +
                (schedule.getTaskCount() == tasks.size()));
    }
}