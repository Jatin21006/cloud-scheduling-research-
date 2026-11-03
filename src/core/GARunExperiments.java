package core;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GARunExperiments {

    public static void main(String[] args) throws Exception {

        int vmCount = 10; // Table 1: 10 VMs

        SchedulingAlgorithm ga = new GeneticAlgorithmScheduler();

        List<ExperimentRunner.TrialResult> allResults =
                new ArrayList<ExperimentRunner.TrialResult>();

        for (ScenarioConfiguration scenario : PaperScenarios.all()) {

            System.out.println(
                    "Running Scenario " + scenario.getScenarioId()
                    + " | Tasks: " + scenario.getTaskCount()
                    + " | Iterations: " + scenario.getIterations()
                    + " | Trials: " + scenario.getTrials()
            );

            ExperimentRunner runner =
                    new ExperimentRunner(ga, scenario, vmCount);

            List<ExperimentRunner.TrialResult> results = runner.run();

            allResults.addAll(results);

            for (ExperimentRunner.TrialResult result : results) {
                System.out.println(
                        "Trial " + result.getTrialNumber()
                        + " | Makespan: " + result.getMakespan()
                );
            }
        }

        ExperimentResultWriter.write(
                Paths.get("results", "ga_normal_results.csv"),
                allResults
        );

        System.out.println("\nGA experiments completed!");
        System.out.println("Total trials: " + allResults.size());
        System.out.println(
                "CSV saved to: results\\ga_normal_results.csv"
        );
    }
}
