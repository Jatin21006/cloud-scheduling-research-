package core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistical analyzer for cloud task scheduling experiment results.
 * Computes mean, median, standard deviation, min, max, execution times,
 * and relative improvements over baseline.
 */
public class ExperimentAnalyzer {

    public static class Row {
        public String algorithm;
        public int scenarioId;
        public String workloadType;
        public int taskCount;
        public int vmCount;
        public int iterations;
        public int trialNumber;
        public long seed;
        public double analyticalMakespan;
        public double cloudSimMakespan;
        public long executionTimeMs;
        public String status;
        public String errorDetails;
    }

    public static class GroupStats {
        public String algorithm;
        public int scenarioId;
        public String workloadType;
        public int taskCount;
        public int vmCount;
        public int iterations;

        public int totalTrials;
        public int successfulTrials;

        public double meanAnalyticalMakespan;
        public double medianAnalyticalMakespan;
        public double stdAnalyticalMakespan;
        public double minAnalyticalMakespan;
        public double maxAnalyticalMakespan;

        public double meanCloudSimMakespan;
        public double medianCloudSimMakespan;
        public double stdCloudSimMakespan;
        public double minCloudSimMakespan;
        public double maxCloudSimMakespan;

        public double meanExecutionTimeMs;

        public double improvementOverPsoPct; // ((Makespan_PSO - Makespan_A) / Makespan_PSO) * 100
        public double improvementOverGaPct;  // ((Makespan_GA - Makespan_A) / Makespan_GA) * 100
    }

    public static void main(String[] args) throws IOException {
        Path rawCsv = Path.of("results", "raw", "raw_experiment_results.csv");
        Path summaryDir = Path.of("results", "summary");
        if (args.length > 0) {
            rawCsv = Path.of(args[0]);
        }
        if (args.length > 1) {
            summaryDir = Path.of(args[1]);
        }
        analyze(rawCsv, summaryDir);
    }

    public static void analyze(Path rawCsv, Path summaryDir) throws IOException {
        if (!Files.exists(rawCsv)) {
            System.err.println("Cannot run analysis: file not found: " + rawCsv);
            return;
        }

        Files.createDirectories(summaryDir);
        List<Row> rows = readRawRows(rawCsv);

        // Group rows by Key: "workloadType|scenarioId|algorithm"
        Map<String, List<Row>> grouped = new LinkedHashMap<String, List<Row>>();
        for (Row r : rows) {
            String key = r.workloadType + "|" + r.scenarioId + "|" + r.algorithm;
            if (!grouped.containsKey(key)) {
                grouped.put(key, new ArrayList<Row>());
            }
            grouped.get(key).add(r);
        }

        Map<String, GroupStats> statsMap = new LinkedHashMap<String, GroupStats>();

        for (Map.Entry<String, List<Row>> entry : grouped.entrySet()) {
            List<Row> groupRows = entry.getValue();
            if (groupRows.isEmpty()) continue;

            Row first = groupRows.get(0);
            GroupStats gs = new GroupStats();
            gs.algorithm = first.algorithm;
            gs.scenarioId = first.scenarioId;
            gs.workloadType = first.workloadType;
            gs.taskCount = first.taskCount;
            gs.vmCount = first.vmCount;
            gs.iterations = first.iterations;
            gs.totalTrials = groupRows.size();

            List<Double> analyticalVals = new ArrayList<Double>();
            List<Double> cloudSimVals = new ArrayList<Double>();
            List<Long> execTimes = new ArrayList<Long>();

            int successes = 0;
            for (Row r : groupRows) {
                if ("SUCCESS".equalsIgnoreCase(r.status)) {
                    successes++;
                    analyticalVals.add(r.analyticalMakespan);
                    cloudSimVals.add(r.cloudSimMakespan);
                    execTimes.add(r.executionTimeMs);
                }
            }
            gs.successfulTrials = successes;

            gs.meanAnalyticalMakespan = computeMean(analyticalVals);
            gs.medianAnalyticalMakespan = computeMedian(analyticalVals);
            gs.stdAnalyticalMakespan = computeStd(analyticalVals, gs.meanAnalyticalMakespan);
            gs.minAnalyticalMakespan = computeMin(analyticalVals);
            gs.maxAnalyticalMakespan = computeMax(analyticalVals);

            gs.meanCloudSimMakespan = computeMean(cloudSimVals);
            gs.medianCloudSimMakespan = computeMedian(cloudSimVals);
            gs.stdCloudSimMakespan = computeStd(cloudSimVals, gs.meanCloudSimMakespan);
            gs.minCloudSimMakespan = computeMin(cloudSimVals);
            gs.maxCloudSimMakespan = computeMax(cloudSimVals);

            double sumExec = 0;
            for (Long t : execTimes) sumExec += t;
            gs.meanExecutionTimeMs = execTimes.isEmpty() ? 0 : sumExec / execTimes.size();

            statsMap.put(entry.getKey(), gs);
        }

        // Calculate relative improvements over baselines within matched scenario & workload
        for (GroupStats gs : statsMap.values()) {
            String psoKey = gs.workloadType + "|" + gs.scenarioId + "|PSO";
            String gaKey = gs.workloadType + "|" + gs.scenarioId + "|GA";

            if (statsMap.containsKey(psoKey)) {
                double psoMakespan = statsMap.get(psoKey).meanAnalyticalMakespan;
                if (psoMakespan > 0) {
                    gs.improvementOverPsoPct = ((psoMakespan - gs.meanAnalyticalMakespan) / psoMakespan) * 100.0;
                }
            }

            if (statsMap.containsKey(gaKey)) {
                double gaMakespan = statsMap.get(gaKey).meanAnalyticalMakespan;
                if (gaMakespan > 0) {
                    gs.improvementOverGaPct = ((gaMakespan - gs.meanAnalyticalMakespan) / gaMakespan) * 100.0;
                }
            }
        }

        // Write summary CSVs
        Path allSummaryPath = summaryDir.resolve("experiment_summary.csv");
        Path normalSummaryPath = summaryDir.resolve("normal_workload_summary.csv");
        Path ddosSummaryPath = summaryDir.resolve("ddos_workload_summary.csv");

        writeSummaryCsv(allSummaryPath, statsMap.values());

        List<GroupStats> normalList = new ArrayList<GroupStats>();
        List<GroupStats> ddosList = new ArrayList<GroupStats>();
        for (GroupStats gs : statsMap.values()) {
            if ("NORMAL".equalsIgnoreCase(gs.workloadType)) normalList.add(gs);
            else if ("DDOS".equalsIgnoreCase(gs.workloadType)) ddosList.add(gs);
        }
        writeSummaryCsv(normalSummaryPath, normalList);
        writeSummaryCsv(ddosSummaryPath, ddosList);

        printSummaryTable(statsMap.values());
    }

    private static double computeMean(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : vals) sum += v;
        return sum / vals.size();
    }

    private static double computeMedian(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        List<Double> copy = new ArrayList<Double>(vals);
        Collections.sort(copy);
        int mid = copy.size() / 2;
        if (copy.size() % 2 == 1) {
            return copy.get(mid);
        } else {
            return (copy.get(mid - 1) + copy.get(mid)) / 2.0;
        }
    }

    private static double computeStd(List<Double> vals, double mean) {
        if (vals.size() <= 1) return 0.0;
        double sumSq = 0.0;
        for (double v : vals) {
            sumSq += Math.pow(v - mean, 2);
        }
        return Math.sqrt(sumSq / (vals.size() - 1));
    }

    private static double computeMin(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        double min = Double.MAX_VALUE;
        for (double v : vals) min = Math.min(min, v);
        return min;
    }

    private static double computeMax(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        double max = -Double.MAX_VALUE;
        for (double v : vals) max = Math.max(max, v);
        return max;
    }

    private static void writeSummaryCsv(Path path, Iterable<GroupStats> statsList) throws IOException {
        String header = "workload_type,scenario_id,task_count,vm_count,iterations,algorithm,trials,successes,"
                + "mean_analytical_makespan,median_analytical_makespan,std_analytical_makespan,min_analytical_makespan,max_analytical_makespan,"
                + "mean_cloudsim_makespan,median_cloudsim_makespan,std_cloudsim_makespan,min_cloudsim_makespan,max_cloudsim_makespan,"
                + "mean_execution_time_ms,improvement_over_pso_pct,improvement_over_ga_pct";

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(header);
            writer.newLine();
            for (GroupStats gs : statsList) {
                String row = String.format("%s,%d,%d,%d,%d,%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f,%.2f",
                        gs.workloadType,
                        gs.scenarioId,
                        gs.taskCount,
                        gs.vmCount,
                        gs.iterations,
                        gs.algorithm,
                        gs.totalTrials,
                        gs.successfulTrials,
                        gs.meanAnalyticalMakespan,
                        gs.medianAnalyticalMakespan,
                        gs.stdAnalyticalMakespan,
                        gs.minAnalyticalMakespan,
                        gs.maxAnalyticalMakespan,
                        gs.meanCloudSimMakespan,
                        gs.medianCloudSimMakespan,
                        gs.stdCloudSimMakespan,
                        gs.minCloudSimMakespan,
                        gs.maxCloudSimMakespan,
                        gs.meanExecutionTimeMs,
                        gs.improvementOverPsoPct,
                        gs.improvementOverGaPct);
                writer.write(row);
                writer.newLine();
            }
        }
    }

    private static void printSummaryTable(Iterable<GroupStats> statsList) {
        System.out.println("\n===========================================================================================================================================");
        System.out.println("                                                   EXPERIMENTAL SUMMARY STATISTICS                                                         ");
        System.out.println("===========================================================================================================================================");
        System.out.printf("%-7s | %-4s | %-5s | %-5s | %-12s | %-12s | %-12s | %-10s | %-10s | %-10s%n",
                "Workload", "Scen", "Tasks", "Iter", "Algorithm", "Mean M-Span", "CloudSim M-Span", "Std Dev", "Exec (ms)", "Imprv vs PSO");
        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------");

        for (GroupStats gs : statsList) {
            System.out.printf("%-7s | %-4d | %-5d | %-5d | %-12s | %12.4f | %15.4f | %10.4f | %10.2f | %11.2f%%%n",
                    gs.workloadType,
                    gs.scenarioId,
                    gs.taskCount,
                    gs.iterations,
                    gs.algorithm,
                    gs.meanAnalyticalMakespan,
                    gs.meanCloudSimMakespan,
                    gs.stdAnalyticalMakespan,
                    gs.meanExecutionTimeMs,
                    gs.improvementOverPsoPct);
        }
        System.out.println("===========================================================================================================================================\n");
    }

    private static List<Row> readRawRows(Path csv) throws IOException {
        List<Row> rows = new ArrayList<Row>();
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 12) continue;

                Row r = new Row();
                r.algorithm = parts[0].trim();
                r.scenarioId = Integer.parseInt(parts[1].trim());
                r.workloadType = parts[2].trim();
                r.taskCount = Integer.parseInt(parts[3].trim());
                r.vmCount = Integer.parseInt(parts[4].trim());
                r.iterations = Integer.parseInt(parts[5].trim());
                r.trialNumber = Integer.parseInt(parts[6].trim());
                r.seed = Long.parseLong(parts[7].trim());
                r.analyticalMakespan = Double.parseDouble(parts[8].trim());
                r.cloudSimMakespan = Double.parseDouble(parts[9].trim());
                r.executionTimeMs = Long.parseLong(parts[10].trim());
                r.status = parts[11].trim();
                r.errorDetails = parts.length > 12 ? parts[12].trim() : "";
                rows.add(r);
            }
        }
        return rows;
    }
}
