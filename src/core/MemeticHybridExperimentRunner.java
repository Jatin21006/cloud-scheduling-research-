package core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cloudsim.CloudSimScheduleAdapter;
import org.cloudbus.cloudsim.Cloudlet;

/**
 * Dedicated Experiment Runner for Memetic Hybrid GA-PSO research extension.
 *
 * Compares:
 *   1. Hybrid GA-PSO [Baseline reproduction, Kaplan & Babalik 2025]
 *   2. Memetic-Hybrid-GA-PSO [Proposed research extension]
 *
 * Conducts matched experiments across all 6 paper scenarios, both Normal and DDoS workloads,
 * across 10 independent trials with controlled reproducible seeds (0..9).
 * Total trial runs: 2 algorithms x 6 scenarios x 2 workloads x 10 trials = 240 runs.
 *
 * All outputs saved into: results/memetic_hybrid/ (raw/ and summary/)
 */
public class MemeticHybridExperimentRunner {

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
        public final int totalEvaluations;
        public final int localSearchEvaluations;
        public final int localSearchImprovements;
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
                int totalEvaluations,
                int localSearchEvaluations,
                int localSearchImprovements,
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
            this.totalEvaluations = totalEvaluations;
            this.localSearchEvaluations = localSearchEvaluations;
            this.localSearchImprovements = localSearchImprovements;
            this.status = status;
            this.errorDetails = errorDetails;
        }

        public String toCsvRow() {
            return String.format("%s,%d,%s,%d,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%d,%d,%d,%d,%s,%s",
                    algorithm,
                    scenarioId,
                    workloadType,
                    taskCount,
                    vmCount,
                    iterations,
                    trialNumber,
                    seed,
                    analyticalMakespan,
                    analyticalResourceUtilization,
                    cloudSimMakespan,
                    cloudSimResourceUtilization,
                    executionTimeMs,
                    totalEvaluations,
                    localSearchEvaluations,
                    localSearchImprovements,
                    status,
                    escapeCsv(errorDetails));
        }

        private static String escapeCsv(String val) {
            if (val == null || val.isEmpty()) return "";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                return "\"" + val.replace("\"", "\"\"") + "\"";
            }
            return val;
        }
    }

    public static void main(String[] args) throws Exception {
        int filterScenario = -1;
        String filterWorkload = "ALL";
        boolean runCloudSim = true;
        int vmCount = DEFAULT_VM_COUNT;
        Path outputDir = Paths.get("results", "memetic_hybrid");
        WorkloadGenerator.Distribution distribution = WorkloadGenerator.Distribution.UNIFORM_100_300;

        for (int i = 0; i < args.length; i++) {
            if ("--scenario".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                filterScenario = Integer.parseInt(args[++i]);
            } else if ("--workload".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                filterWorkload = args[++i].toUpperCase();
            } else if ("--cloudsim".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                runCloudSim = Boolean.parseBoolean(args[++i]);
            } else if ("--vmCount".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                vmCount = Integer.parseInt(args[++i]);
            } else if ("--outputDir".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                outputDir = Paths.get(args[++i]);
            }
        }

        System.out.println("=================================================================");
        System.out.println("   Memetic Hybrid GA-PSO Research Experiment Evaluation Runner  ");
        System.out.println("=================================================================");
        System.out.println("Algorithms        : Hybrid-GA-PSO (Baseline) vs Memetic-Hybrid-GA-PSO (Proposed)");
        System.out.println("Scenario Filter   : " + (filterScenario == -1 ? "ALL (1-6)" : filterScenario));
        System.out.println("Workload Filter   : " + filterWorkload);
        System.out.println("Distribution      : " + distribution.name() + " [100, 300] MI");
        System.out.println("CloudSim Execution: " + runCloudSim);
        System.out.println("VM Count          : " + vmCount + " (100 MIPS each)");
        System.out.println("Output Directory  : " + outputDir.toAbsolutePath());
        System.out.println("=================================================================\n");

        List<WorkloadGenerator.WorkloadType> workloadTypes = new ArrayList<WorkloadGenerator.WorkloadType>();
        if ("ALL".equals(filterWorkload) || "NORMAL".equals(filterWorkload)) {
            workloadTypes.add(WorkloadGenerator.WorkloadType.NORMAL);
        }
        if ("ALL".equals(filterWorkload) || "DDOS".equals(filterWorkload)) {
            workloadTypes.add(WorkloadGenerator.WorkloadType.DDOS);
        }

        List<ScenarioConfiguration> scenarios = new ArrayList<ScenarioConfiguration>();
        for (ScenarioConfiguration sc : PaperScenarios.all()) {
            if (filterScenario == -1 || sc.getScenarioId() == filterScenario) {
                scenarios.add(sc);
            }
        }

        WorkloadGenerator generator = new WorkloadGenerator();
        FitnessEvaluator evaluator = new FitnessEvaluator(vmCount);
        CloudSimScheduleAdapter cloudSimAdapter = new CloudSimScheduleAdapter();
        cloudSimAdapter.setVerbose(false);

        List<ExperimentRecord> allRecords = new ArrayList<ExperimentRecord>();

        int totalExpectedRuns = scenarios.size() * workloadTypes.size() * 10 * 2;
        System.out.printf("Total experiment executions to run: %d%n%n", totalExpectedRuns);

        long experimentStart = System.currentTimeMillis();
        int completedRuns = 0;

        for (WorkloadGenerator.WorkloadType wlType : workloadTypes) {
            for (ScenarioConfiguration scenario : scenarios) {
                System.out.printf(">>> Executing Scenario %d [%s Workload] | Tasks: %d, Iterations: %d, Trials: %d%n",
                        scenario.getScenarioId(),
                        wlType.name(),
                        scenario.getTaskCount(),
                        scenario.getIterations(),
                        scenario.getTrials());

                for (int trial = 0; trial < scenario.getTrials(); trial++) {
                    long seed = DEFAULT_BASE_SEED + trial;

                    // Generate matched workload once per (scenario, workload, trial)
                    List<WorkloadTask> workload = generator.generateWorkload(
                            wlType,
                            scenario.getTaskCount(),
                            seed,
                            WorkloadGenerator.DEFAULT_DDOS_ATTACK_RATIO,
                            distribution);
                    List<Task> tasks = generator.toTasks(workload);

                    // 1. Run Baseline Hybrid GA-PSO
                    runAlgorithm("Hybrid-GA-PSO", new HybridGaPsoScheduler(), tasks, workload, scenario,
                            wlType, trial, seed, vmCount, evaluator, cloudSimAdapter, runCloudSim, allRecords);
                    completedRuns++;

                    // 2. Run Proposed Memetic-Hybrid-GA-PSO
                    runAlgorithm("Memetic-Hybrid-GA-PSO", new MemeticHybridGaPsoScheduler(), tasks, workload, scenario,
                            wlType, trial, seed, vmCount, evaluator, cloudSimAdapter, runCloudSim, allRecords);
                    completedRuns++;
                }
            }
        }

        long totalTimeMs = System.currentTimeMillis() - experimentStart;
        System.out.printf("%nAll %d experimental runs completed successfully in %.2f seconds!%n",
                completedRuns, totalTimeMs / 1000.0);

        // Create output directories
        Path rawDir = outputDir.resolve("raw");
        Path summaryDir = outputDir.resolve("summary");
        Files.createDirectories(rawDir);
        Files.createDirectories(summaryDir);

        // Write raw CSV
        Path rawCsv = rawDir.resolve("raw_memetic_hybrid_results.csv");
        writeRawCsv(rawCsv, allRecords);
        System.out.println("Raw results written to: " + rawCsv.toAbsolutePath());

        // Generate summary analysis
        writeSummaryCsvAndReport(summaryDir, allRecords, scenarios, workloadTypes);
    }

    private static void runAlgorithm(
            String algoName,
            SchedulingAlgorithm scheduler,
            List<Task> tasks,
            List<WorkloadTask> workload,
            ScenarioConfiguration scenario,
            WorkloadGenerator.WorkloadType wlType,
            int trial,
            long seed,
            int vmCount,
            FitnessEvaluator evaluator,
            CloudSimScheduleAdapter cloudSimAdapter,
            boolean runCloudSim,
            List<ExperimentRecord> allRecords) {

        double analyticalMakespan = -1.0;
        double analyticalRU = -1.0;
        double csMakespan = -1.0;
        double csRU = -1.0;
        long execTimeMs = 0;
        int totalEvals = 0;
        int localEvals = 0;
        int localImprs = 0;
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

            if (scheduler instanceof MemeticHybridGaPsoScheduler) {
                MemeticHybridGaPsoScheduler mh = (MemeticHybridGaPsoScheduler) scheduler;
                totalEvals = mh.getTotalEvaluationsCount();
                localEvals = mh.getLocalSearchEvaluationsCount();
                localImprs = mh.getLocalSearchImprovementsCount();
            } else {
                int hybridIters = Math.max(1, scenario.getIterations() / 2);
                totalEvals = 75 + 75 + hybridIters * (75 + 75);
            }

            if (runCloudSim) {
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
                analyticalMakespan, analyticalRU, csMakespan, csRU, execTimeMs,
                totalEvals, localEvals, localImprs, status, errorDetails));
    }

    private static void writeRawCsv(Path outPath, List<ExperimentRecord> records) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            bw.write("algorithm,scenario_id,workload_type,task_count,vm_count,iterations,trial_number,seed,analytical_makespan,analytical_resource_utilization,cloudsim_makespan,cloudsim_resource_utilization,execution_time_ms,total_evaluations,local_search_evaluations,local_search_improvements,status,error_details\n");
            for (ExperimentRecord r : records) {
                bw.write(r.toCsvRow());
                bw.write("\n");
            }
        }
    }

    private static void writeSummaryCsvAndReport(
            Path summaryDir,
            List<ExperimentRecord> records,
            List<ScenarioConfiguration> scenarios,
            List<WorkloadGenerator.WorkloadType> workloads) throws IOException {

        Path summaryCsv = summaryDir.resolve("memetic_hybrid_comparison_summary.csv");
        Path summaryMd = summaryDir.resolve("MEMETIC_HYBRID_EXPERIMENTAL_ANALYSIS.md");

        Map<String, List<Double>> makespanMap = new LinkedHashMap<String, List<Double>>();
        Map<String, List<Double>> ruMap = new LinkedHashMap<String, List<Double>>();
        Map<String, List<Long>> timeMap = new LinkedHashMap<String, List<Long>>();
        Map<String, List<Integer>> localEvalsMap = new LinkedHashMap<String, List<Integer>>();
        Map<String, List<Integer>> totalEvalsMap = new LinkedHashMap<String, List<Integer>>();

        for (ExperimentRecord r : records) {
            String key = String.format("%s|%d|%s", r.workloadType, r.scenarioId, r.algorithm);
            makespanMap.computeIfAbsent(key, k -> new ArrayList<Double>()).add(r.analyticalMakespan);
            ruMap.computeIfAbsent(key, k -> new ArrayList<Double>()).add(r.analyticalResourceUtilization);
            timeMap.computeIfAbsent(key, k -> new ArrayList<Long>()).add(r.executionTimeMs);
            localEvalsMap.computeIfAbsent(key, k -> new ArrayList<Integer>()).add(r.localSearchEvaluations);
            totalEvalsMap.computeIfAbsent(key, k -> new ArrayList<Integer>()).add(r.totalEvaluations);
        }

        StringBuilder md = new StringBuilder();
        md.append("# Experimental Evaluation: Memetic Hybrid GA-PSO vs. Baseline Hybrid GA-PSO\n\n");
        md.append("Empirical comparison across 6 scenarios, Normal and DDoS workloads (10 trials each, 240 runs total).\n\n");

        try (BufferedWriter bw = Files.newBufferedWriter(summaryCsv, StandardCharsets.UTF_8)) {
            bw.write("workload,scenario_id,task_count,iterations,baseline_hybrid_mean_ms,baseline_hybrid_best_ms,baseline_hybrid_worst_ms,baseline_hybrid_std_ms,memetic_hybrid_mean_ms,memetic_hybrid_best_ms,memetic_hybrid_worst_ms,memetic_hybrid_std_ms,makespan_reduction_s,improvement_pct,baseline_ru,memetic_hybrid_ru,baseline_time_ms,memetic_hybrid_time_ms,memetic_local_evals,memetic_total_evals\n");

            for (WorkloadGenerator.WorkloadType wl : workloads) {
                md.append("## ").append(wl.name()).append(" Workload Evaluation\n\n");
                md.append("| Scenario | Tasks | Iters | Metric | Baseline Hybrid GA-PSO | Memetic-Hybrid-GA-PSO | Delta | Impr (%) |\n");
                md.append("|---|---|---|---|---|---|---|---|\n");

                for (ScenarioConfiguration sc : scenarios) {
                    String baseKey = String.format("%s|%d|Hybrid-GA-PSO", wl.name(), sc.getScenarioId());
                    String memKey = String.format("%s|%d|Memetic-Hybrid-GA-PSO", wl.name(), sc.getScenarioId());

                    List<Double> baseMakespans = makespanMap.getOrDefault(baseKey, Collections.emptyList());
                    List<Double> memMakespans = makespanMap.getOrDefault(memKey, Collections.emptyList());
                    List<Double> baseRUs = ruMap.getOrDefault(baseKey, Collections.emptyList());
                    List<Double> memRUs = ruMap.getOrDefault(memKey, Collections.emptyList());
                    List<Long> baseTimes = timeMap.getOrDefault(baseKey, Collections.emptyList());
                    List<Long> memTimes = timeMap.getOrDefault(memKey, Collections.emptyList());
                    List<Integer> memLocalEvals = localEvalsMap.getOrDefault(memKey, Collections.emptyList());
                    List<Integer> memTotalEvals = totalEvalsMap.getOrDefault(memKey, Collections.emptyList());

                    Stats baseStats = computeStats(baseMakespans);
                    Stats memStats = computeStats(memMakespans);

                    double reduction = baseStats.mean - memStats.mean;
                    double imprPct = (reduction / baseStats.mean) * 100.0;

                    double meanBaseRu = mean(baseRUs);
                    double meanMemRu = mean(memRUs);

                    double meanBaseTime = meanL(baseTimes);
                    double meanMemTime = meanL(memTimes);
                    double meanLocalEvals = meanI(memLocalEvals);
                    double meanTotalEvals = meanI(memTotalEvals);

                    bw.write(String.format("%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.4f,%.4f,%.1f,%.1f,%.1f,%.1f\n",
                            wl.name(), sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                            baseStats.mean, baseStats.min, baseStats.max, baseStats.std,
                            memStats.mean, memStats.min, memStats.max, memStats.std,
                            reduction, imprPct,
                            meanBaseRu, meanMemRu, meanBaseTime, meanMemTime, meanLocalEvals, meanTotalEvals));

                    md.append(String.format("| S%d | %d | %d | **Mean Makespan** | %.3f s | **%.3f s** | %+.3f s | **%+.2f%%** |\n",
                            sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                            baseStats.mean, memStats.mean, reduction, imprPct));
                    md.append(String.format("| | | | Best Makespan | %.3f s | **%.3f s** | %+.3f s | - |\n",
                            baseStats.min, memStats.min, baseStats.min - memStats.min));
                    md.append(String.format("| | | | Worst Makespan | %.3f s | **%.3f s** | %+.3f s | - |\n",
                            baseStats.max, memStats.max, baseStats.max - memStats.max));
                    md.append(String.format("| | | | Std. Dev | %.3f s | %.3f s | - | - |\n",
                            baseStats.std, memStats.std));
                    md.append(String.format("| | | | Resource Util. | %.2f%% | **%.2f%%** | %+.2f%% | - |\n",
                            meanBaseRu, meanMemRu, meanMemRu - meanBaseRu));
                    md.append(String.format("| | | | Host Exec Time | %.1f ms | %.1f ms | - | - |\n",
                            meanBaseTime, meanMemTime));
                    md.append(String.format("| | | | Local Search Evals | 0 | %.0f evals | - | - |\n",
                            meanLocalEvals));
                }
                md.append("\n");
            }
        }

        Files.writeString(summaryMd, md.toString(), StandardCharsets.UTF_8);
        System.out.println("Summary CSV written to: " + summaryCsv.toAbsolutePath());
        System.out.println("Summary Analysis MD written to: " + summaryMd.toAbsolutePath());
    }

    private static class Stats {
        public final double mean;
        public final double min;
        public final double max;
        public final double std;

        public Stats(double mean, double min, double max, double std) {
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.std = std;
        }
    }

    private static Stats computeStats(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new Stats(0.0, 0.0, 0.0, 0.0);
        }
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double mean = sum / values.size();
        double varianceSum = 0.0;
        for (double v : values) {
            varianceSum += Math.pow(v - mean, 2);
        }
        double std = Math.sqrt(varianceSum / values.size());
        return new Stats(mean, min, max, std);
    }

    private static double mean(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    private static double meanL(List<Long> values) {
        if (values == null || values.isEmpty()) return 0.0;
        long sum = 0L;
        for (long v : values) sum += v;
        return (double) sum / values.size();
    }

    private static double meanI(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0.0;
        int sum = 0;
        for (int v : values) sum += v;
        return (double) sum / values.size();
    }
}

