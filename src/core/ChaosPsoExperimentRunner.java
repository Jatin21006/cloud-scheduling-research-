package core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cloudsim.CloudSimScheduleAdapter;
import org.cloudbus.cloudsim.Cloudlet;

/**
 * Dedicated Experiment Runner for Chaos-Enhanced PSO research extension.
 *
 * Compares:
 *   1. Baseline Canonical PSO (w = 0.9) [Kaplan & Babalik, 2025 reproduction]
 *   2. Linearly Decreasing Inertia Weight PSO (LDIW-PSO) [Shi & Eberhart control]
 *   3. Chaos-Enhanced PSO (Chaos-PSO) [Proposed extension]
 *
 * Evaluates across all 6 paper scenarios, both Normal and DDoS workloads,
 * across 10 independent trials with controlled reproducible seeds (0..9).
 * Total trial runs: 3 algorithms x 6 scenarios x 2 workloads x 10 trials = 360 runs.
 *
 * All outputs saved into: results/chaos_pso/ (raw/, summary/, convergence/, charts/)
 */
public class ChaosPsoExperimentRunner {

    public static final long DEFAULT_BASE_SEED = 0L;
    public static final int DEFAULT_VM_COUNT = 10; // 10 VMs @ 100 MIPS each

    public static class ExperimentRecord {
        public final String algorithm;
        public final int scenarioId;
        public final String workloadType;
        public final int taskCount;
        public final int vmCount;
        public final int iterations;
        public final int trialNumber;
        public final long seed;
        public final double analyticalMakespan;
        public final double analyticalResourceUtilization;
        public final double cloudSimMakespan;
        public final double cloudSimResourceUtilization;
        public final long executionTimeMs;
        public final String status;
        public final String errorDetails;

        public ExperimentRecord(
                String algorithm,
                int scenarioId,
                String workloadType,
                int taskCount,
                int vmCount,
                int iterations,
                int trialNumber,
                long seed,
                double analyticalMakespan,
                double analyticalResourceUtilization,
                double cloudSimMakespan,
                double cloudSimResourceUtilization,
                long executionTimeMs,
                String status,
                String errorDetails) {
            this.algorithm = algorithm;
            this.scenarioId = scenarioId;
            this.workloadType = workloadType;
            this.taskCount = taskCount;
            this.vmCount = vmCount;
            this.iterations = iterations;
            this.trialNumber = trialNumber;
            this.seed = seed;
            this.analyticalMakespan = analyticalMakespan;
            this.analyticalResourceUtilization = analyticalResourceUtilization;
            this.cloudSimMakespan = cloudSimMakespan;
            this.cloudSimResourceUtilization = cloudSimResourceUtilization;
            this.executionTimeMs = executionTimeMs;
            this.status = status;
            this.errorDetails = errorDetails;
        }

        public String toCsvRow() {
            return String.format("%s,%d,%s,%d,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%d,%s,%s",
                    algorithm, scenarioId, workloadType, taskCount, vmCount,
                    iterations, trialNumber, seed,
                    analyticalMakespan, analyticalResourceUtilization,
                    cloudSimMakespan, cloudSimResourceUtilization,
                    executionTimeMs, status, escapeCsv(errorDetails));
        }

        private static String escapeCsv(String val) {
            if (val == null || val.isEmpty()) return "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                return "\"" + val.replace("\"", "\"\"") + "\"";
            }
            return val;
        }
    }

    public static class TrialConvergencePoint {
        public final String workloadType;
        public final int scenarioId;
        public final String algorithm;
        public final int trialNumber;
        public final long seed;
        public final int iteration;
        public final double makespan;

        public TrialConvergencePoint(String workloadType, int scenarioId, String algorithm,
                                     int trialNumber, long seed, int iteration, double makespan) {
            this.workloadType = workloadType;
            this.scenarioId = scenarioId;
            this.algorithm = algorithm;
            this.trialNumber = trialNumber;
            this.seed = seed;
            this.iteration = iteration;
            this.makespan = makespan;
        }

        public String toCsvRow() {
            return String.format("%s,%d,%s,%d,%d,%d,%.6f",
                    workloadType, scenarioId, algorithm, trialNumber, seed, iteration, makespan);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================================");
        System.out.println("   Chaos-Enhanced PSO Cloud Scheduling Experimental Pipeline     ");
        System.out.println("=================================================================");

        WorkloadGenerator.Distribution distribution = WorkloadGenerator.Distribution.UNIFORM_100_300;
        for (int i = 0; i < args.length; i++) {
            if ("--distribution".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                String distStr = args[++i].toUpperCase();
                if ("CONSTANT".equals(distStr) || "CONSTANT_300".equals(distStr)) {
                    distribution = WorkloadGenerator.Distribution.CONSTANT_300;
                } else {
                    distribution = WorkloadGenerator.Distribution.UNIFORM_100_300;
                }
            }
        }
        System.out.println("Workload Distribution: " + distribution.name());

        Path baseDir = Paths.get("results", "chaos_pso");
        Path rawDir = baseDir.resolve("raw");
        Path summaryDir = baseDir.resolve("summary");
        Path convDir = baseDir.resolve("convergence");
        Path chartsDir = baseDir.resolve("charts");

        Files.createDirectories(rawDir);
        Files.createDirectories(summaryDir);
        Files.createDirectories(convDir);
        Files.createDirectories(chartsDir);

        int vmCount = DEFAULT_VM_COUNT;
        WorkloadGenerator generator = new WorkloadGenerator();
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        CloudSimScheduleAdapter cloudSimAdapter = new CloudSimScheduleAdapter();
        cloudSimAdapter.setVerbose(false);

        List<WorkloadGenerator.WorkloadType> workloads = List.of(
                WorkloadGenerator.WorkloadType.NORMAL,
                WorkloadGenerator.WorkloadType.DDOS
        );

        List<ScenarioConfiguration> scenarios = PaperScenarios.all();
        List<ExperimentRecord> allRecords = new ArrayList<>();
        List<TrialConvergencePoint> allTrialConvPoints = new ArrayList<>();

        // Convergence sums: Key = "workload|scenarioId|algorithm" -> array of sums
        Map<String, double[]> convergenceSums = new LinkedHashMap<>();
        // Inertia sums: Key = "algorithm|scenarioId"
        Map<String, double[]> inertiaSums = new LinkedHashMap<>();

        System.out.printf("Total experiment trials to execute: %d scenarios x %d workloads x 3 algorithms x 10 trials = 360 runs%n%n",
                scenarios.size(), workloads.size());

        long startTime = System.currentTimeMillis();
        int completedRuns = 0;
        int failedRuns = 0;

        for (WorkloadGenerator.WorkloadType wlType : workloads) {
            for (ScenarioConfiguration scenario : scenarios) {
                int scId = scenario.getScenarioId();
                int taskCount = scenario.getTaskCount();
                int iters = scenario.getIterations();
                int trials = scenario.getTrials();

                System.out.printf(">>> Executing Scenario %d [%s Workload] | Tasks: %d, Iterations: %d, Trials: %d%n",
                        scId, wlType.name(), taskCount, iters, trials);

                for (int trial = 0; trial < trials; trial++) {
                    long seed = DEFAULT_BASE_SEED + trial;

                    // Generate matched workload for this trial
                    List<WorkloadTask> workload = generator.generateWorkload(wlType, taskCount, seed, WorkloadGenerator.DEFAULT_DDOS_ATTACK_RATIO, distribution);
                    List<Task> tasks = generator.toTasks(workload);

                    // 1. Baseline Canonical PSO (Instrumented for telemetry)
                    InstrumentedBaselinePsoScheduler baselinePso = new InstrumentedBaselinePsoScheduler();
                    runTrial("Baseline-PSO", baselinePso, tasks, workload, scenario, trial, seed, vmCount,
                            wlType, evaluator, cloudSimAdapter, allRecords, allTrialConvPoints, convergenceSums, null);

                    // 2. LDIW-PSO
                    LdiwParticleSwarmScheduler ldiwPso = new LdiwParticleSwarmScheduler();
                    runTrial("LDIW-PSO", ldiwPso, tasks, workload, scenario, trial, seed, vmCount,
                            wlType, evaluator, cloudSimAdapter, allRecords, allTrialConvPoints, convergenceSums, inertiaSums);

                    // 3. Chaos-PSO
                    ChaosParticleSwarmScheduler chaosPso = new ChaosParticleSwarmScheduler();
                    runTrial("Chaos-PSO", chaosPso, tasks, workload, scenario, trial, seed, vmCount,
                            wlType, evaluator, cloudSimAdapter, allRecords, allTrialConvPoints, convergenceSums, inertiaSums);

                    completedRuns += 3;
                }
            }
        }

        for (ExperimentRecord rec : allRecords) {
            if ("FAILURE".equals(rec.status)) {
                failedRuns++;
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("%nExecution completed in %.2f seconds.%n", totalTime / 1000.0);
        System.out.printf("Completed runs: %d / 360 | Failed runs: %d%n", completedRuns, failedRuns);

        // 1. Save raw results CSV
        Path rawCsv = rawDir.resolve("chaos_pso_raw_results.csv");
        writeRawCsv(rawCsv, allRecords);
        System.out.println("Saved raw results to: " + rawCsv.toAbsolutePath());

        // 2. Save individual trial convergence CSV
        Path trialConvCsv = convDir.resolve("individual_trial_convergence.csv");
        writeTrialConvCsv(trialConvCsv, allTrialConvPoints);
        System.out.println("Saved individual trial convergence to: " + trialConvCsv.toAbsolutePath());

        // 3. Save aggregated mean convergence CSV
        Path meanConvCsv = convDir.resolve("mean_convergence_trajectories.csv");
        writeMeanConvergenceCsv(meanConvCsv, convergenceSums, scenarios, workloads);
        System.out.println("Saved mean convergence trajectories to: " + meanConvCsv.toAbsolutePath());

        // 4. Save summary comparison CSV & print summary table
        Path summaryCsv = summaryDir.resolve("chaos_pso_comparison_summary.csv");
        writeSummaryCsv(summaryCsv, allRecords, scenarios, workloads);
        System.out.println("Saved summary statistics to: " + summaryCsv.toAbsolutePath());
    }

    private static void runTrial(
            String algoName,
            SchedulingAlgorithm scheduler,
            List<Task> tasks,
            List<WorkloadTask> workload,
            ScenarioConfiguration scenario,
            int trial,
            long seed,
            int vmCount,
            WorkloadGenerator.WorkloadType wlType,
            FitnessEvaluator evaluator,
            CloudSimScheduleAdapter cloudSimAdapter,
            List<ExperimentRecord> allRecords,
            List<TrialConvergencePoint> allTrialConvPoints,
            Map<String, double[]> convergenceSums,
            Map<String, double[]> inertiaSums) {

        double analyticalMakespan = -1.0;
        double analyticalRU = -1.0;
        double csMakespan = -1.0;
        double csRU = -1.0;
        long execTimeMs = 0;
        String status = "SUCCESS";
        String errorDetails = "";

        try {
            long startNs = System.nanoTime();
            Schedule schedule = scheduler.solve(tasks, vmCount, scenario.getIterations(), seed);
            long endNs = System.nanoTime();
            execTimeMs = (endNs - startNs) / 1_000_000L;

            FitnessEvaluator.Result evalRes = evaluator.evaluate(tasks, schedule);
            analyticalMakespan = evalRes.getMakespan();
            analyticalRU = evalRes.getResourceUtilization();

            // CloudSim verification
            CloudSimScheduleAdapter.Result csRes = cloudSimAdapter.executeWorkload(workload, schedule, vmCount);
            csMakespan = csRes.getObservedMakespan();
            csRU = csRes.getResourceUtilization();

            for (CloudSimScheduleAdapter.Execution ex : csRes.getExecutions()) {
                if (ex.getStatus() != Cloudlet.SUCCESS) {
                    status = "WARNING";
                    errorDetails = "Cloudlet " + ex.getCloudletId() + " status=" + ex.getStatus();
                    break;
                }
            }
        } catch (Exception ex) {
            status = "FAILURE";
            errorDetails = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            System.err.printf("ERROR in %s Scenario %d Trial %d [%s]: %s%n",
                    algoName, scenario.getScenarioId(), trial + 1, wlType.name(), errorDetails);
        }

        allRecords.add(new ExperimentRecord(
                algoName, scenario.getScenarioId(), wlType.name(), scenario.getTaskCount(),
                vmCount, scenario.getIterations(), trial + 1, seed,
                analyticalMakespan, analyticalRU, csMakespan, csRU, execTimeMs, status, errorDetails
        ));

        // Extract convergence telemetry
        double[] conv = null;
        if (scheduler instanceof InstrumentedBaselinePsoScheduler) {
            conv = ((InstrumentedBaselinePsoScheduler) scheduler).getConvergenceHistory();
        } else if (scheduler instanceof LdiwParticleSwarmScheduler) {
            conv = ((LdiwParticleSwarmScheduler) scheduler).getConvergenceHistory();
            if (inertiaSums != null) {
                accumulateInertia(inertiaSums, algoName, scenario.getScenarioId(),
                        ((LdiwParticleSwarmScheduler) scheduler).getInertiaHistory());
            }
        } else if (scheduler instanceof ChaosParticleSwarmScheduler) {
            conv = ((ChaosParticleSwarmScheduler) scheduler).getConvergenceHistory();
            if (inertiaSums != null) {
                accumulateInertia(inertiaSums, algoName, scenario.getScenarioId(),
                        ((ChaosParticleSwarmScheduler) scheduler).getInertiaHistory());
            }
        }

        if (conv != null) {
            // Record individual trial convergence points
            for (int iter = 0; iter < conv.length; iter++) {
                allTrialConvPoints.add(new TrialConvergencePoint(
                        wlType.name(), scenario.getScenarioId(), algoName, trial + 1, seed, iter, conv[iter]
                ));
            }

            // Accumulate for mean trajectory
            final double[] finalConv = conv;
            String convKey = String.format("%s|%d|%s", wlType.name(), scenario.getScenarioId(), algoName);
            convergenceSums.compute(convKey, (k, existing) -> {
                if (existing == null) {
                    return finalConv.clone();
                }
                for (int i = 0; i < existing.length && i < finalConv.length; i++) {
                    existing[i] += finalConv[i];
                }
                return existing;
            });
        }
    }

    private static void accumulateInertia(Map<String, double[]> inertiaSums, String algo, int scId, double[] history) {
        String key = algo + "|" + scId;
        inertiaSums.compute(key, (k, existing) -> {
            if (existing == null) return history.clone();
            for (int i = 0; i < existing.length && i < history.length; i++) {
                existing[i] += history[i];
            }
            return existing;
        });
    }

    private static void writeRawCsv(Path outPath, List<ExperimentRecord> records) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            bw.write("algorithm,scenario_id,workload_type,task_count,vm_count,iterations,trial_number,seed,analytical_makespan,analytical_resource_utilization,cloudsim_makespan,cloudsim_resource_utilization,execution_time_ms,status,error_details\n");
            for (ExperimentRecord r : records) {
                bw.write(r.toCsvRow());
                bw.write("\n");
            }
        }
    }

    private static void writeTrialConvCsv(Path outPath, List<TrialConvergencePoint> points) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            bw.write("workload_type,scenario_id,algorithm,trial_number,seed,iteration,makespan\n");
            for (TrialConvergencePoint p : points) {
                bw.write(p.toCsvRow());
                bw.write("\n");
            }
        }
    }

    private static void writeMeanConvergenceCsv(
            Path outPath,
            Map<String, double[]> convSums,
            List<ScenarioConfiguration> scenarios,
            List<WorkloadGenerator.WorkloadType> workloads) throws IOException {

        try (BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            bw.write("workload_type,scenario_id,algorithm,iteration,mean_makespan\n");
            for (WorkloadGenerator.WorkloadType wl : workloads) {
                for (ScenarioConfiguration sc : scenarios) {
                    for (String algo : List.of("Baseline-PSO", "LDIW-PSO", "Chaos-PSO")) {
                        String key = String.format("%s|%d|%s", wl.name(), sc.getScenarioId(), algo);
                        double[] sums = convSums.get(key);
                        if (sums != null) {
                            for (int iter = 0; iter < sums.length; iter++) {
                                double mean = sums[iter] / 10.0;
                                bw.write(String.format("%s,%d,%s,%d,%.6f\n",
                                        wl.name(), sc.getScenarioId(), algo, iter, mean));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void writeSummaryCsv(
            Path outPath,
            List<ExperimentRecord> records,
            List<ScenarioConfiguration> scenarios,
            List<WorkloadGenerator.WorkloadType> workloads) throws IOException {

        List<String> algos = List.of("Baseline-PSO", "LDIW-PSO", "Chaos-PSO");

        System.out.println("\n==========================================================================================================");
        System.out.println("                                EXPERIMENTAL SUMMARY & COMPARISON                                         ");
        System.out.println("==========================================================================================================");
        System.out.printf("%-4s | %-6s | %-4s | %-6s | %-12s | %-12s | %-12s | %-12s | %-12s%n",
                "Scen", "Tasks", "Iter", "Type", "Base-PSO (s)", "LDIW-PSO (s)", "Chaos-PSO (s)", "Imp/Base (%)", "Imp/LDIW (%)");
        System.out.println("----------------------------------------------------------------------------------------------------------");

        try (BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            bw.write("scenario_id,workload_type,task_count,vm_count,iterations,base_pso_mean,base_pso_median,base_pso_std,ldiw_pso_mean,ldiw_pso_median,ldiw_pso_std,chaos_pso_mean,chaos_pso_median,chaos_pso_std,chaos_pso_min,chaos_pso_max,base_pso_ru,chaos_pso_ru,mean_runtime_ms,improvement_over_base_pct,improvement_over_ldiw_pct\n");

            for (WorkloadGenerator.WorkloadType wl : workloads) {
                for (ScenarioConfiguration sc : scenarios) {
                    Map<String, List<Double>> algoMakespans = new LinkedHashMap<>();
                    Map<String, List<Double>> algoRUs = new LinkedHashMap<>();
                    Map<String, List<Long>> algoRuntimes = new LinkedHashMap<>();

                    for (String a : algos) {
                        algoMakespans.put(a, new ArrayList<>());
                        algoRUs.put(a, new ArrayList<>());
                        algoRuntimes.put(a, new ArrayList<>());
                    }

                    for (ExperimentRecord r : records) {
                        if (r.scenarioId == sc.getScenarioId() && r.workloadType.equals(wl.name())) {
                            algoMakespans.get(r.algorithm).add(r.analyticalMakespan);
                            algoRUs.get(r.algorithm).add(r.analyticalResourceUtilization);
                            algoRuntimes.get(r.algorithm).add(r.executionTimeMs);
                        }
                    }

                    double baseMean = mean(algoMakespans.get("Baseline-PSO"));
                    double baseMedian = median(algoMakespans.get("Baseline-PSO"));
                    double baseStd = populationStd(algoMakespans.get("Baseline-PSO"), baseMean);
                    double baseRu = mean(algoRUs.get("Baseline-PSO"));

                    double ldiwMean = mean(algoMakespans.get("LDIW-PSO"));
                    double ldiwMedian = median(algoMakespans.get("LDIW-PSO"));
                    double ldiwStd = populationStd(algoMakespans.get("LDIW-PSO"), ldiwMean);

                    double chaosMean = mean(algoMakespans.get("Chaos-PSO"));
                    double chaosMedian = median(algoMakespans.get("Chaos-PSO"));
                    double chaosStd = populationStd(algoMakespans.get("Chaos-PSO"), chaosMean);
                    double chaosRu = mean(algoRUs.get("Chaos-PSO"));
                    double chaosMin = min(algoMakespans.get("Chaos-PSO"));
                    double chaosMax = max(algoMakespans.get("Chaos-PSO"));
                    double meanRuntime = meanLong(algoRuntimes.get("Chaos-PSO"));

                    double impOverBase = ((baseMean - chaosMean) / baseMean) * 100.0;
                    double impOverLdiw = ((ldiwMean - chaosMean) / ldiwMean) * 100.0;

                    bw.write(String.format("%d,%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f,%.1f,%.2f,%.2f\n",
                            sc.getScenarioId(), wl.name(), sc.getTaskCount(), sc.getTrials(), sc.getIterations(),
                            baseMean, baseMedian, baseStd, ldiwMean, ldiwMedian, ldiwStd,
                            chaosMean, chaosMedian, chaosStd,
                            chaosMin, chaosMax, baseRu, chaosRu, meanRuntime, impOverBase, impOverLdiw));

                    System.out.printf("S%-3d | %-6d | %-4d | %-6s | %12.4f | %12.4f | %12.4f | %+11.2f%% | %+11.2f%%%n",
                            sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(), wl.name(),
                            baseMean, ldiwMean, chaosMean, impOverBase, impOverLdiw);
                }
            }
        }
        System.out.println("==========================================================================================================");
    }

    private static double mean(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : vals) sum += v;
        return sum / vals.size();
    }

    private static double median(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(vals);
        java.util.Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        } else {
            return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        }
    }

    private static double meanLong(List<Long> vals) {
        if (vals.isEmpty()) return 0.0;
        double sum = 0.0;
        for (long v : vals) sum += v;
        return sum / vals.size();
    }

    private static double populationStd(List<Double> vals, double mean) {
        if (vals.isEmpty()) return 0.0;
        double sumSq = 0.0;
        for (double v : vals) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / vals.size());
    }

    private static double min(List<Double> vals) {
        double m = Double.POSITIVE_INFINITY;
        for (double v : vals) if (v < m) m = v;
        return m;
    }

    private static double max(List<Double> vals) {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : vals) if (v > m) m = v;
        return m;
    }
}
