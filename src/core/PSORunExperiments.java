package core;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PSORunExperiments {

    public static void main(String[] args) throws Exception {

        int vmCount = 10; // Table 1: 10 VMs
        SchedulingAlgorithm pso = new ParticleSwarmScheduler();

        List<ExperimentRunner.TrialResult> allResults =
                new ArrayList<ExperimentRunner.TrialResult>();

        for (ScenarioConfiguration scenario : PaperScenarios.all()) {

            System.out.println(
                    "Running Scenario " + scenario.getScenarioId()
                    + " | Tasks: " + scenario.getTaskCount()
                    + " | Iterations: " + scenario.getIterations()
            );

            ExperimentRunner runner =
                    new ExperimentRunner(pso, scenario, vmCount);

            allResults.addAll(runner.run());
        }

        ExperimentResultWriter.write(
                Paths.get("results", "pso_normal_results.csv"),
                allResults
        );

        System.out.println("PSO experiments completed!");
        System.out.println("Total trials: " + allResults.size());
        System.out.println(
                "CSV saved to: results\\pso_normal_results.csv"
        );
    }
}
