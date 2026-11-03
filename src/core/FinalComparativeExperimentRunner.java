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
 * Final Unified Comparative Experiment Runner for Cloud Task Scheduling Research.
 *
 * Compares:
 *   1. Hybrid-GA-PSO (Original Baseline - Kaplan & Babalik 2025)
 *   2. SA-Hybrid-GA-PSO (Simulated Annealing Enhancement)
 *   3. OBL-Hybrid-GA-PSO (Opposition-Based Learning Enhancement)
 *   4. Memetic-Hybrid-GA-PSO (Local Search Hill-Climbing Enhancement)
 *   5. Chaos-PSO (Chaos-Enhanced Inertia Modulation via Logistic Map)
 *
 * Evaluates across:
 *   - 6 Scenarios (S1-S6: 50 to 100 tasks, 10 to 100 iterations)
 *   - 2 Workload Types (Normal: U[100, 300] MI; DDoS: 100x scaled flood U[10000, 30000] MI)
 *   - 10 Independent Trials (Seeds 0..9) with matched task workloads
 *
 * Total Runs: 5 algorithms x 6 scenarios x 2 workloads x 10 trials = 600 runs.
 * Output Directory: results/final_comparison/ (raw/ and summary/)
 */
public class FinalComparativeExperimentRunner {

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
        Path outputDir = Paths.get("results", "final_comparison");
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

        System.out.println("============================================================================");
        System.out.println("   Final Comparative Research Experiment Runner: 5 Cloud Schedulers        ");
        System.out.println("============================================================================");
        System.out.println("Algorithms        : Hybrid-GA-PSO, SA-Hybrid, OBL-Hybrid, Memetic-Hybrid, Chaos-PSO");
        System.out.println("Scenarios         : " + (filterScenario == -1 ? "ALL (S1-S6)" : ("S" + filterScenario)));
        System.out.println("Workload Filter   : " + filterWorkload);
        System.out.println("Task Distribution : " + distribution.name() + " [100, 300] MI");
        System.out.println("CloudSim Execution: " + runCloudSim);
        System.out.println("VM Count          : " + vmCount + " (100 MIPS each)");
        System.out.println("Output Directory  : " + outputDir.toAbsolutePath());
        System.out.println("============================================================================\n");

        Map<String, SchedulingAlgorithm> algorithmMap = new LinkedHashMap<String, SchedulingAlgorithm>();
        algorithmMap.put("Hybrid-GA-PSO", new HybridGaPsoScheduler());
        algorithmMap.put("SA-Hybrid-GA-PSO", new SimulatedAnnealingHybridGaPsoScheduler());
        algorithmMap.put("OBL-Hybrid-GA-PSO", new OblHybridGaPsoScheduler());
        algorithmMap.put("Memetic-Hybrid-GA-PSO", new MemeticHybridGaPsoScheduler());
        algorithmMap.put("Chaos-PSO", new ChaosParticleSwarmScheduler());

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

        int totalExpectedRuns = scenarios.size() * workloadTypes.size() * 10 * algorithmMap.size();
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

                    // Execute each of the 5 algorithms on identical task inputs
                    for (Map.Entry<String, SchedulingAlgorithm> entry : algorithmMap.entrySet()) {
                        String algoName = entry.getKey();
                        SchedulingAlgorithm scheduler = entry.getValue();

                        runAlgorithm(algoName, scheduler, tasks, workload, scenario,
                                wlType, trial, seed, vmCount, evaluator, cloudSimAdapter, runCloudSim, allRecords);
                        completedRuns++;
                    }
                }
            }
        }

        long totalTimeMs = System.currentTimeMillis() - experimentStart;
        System.out.printf("%nAll %d comparative experimental runs completed successfully in %.2f seconds!%n",
                completedRuns, totalTimeMs / 1000.0);

        // Create output directories
        Path rawDir = outputDir.resolve("raw");
        Path summaryDir = outputDir.resolve("summary");
        Files.createDirectories(rawDir);
        Files.createDirectories(summaryDir);

        // Write raw CSV
        Path rawCsv = rawDir.resolve("raw_final_comparative_results.csv");
        writeRawCsv(rawCsv, allRecords);
        System.out.println("Raw results written to: " + rawCsv.toAbsolutePath());

        // Generate comprehensive summary CSVs and Markdown
        generateSummaryFiles(summaryDir, allRecords, scenarios, workloadTypes, algorithmMap.keySet());
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
        double analyticalResourceUtilization = -1.0;
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
            analyticalResourceUtilization = evalRes.getResourceUtilization();

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
                analyticalMakespan, analyticalResourceUtilization, csMakespan, csRU, execTimeMs,
                status, errorDetails));
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

    private static void generateSummaryFiles(
            Path summaryDir,
            List<ExperimentRecord> records,
            List<ScenarioConfiguration> scenarios,
            List<WorkloadGenerator.WorkloadType> workloads,
            Iterable<String> algorithms) throws IOException {

        Path summaryCsv = summaryDir.resolve("summary_statistics.csv");
        Path scenarioWiseCsv = summaryDir.resolve("scenario_wise_comparison.csv");
        Path improvementCsv = summaryDir.resolve("percentage_improvement.csv");
        Path summaryMd = summaryDir.resolve("FINAL_COMPARATIVE_ANALYSIS.md");

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

        // 1. Write summary_statistics.csv
        try (BufferedWriter bw = Files.newBufferedWriter(summaryCsv, StandardCharsets.UTF_8)) {
            bw.write("workload,scenario_id,task_count,iterations,algorithm,mean_makespan_s,min_makespan_s,max_makespan_s,std_pop_s,std_sample_s,mean_ru_pct,mean_time_ms\n");
            for (WorkloadGenerator.WorkloadType wl : workloads) {
                for (ScenarioConfiguration sc : scenarios) {
                    for (String algo : algorithms) {
                        String key = String.format("%s|%d|%s", wl.name(), sc.getScenarioId(), algo);
                        List<Double> msList = makespanMap.getOrDefault(key, Collections.emptyList());
                        List<Double> ruList = ruMap.getOrDefault(key, Collections.emptyList());
                        List<Long> timeList = timeMap.getOrDefault(key, Collections.emptyList());

                        Stats stats = computeStats(msList);
                        double meanRu = mean(ruList);
                        double meanTime = meanL(timeList);

                        bw.write(String.format("%s,%d,%d,%d,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.1f\n",
                                wl.name(), sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                                algo, stats.mean, stats.min, stats.max, stats.stdPop, stats.stdSample,
                                meanRu, meanTime));
                    }
                }
            }
        }

        // 2. Write scenario_wise_comparison.csv
        try (BufferedWriter bw = Files.newBufferedWriter(scenarioWiseCsv, StandardCharsets.UTF_8)) {
            bw.write("workload,scenario_id,task_count,iterations,hybrid_ga_pso_makespan,sa_hybrid_makespan,obl_hybrid_makespan,memetic_hybrid_makespan,chaos_pso_makespan\n");
            for (WorkloadGenerator.WorkloadType wl : workloads) {
                for (ScenarioConfiguration sc : scenarios) {
                    double hMs = mean(makespanMap.get(String.format("%s|%d|Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double saMs = mean(makespanMap.get(String.format("%s|%d|SA-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double oblMs = mean(makespanMap.get(String.format("%s|%d|OBL-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double memMs = mean(makespanMap.get(String.format("%s|%d|Memetic-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double chaosMs = mean(makespanMap.get(String.format("%s|%d|Chaos-PSO", wl.name(), sc.getScenarioId())));

                    bw.write(String.format("%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f\n",
                            wl.name(), sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                            hMs, saMs, oblMs, memMs, chaosMs));
                }
            }
        }

        // 3. Write percentage_improvement.csv (relative to Baseline Hybrid-GA-PSO)
        try (BufferedWriter bw = Files.newBufferedWriter(improvementCsv, StandardCharsets.UTF_8)) {
            bw.write("workload,scenario_id,task_count,iterations,sa_hybrid_impr_pct,obl_hybrid_impr_pct,memetic_hybrid_impr_pct,chaos_pso_impr_pct\n");
            for (WorkloadGenerator.WorkloadType wl : workloads) {
                for (ScenarioConfiguration sc : scenarios) {
                    double hMs = mean(makespanMap.get(String.format("%s|%d|Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double saMs = mean(makespanMap.get(String.format("%s|%d|SA-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double oblMs = mean(makespanMap.get(String.format("%s|%d|OBL-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double memMs = mean(makespanMap.get(String.format("%s|%d|Memetic-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                    double chaosMs = mean(makespanMap.get(String.format("%s|%d|Chaos-PSO", wl.name(), sc.getScenarioId())));

                    double saImpr = ((hMs - saMs) / hMs) * 100.0;
                    double oblImpr = ((hMs - oblMs) / hMs) * 100.0;
                    double memImpr = ((hMs - memMs) / hMs) * 100.0;
                    double chaosImpr = ((hMs - chaosMs) / hMs) * 100.0;

                    bw.write(String.format("%s,%d,%d,%d,%.2f,%.2f,%.2f,%.2f\n",
                            wl.name(), sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                            saImpr, oblImpr, memImpr, chaosImpr));
                }
            }
        }

        // 4. Generate FINAL_COMPARATIVE_ANALYSIS.md
        StringBuilder md = new StringBuilder();
        md.append("# Final Comparative Experimental Analysis: 5 Cloud Schedulers\n\n");
        md.append("Comprehensive matched experimental evaluation comparing Baseline Hybrid GA-PSO against all four research extensions across 600 total executions.\n\n");

        for (WorkloadGenerator.WorkloadType wl : workloads) {
            md.append("## ").append(wl.name()).append(" Workload Evaluation\n\n");
            md.append("| Scenario | Tasks | Iters | Hybrid-GA-PSO (s) | SA-Hybrid (s) | OBL-Hybrid (s) | Memetic-Hybrid (s) | Chaos-PSO (s) |\n");
            md.append("|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|\n");

            for (ScenarioConfiguration sc : scenarios) {
                double hMs = mean(makespanMap.get(String.format("%s|%d|Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                double saMs = mean(makespanMap.get(String.format("%s|%d|SA-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                double oblMs = mean(makespanMap.get(String.format("%s|%d|OBL-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                double memMs = mean(makespanMap.get(String.format("%s|%d|Memetic-Hybrid-GA-PSO", wl.name(), sc.getScenarioId())));
                double chaosMs = mean(makespanMap.get(String.format("%s|%d|Chaos-PSO", wl.name(), sc.getScenarioId())));

                md.append(String.format("| S%d | %d | %d | %.3f | %.3f | %.3f | **%.3f** | %.3f |\n",
                        sc.getScenarioId(), sc.getTaskCount(), sc.getIterations(),
                        hMs, saMs, oblMs, memMs, chaosMs));
            }
            md.append("\n");
        }

        Files.writeString(summaryMd, md.toString(), StandardCharsets.UTF_8);
        System.out.println("Summary statistics CSV written to: " + summaryCsv.toAbsolutePath());
        System.out.println("Scenario comparison CSV written to: " + scenarioWiseCsv.toAbsolutePath());
        System.out.println("Percentage improvement CSV written to: " + improvementCsv.toAbsolutePath());
        System.out.println("Analysis report MD written to: " + summaryMd.toAbsolutePath());
    }

    private static class Stats {
        public final double mean;
        public final double min;
        public final double max;
        public final double stdPop;
        public final double stdSample;

        public Stats(double mean, double min, double max, double stdPop, double stdSample) {
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.stdPop = stdPop;
            this.stdSample = stdSample;
        }
    }

    private static Stats computeStats(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new Stats(0.0, 0.0, 0.0, 0.0, 0.0);
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
        double stdPop = Math.sqrt(varianceSum / values.size());
        double stdSample = values.size() > 1 ? Math.sqrt(varianceSum / (values.size() - 1)) : 0.0;
        return new Stats(mean, min, max, stdPop, stdSample);
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
