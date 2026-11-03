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
 * Dedicated Experiment Runner for Opposition-Based Learning (OBL) Enhanced Hybrid GA-PSO.
 *
 * Compares:
 *   1. Hybrid GA-PSO [Baseline reproduction, Kaplan & Babalik 2025]
 *   2. OBL-Hybrid GA-PSO [Proposed research extension]
 *
 * Conducts matched experiments across all 6 paper scenarios, both Normal and DDoS workloads,
 * across 10 independent trials with controlled reproducible seeds (0..9).
 * Total trial runs: 2 algorithms x 6 scenarios x 2 workloads x 10 trials = 240 runs.
 *
 * All outputs saved into: results/obl_hybrid/ (raw/ and summary/)
 */
public class OblHybridExperimentRunner {

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
        Path outputDir = Paths.get("results", "obl_hybrid");
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
        System.out.println("   OBL-Hybrid GA-PSO Research Experiment Evaluation Runner      ");
        System.out.println("=================================================================");
        System.out.println("Algorithms        : Hybrid-GA-PSO (Baseline) vs OBL-Hybrid-GA-PSO (Proposed)");
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

                    // 2. Run Proposed OBL-Hybrid GA-PSO
                    runAlgorithm("OBL-Hybrid-GA-PSO", new OblHybridGaPsoScheduler(), tasks, workload, scenario,
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
        Path rawCsv = rawDir.resolve("raw_obl_hybrid_results.csv");
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
                analyticalMakespan, analyticalRU, csMakespan, csRU, execTimeMs, status, errorDetails));
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

    private static void writeSummaryCsvAndReport(
            Path summaryDir,
            List<ExperimentRecord> records,
            List<ScenarioConfiguration> scenarios,
            List<WorkloadGenerator.WorkloadType> workloads) throws IOException {

        Path summaryCsv = summaryDir.resolve("obl_hybrid_comparison_summary.csv");
        Path summaryMd = summaryDir.resolve("OBL_HYBRID_EXPERIMENTAL_ANALYSIS.md");

        // Aggregation structure: Key = workload|scenario|algo -> list of values
        Map<String, List<Double>> makespanMap = new LinkedHashMap<String, List<Double>>();
        Map<String, List<Double>> ruMap = new LinkedHashMap<String, List<Double>>();
        Map<String, List<Long>> timeMap = new LinkedHashMap<String, List<Long>>();

        for (ExperimentRecord r : records) {
            String key = String.format("%s|%d|%s", r.workloadType, r.scenarioId, r.algorithm);
            makespanMap.computeIfAbsent(key, k -> new ArrayList<Double>()).add(r.analyticalMakespan);
            ruMap.computeIfAbsent(key, k -> new ArrayList<Double>()).add(r.analyticalResourceUtilization);
            timeMap.computeIfAbsent(key, k -> new ArrayList<Long>()).add(r.executionTimeMs);
        }

        StringBuilder md = new StringBuilder();
        md.append("# Experimental Evaluation: OBL-Hybrid GA-PSO vs. Baseline Hybrid GA-PSO\n\n");
        md.append("Empirical comparison across 6 scenarios, Normal and DDoS workloads (10 trials each, 240 runs total).\n\n");

        try (BufferedWriter bw = Files.newBufferedWriter(summaryCsv, StandardCharsets.UTF_8)) {
            bw.write("workload,scenario_id,task_count,iterations,baseline_hybrid_mean_ms,baseline_hybrid_best_ms,baseline_hybrid_worst_ms,baseline_hybrid_std_ms,obl_hybrid_mean_ms,obl_hybrid_best_ms,obl_hybrid_worst_ms,obl_hybrid_std_ms,makespan_reduction_s,improvement_pct,baseline_ru,obl_hybrid_ru,baseline_time_ms,obl_hybrid_time_ms\n");

            for (WorkloadGenerator.WorkloadType wl : workloads) {
                md.append("## ").append(wl.name()).append(" Workload Evaluation\n\n");
                md.append("| Scenario | Tasks | Iters | Metric | Baseline Hybrid GA-PSO | OBL-Hybrid GA-PSO | Delta | Impr (%) |\n");
                md.append("|---|---|---|---|---|---|---|---|\n");

                for (ScenarioConfiguration sc : scenarios) {
                    String baseKey = String.format("%s|%d|Hybrid-GA-PSO", wl.name(), sc.getScenarioId());
                    String oblKey = String.format("%s|%d|OBL-Hybrid-GA-PSO", wl.name(), sc.getScenarioId());

                    List<Double> baseMakespans = makespanMap.getOrDefault(baseKey, Collections.emptyList());
                    List<Double> oblMakespans = makespanMap.getOrDefault(oblKey, Collections.emptyList());
                    List<Double> baseRUs = ruMap.getOrDefault(baseKey, Collections.emptyList());
                    List<Double> oblRUs = ruMap.getOrDefault(oblKey, Collections.emptyList());
                    List<Long> baseTimes = timeMap.getOrDefault(baseKey, Collections.emptyList());
                    List<Long> oblTimes = timeMap.getOrDefault(oblKey, Collections.emptyList());

                    Stats baseStats = computeStats(baseMakespans);
                    Stats oblStats = computeStats(oblMakespans);

                    double reduction = baseStats.mean - oblStats.mean;
                    double imprPct = (reduction / baseStats.mean) * 100.0;

                    double meanBaseRu = mean(baseRUs);
                    double meanOblRu = mean(oblRUs);

                    double meanBaseTime = meanL(baseTimes);
                    double meanOblTime = meanL(oblTimes);

                    bw.write(String.format("%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.4f,%.4f,%.1f,%.1f\n",
                            wl.name(), sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                            baseStats.mean, baseStats.min, baseStats.max, baseStats.std,
                            oblStats.mean, oblStats.min, oblStats.max, oblStats.std,
                            reduction, imprPct,
                            meanBaseRu, meanOblRu, meanBaseTime, meanOblTime));

                    md.append(String.format("| S%d | %d | %d | **Mean Makespan** | %.3f s | **%.3f s** | %+.3f s | **%+.2f%%** |\n",
                            sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                            baseStats.mean, oblStats.mean, reduction, imprPct));
                    md.append(String.format("| | | | Best Makespan | %.3f s | %.3f s | %+.3f s | - |\n",
                            baseStats.min, oblStats.min, baseStats.min - oblStats.min));
                    md.append(String.format("| | | | Worst Makespan | %.3f s | **%.3f s** | %+.3f s | - |\n",
                            baseStats.max, oblStats.max, baseStats.max - oblStats.max));
                    md.append(String.format("| | | | Std. Dev | %.3f s | %.3f s | - | - |\n",
                            baseStats.std, oblStats.std));
                    md.append(String.format("| | | | Resource Util. | %.2f%% | **%.2f%%** | %+.2f%% | - |\n",
                            meanBaseRu, meanOblRu, meanOblRu - meanBaseRu));
                    md.append(String.format("| | | | Host Exec Time | %.1f ms | %.1f ms | - | - |\n",
                            meanBaseTime, meanOblTime));
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
}

