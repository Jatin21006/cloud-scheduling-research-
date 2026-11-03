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
 * Unified Experiment Runner for cloud task scheduling research.
 *
 * Runs matched experiments across algorithms under Normal and DDoS workloads.
 * Evaluates both mathematical/analytical makespan and actual CloudSim simulation observed makespan,
 * along with Resource Utilization and host execution time.
 */
public class UnifiedExperimentRunner {

    public static final long DEFAULT_BASE_SEED = 0L;
    public static final int DEFAULT_VM_COUNT = 10; // Table 1: 10 VMs

    public static class Record {
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

        public Record(
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
        String filterAlgorithm = "ALL";
        boolean runCloudSim = true;
        int vmCount = DEFAULT_VM_COUNT;
        Path outputDir = Paths.get("results");
        WorkloadGenerator.Distribution distribution = WorkloadGenerator.Distribution.UNIFORM_100_300;

        for (int i = 0; i < args.length; i++) {
            if ("--scenario".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                filterScenario = Integer.parseInt(args[++i]);
            } else if ("--workload".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                filterWorkload = args[++i].toUpperCase();
            } else if ("--algorithm".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                filterAlgorithm = args[++i].toUpperCase();
            } else if ("--cloudsim".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                runCloudSim = Boolean.parseBoolean(args[++i]);
            } else if ("--vmCount".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                vmCount = Integer.parseInt(args[++i]);
            } else if ("--outputDir".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                outputDir = Paths.get(args[++i]);
            } else if ("--distribution".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                String distStr = args[++i].toUpperCase();
                if ("CONSTANT".equals(distStr) || "CONSTANT_300".equals(distStr)) {
                    distribution = WorkloadGenerator.Distribution.CONSTANT_300;
                } else {
                    distribution = WorkloadGenerator.Distribution.UNIFORM_100_300;
                }
            }
        }

        System.out.println("=================================================");
        System.out.println("   Cloud Scheduling Unified Experiment Runner    ");
        System.out.println("=================================================");
        System.out.println("Scenario Filter   : " + (filterScenario == -1 ? "ALL" : filterScenario));
        System.out.println("Workload Filter   : " + filterWorkload);
        System.out.println("Algorithm Filter  : " + filterAlgorithm);
        System.out.println("Distribution      : " + distribution.name());
        System.out.println("CloudSim Execution: " + runCloudSim);
        System.out.println("VM Count          : " + vmCount);
        System.out.println("Output Directory  : " + outputDir.toAbsolutePath());
        System.out.println("-------------------------------------------------");

        Map<String, SchedulingAlgorithm> algorithms = new LinkedHashMap<String, SchedulingAlgorithm>();
        if ("ALL".equals(filterAlgorithm) || "ALL-EXTENDED".equals(filterAlgorithm) || "GA".equals(filterAlgorithm)) {
            algorithms.put("GA", new GeneticAlgorithmScheduler());
        }
        if ("ALL".equals(filterAlgorithm) || "ALL-EXTENDED".equals(filterAlgorithm) || "ALL-PSO".equals(filterAlgorithm) || "PSO".equals(filterAlgorithm)) {
            algorithms.put("PSO", new ParticleSwarmScheduler());
        }
        if ("ALL-EXTENDED".equals(filterAlgorithm) || "ALL-PSO".equals(filterAlgorithm) || "LDIW".equals(filterAlgorithm) || "LDIW-PSO".equals(filterAlgorithm) || "LDIW_PSO".equals(filterAlgorithm)) {
            algorithms.put("LDIW-PSO", new LdiwParticleSwarmScheduler());
        }
        if ("ALL-EXTENDED".equals(filterAlgorithm) || "ALL-PSO".equals(filterAlgorithm) || "CHAOS".equals(filterAlgorithm) || "CHAOS-PSO".equals(filterAlgorithm) || "CHAOS_PSO".equals(filterAlgorithm)) {
            algorithms.put("Chaos-PSO", new ChaosParticleSwarmScheduler());
        }
        if ("ALL".equals(filterAlgorithm) || "ALL-EXTENDED".equals(filterAlgorithm) || "ABC".equals(filterAlgorithm)) {
            algorithms.put("ABC", new ArtificialBeeColonyScheduler());
        }
        if ("ALL".equals(filterAlgorithm) || "ALL-EXTENDED".equals(filterAlgorithm) || "HYBRID".equals(filterAlgorithm) || "HYBRID-GA-PSO".equals(filterAlgorithm)) {
            algorithms.put("Hybrid-GA-PSO", new HybridGaPsoScheduler());
        }
        if ("ALL-EXTENDED".equals(filterAlgorithm) || "SA-HYBRID".equals(filterAlgorithm) || "SA-HYBRID-GA-PSO".equals(filterAlgorithm) || "SA_HYBRID".equals(filterAlgorithm)) {
            algorithms.put("SA-Hybrid-GA-PSO", new SimulatedAnnealingHybridGaPsoScheduler());
        }

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

        List<Record> allRecords = new ArrayList<Record>();
        int totalExpectedRuns = scenarios.size() * workloadTypes.size() * 10 * algorithms.size();
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

                    // Generate the matched workload once for this (scenario, trial)
                    List<WorkloadTask> workload = generator.generateWorkload(wlType, scenario.getTaskCount(), seed, WorkloadGenerator.DEFAULT_DDOS_ATTACK_RATIO, distribution);
                    List<Task> tasks = generator.toTasks(workload);

                    // Execute each algorithm on the identical workload
                    for (Map.Entry<String, SchedulingAlgorithm> entry : algorithms.entrySet()) {
                        String algoName = entry.getKey();
                        SchedulingAlgorithm algorithm = entry.getValue();

                        double analyticalMakespan = -1.0;
                        double analyticalResourceUtilization = -1.0;
                        double cloudSimMakespan = -1.0;
                        double cloudSimResourceUtilization = -1.0;
                        long execTime = 0;
                        String status = "SUCCESS";
                        String errorDetails = "";

                        try {
                            long startNs = System.nanoTime();
                            Schedule schedule = algorithm.solve(tasks, vmCount, scenario.getIterations(), seed);
                            long endNs = System.nanoTime();
                            execTime = (endNs - startNs) / 1_000_000L;

                            // Analytical makespan and Resource Utilization evaluation (Equation 6 & 9)
                            FitnessEvaluator.Result evalResult = evaluator.evaluate(tasks, schedule);
                            analyticalMakespan = evalResult.getMakespan();
                            analyticalResourceUtilization = evalResult.getResourceUtilization();

                            // CloudSim simulation execution
                            if (runCloudSim) {
                                CloudSimScheduleAdapter.Result csResult =
                                        cloudSimAdapter.executeWorkload(workload, schedule, vmCount);

                                for (CloudSimScheduleAdapter.Execution ex : csResult.getExecutions()) {
                                    if (ex.getStatus() != Cloudlet.SUCCESS) {
                                        status = "WARNING";
                                        errorDetails = "Cloudlet " + ex.getCloudletId() + " status=" + ex.getStatus();
                                        break;
                                    }
                                }
                                cloudSimMakespan = csResult.getObservedMakespan();
                                cloudSimResourceUtilization = csResult.getResourceUtilization();
                            }
                        } catch (Exception ex) {
                            status = "FAILURE";
                            errorDetails = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                            System.err.printf("ERROR in %s Scenario %d Trial %d: %s%n",
                                    algoName, scenario.getScenarioId(), trial + 1, errorDetails);
                        }

                        Record record = new Record(
                                algoName,
                                scenario.getScenarioId(),
                                wlType.name(),
                                scenario.getTaskCount(),
                                vmCount,
                                scenario.getIterations(),
                                trial + 1,
                                seed,
                                analyticalMakespan,
                                analyticalResourceUtilization,
                                cloudSimMakespan,
                                cloudSimResourceUtilization,
                                execTime,
                                status,
                                errorDetails);

                        allRecords.add(record);
                        completedRuns++;
                    }
                }
            }
        }

        long totalTimeMs = System.currentTimeMillis() - experimentStart;
        System.out.printf("%nAll %d runs completed in %.2f seconds!%n",
                completedRuns, totalTimeMs / 1000.0);

        // Write outputs
        Path rawDir = outputDir.resolve("raw");
        Files.createDirectories(rawDir);

        Path rawCsv = rawDir.resolve("raw_experiment_results.csv");
        writeCsv(rawCsv, allRecords);
        System.out.println("Raw results written to: " + rawCsv.toAbsolutePath());

        // Also write legacy files for GA and PSO on normal workload for backward compatibility
        writeLegacyFiles(outputDir, allRecords);

        // Run analysis and generate summary files
        ExperimentAnalyzer.analyze(rawCsv, outputDir.resolve("summary"));
    }

    private static void writeCsv(Path path, List<Record> records) throws IOException {
        String header = "algorithm,scenario_id,workload_type,task_count,vm_count,iterations,trial,seed,analytical_makespan,analytical_resource_utilization,cloudsim_makespan,cloudsim_resource_utilization,execution_time_ms,status,error_details";
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(header);
            writer.newLine();
            for (Record r : records) {
                writer.write(r.toCsvRow());
                writer.newLine();
            }
        }
    }

    private static void writeLegacyFiles(Path outputDir, List<Record> records) {
        try {
            Path gaPath = outputDir.resolve("ga_normal_results.csv");
            Path psoPath = outputDir.resolve("pso_normal_results.csv");

            try (BufferedWriter gaWriter = Files.newBufferedWriter(gaPath, StandardCharsets.UTF_8);
                 BufferedWriter psoWriter = Files.newBufferedWriter(psoPath, StandardCharsets.UTF_8)) {

                String header = "algorithm,scenario,trial,seed,makespan";
                gaWriter.write(header);
                gaWriter.newLine();
                psoWriter.write(header);
                psoWriter.newLine();

                for (Record r : records) {
                    if ("NORMAL".equalsIgnoreCase(r.workloadType)) {
                        String row = String.format("%s,%d,%d,%d,%.16f",
                                r.algorithm.equals("GA") ? "GeneticAlgorithmScheduler" : (r.algorithm.equals("PSO") ? "ParticleSwarmScheduler" : r.algorithm),
                                r.scenarioId,
                                r.trialNumber,
                                r.seed,
                                r.analyticalMakespan);
                        if ("GA".equals(r.algorithm)) {
                            gaWriter.write(row);
                            gaWriter.newLine();
                        } else if ("PSO".equals(r.algorithm)) {
                            psoWriter.write(row);
                            psoWriter.newLine();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: could not write legacy CSVs: " + e.getMessage());
        }
    }
}
