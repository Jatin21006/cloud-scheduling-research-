# Reproduction & Operational Run Guide

## 1. Prerequisites and Environment

- **Operating System:** Windows 10 / 11 (64-bit)
- **Java Development Kit (JDK):** JDK 21 (or JDK 8+)
  - Verified path: `C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe`
- **Shell:** Windows PowerShell 5.1 or PowerShell Core (pwsh)
- **Python (Optional for charts):** Python 3.8+ (Uses standard library only, zero pip dependencies required)
- **CloudSim Toolkit:** Bundled in `cloudsim-3.0.3/jars/cloudsim-3.0.3.jar` (no external Maven downloads required)

---

## 2. Automated Test Execution

To compile the entire codebase (including CloudSim integration) and run all 12 unit and integration test suites:

```powershell
# From the repository root
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run_tests.ps1
```

### What `run_tests.ps1` Validates:
1. **Compilation:** Compiles all 32 source files against `cloudsim-3.0.3.jar`.
2. **Scheduling Algorithms:** Validates GA, PSO, ABC, and Hybrid-GA-PSO for:
   - Output schedule validity (every task assigned to a valid VM).
   - Deterministic execution with fixed seeds.
   - Input validation (rejects null tasks, negative iterations, non-positive VM counts).
   - Graceful handling of empty task lists ($N = 0$).
3. **Fitness Evaluation:** Tests mathematical makespan calculations against hand-calculated ground truth.
4. **Workload Generation:** Verifies Table 3 scenarios, 300 MI normal tasks, 30,000 MI DDoS attack tasks, and deterministic selection.
5. **CSV Writing:** Validates CSV schema preservation and file integrity.
6. **CloudSim Integration:** Runs actual simulation via `CloudSimScheduleAdapter`, verifying all 5 Cloudlets complete with `Cloudlet.SUCCESS` and observed makespan matches mathematical calculations.

---

## 3. Automated Experiment Pipeline

To compile, execute the entire 480-run experiment matrix, calculate statistical summaries, and generate SVG/HTML charts:

```powershell
# From the repository root
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run_experiments.ps1
```

### Custom Execution Flags:
You can also run specific subsets of experiments using command-line arguments:

```powershell
# Run only Scenario 1 on Normal Workload
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run_experiments.ps1 -Scenario 1 -Workload NORMAL

# Run only DDoS Workload on PSO and GA with 5 VMs
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run_experiments.ps1 -Workload DDOS -Algorithm PSO

# Run fast analytical evaluation (skipping CloudSim simulation)
powershell.exe -ExecutionPolicy Bypass -File .\scripts\run_experiments.ps1 -SkipCloudSim
```

---

## 4. Manual Direct Java Invocations

If you prefer direct command-line compilation and execution without PowerShell scripts:

### Step 1: Compile All Sources
```powershell
$sourceFiles = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
& "C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe" -cp "cloudsim-3.0.3\jars\cloudsim-3.0.3.jar;out" -d out $sourceFiles
```

### Step 2: Run the Unified Experiment Runner
```powershell
& "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" -cp "cloudsim-3.0.3\jars\cloudsim-3.0.3.jar;out" core.UnifiedExperimentRunner
```

### Step 3: Run the Statistical Analyzer
```powershell
& "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" -cp "cloudsim-3.0.3\jars\cloudsim-3.0.3.jar;out" core.ExperimentAnalyzer results\raw\raw_experiment_results.csv results\summary
```

### Step 4: Generate Charts
```powershell
python scripts\generate_charts.py
```

---

## 5. Inspecting Outputs

All generated experimental deliverables are stored in `results/`:
- **Raw Trial Data (480 entries):** `results/raw/raw_experiment_results.csv`
- **Summary Statistics:** `results/summary/experiment_summary.csv`
- **Normal Workload Summary:** `results/summary/normal_workload_summary.csv`
- **DDoS Workload Summary:** `results/summary/ddos_workload_summary.csv`
- **Visual Figures (SVG):**
  - `results/figures/makespan_comparison_normal.svg`
  - `results/figures/makespan_comparison_ddos.svg`
  - `results/figures/convergence_iterations.svg`
  - `results/figures/execution_time_comparison.svg`
- **Web Dashboard:** `results/figures/dashboard.html` (open in any web browser to view interactive charts and full summary tables).
