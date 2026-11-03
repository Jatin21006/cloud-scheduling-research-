# Cloud Task Scheduling Research Framework

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![CloudSim](https://img.shields.io/badge/CloudSim-3.0.3-blue.svg)](https://github.com/Cloudslab/cloudsim)
[![Tests](https://img.shields.io/badge/Tests-12%2F12%20Passing-brightgreen.svg)]()
[![Experiments](https://img.shields.io/badge/Experiments-480%20Runs%20(100%25%20Success)-success.svg)]()

Research project evaluating population-based and swarm-based metaheuristic algorithms for **independent cloud task scheduling** under **normal operating workloads** and **synthetic volumetric DDoS attack conditions**, verified using the **CloudSim 3.0.3** discrete-event simulation toolkit.

---

## 👥 Research Team & Ownership

| Member | Assigned Scope | Key Contributions & Modules |
| :--- | :--- | :--- |
| **Kabeer** | Genetic Algorithm (GA) & Particle Swarm (PSO) | [`GeneticAlgorithmScheduler.java`](src/core/GeneticAlgorithmScheduler.java), [`ParticleSwarmScheduler.java`](src/core/ParticleSwarmScheduler.java), [`GARunExperiments.java`](src/core/GARunExperiments.java), [`PSORunExperiments.java`](src/core/PSORunExperiments.java), and core unit tests. |
| **Joy** | Artificial Bee Colony (ABC) & Hybrid GA-PSO | [`ArtificialBeeColonyScheduler.java`](src/core/ArtificialBeeColonyScheduler.java), [`HybridGaPsoScheduler.java`](src/core/HybridGaPsoScheduler.java) (hybrid velocity momentum with greedy crossover acceptance), and associated unit tests. |
| **Jatin** | Integration, Paper Alignment, Experiment Validation, GitHub Coordination, & Final Deliverables | Common scheduling interfaces, CloudSim adapter ([`CloudSimScheduleAdapter.java`](src/cloudsim/CloudSimScheduleAdapter.java)), scenario configuration matrix ([`PaperScenarios.java`](src/core/PaperScenarios.java)), [`UnifiedExperimentRunner.java`](src/core/UnifiedExperimentRunner.java), [`ExperimentAnalyzer.java`](src/core/ExperimentAnalyzer.java), chart generation, audit report, and final documentation. |

---

## 🔬 Researched Algorithms

1. **Genetic Algorithm (GA)**
   - Chromosome: Direct integer encoding $S(k) \in \{0, \dots, M-1\}$.
   - Configuration: Population $P = 75$, Tournament Size $K = 5$, One-point Crossover $P_c = 0.50$, Gene Mutation $P_m = 0.015$, Elitism $= 1$.
   - Characteristics: High convergence stability, consistently reaches global theoretical optimum makespan.

2. **Particle Swarm Optimization (PSO)**
   - Continuous velocity vectors with discrete rounding:
     $$v_{i, d}^{(t+1)} = w \cdot v_{i, d}^{(t)} + c_1 r_1 (pbest_{i, d} - x_{i, d}^{(t)}) + c_2 r_2 (gbest_d - x_{i, d}^{(t)})$$
   - Configuration: Swarm Size $S = 75$, Inertia $w = 0.90$, Acceleration $c_1 = 1.50, c_2 = 1.50$, Velocity clamping $V_{max} = M - 1$.
   - Characteristics: Fast trajectory exploration with persistent momentum.

3. **Artificial Bee Colony (ABC)**
   - Three-phase biological swarm optimization: Employed bees (neighborhood difference search), Onlooker bees (roulette-wheel probability selection based on fitness $1/(1 + \text{makespan})$), and Scout bees (abandonment limit $= 20$).
   - Configuration: Colony Size $CS = 75$, Food Sources $SN = 75$.
   - Characteristics: Exceptional convergence speed ($1.8\times$ to $4\times$ faster execution than GA/PSO) while reaching optimal makespan parity.

4. **Hybrid GA-PSO**
   - Collaborative synthesis: Combines PSO continuous velocity momentum with tournament-based genetic crossover and mutation.
   - Enhanced with **greedy acceptance gating**: genetic offspring are accepted only when they match or improve candidate fitness, preventing destructive trajectory corruption.
   - Characteristics: Balances swarm momentum with genetic diversity to overcome local minima.

---

## 📊 Experimental Benchmark Matrix (Table 3 Contract)

Experiments evaluate the benchmark scenario matrix referenced as **Table 3** across 2 workload profiles with 10 matched trials per configuration:

| Scenario | Tasks ($N$) | Iterations ($I$) | Trials | Evaluated Workloads | Total Runs |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **Scenario 1** | 50 | 10 | 10 | Normal, Synthetic DDoS | 80 |
| **Scenario 2** | 100 | 10 | 10 | Normal, Synthetic DDoS | 80 |
| **Scenario 3** | 50 | 50 | 10 | Normal, Synthetic DDoS | 80 |
| **Scenario 4** | 100 | 50 | 10 | Normal, Synthetic DDoS | 80 |
| **Scenario 5** | 50 | 100 | 10 | Normal, Synthetic DDoS | 80 |
| **Scenario 6** | 100 | 100 | 10 | Normal, Synthetic DDoS | 80 |
| **TOTAL** | — | — | — | **4 Algorithms $\times$ 480 Executions** | **480 Runs** |

### Workload Definitions
- **Normal Workload:** Homogeneous tasks with instruction length $300\text{ MI}$ and file transfer sizes $300\text{ MB}$.
- **Synthetic DDoS Workload:** 10% of tasks deterministically selected via seeded Fisher-Yates shuffle as volumetric attack tasks with $100\times$ CPU instruction length ($30,000\text{ MI}$) and $10\times$ I/O volume ($3,000\text{ MB}$).

---

## 📈 Key Results Summary

All 480 runs executed through both mathematical makespan calculation and native **CloudSim 3.0.3** discrete-event simulation with **100% cloudlet completion (`SUCCESS`)**.

### Normal Workload ($M = 5$ VMs, $1,000$ MIPS)
- Theoretical Optimal Makespan (50 Tasks): **$3.0000\text{ s}$**
- Theoretical Optimal Makespan (100 Tasks): **$6.0000\text{ s}$**

| Scenario | Tasks | Iter | GA Makespan (s) | PSO Makespan (s) | ABC Makespan (s) | Hybrid GA-PSO (s) | Best Algorithm |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **S1** | 50 | 10 | **3.0000** | 3.2700 | 3.0300 | 3.1800 | **GA** |
| **S2** | 100 | 10 | **6.0300** | 6.2700 | 6.1500 | 6.2700 | **GA** |
| **S3** | 50 | 50 | **3.0000** | 3.1200 | **3.0000** | 3.1200 | **GA / ABC** |
| **S4** | 100 | 50 | **6.0000** | 6.2400 | **6.0000** | 6.2400 | **GA / ABC** |
| **S5** | 50 | 100 | **3.0000** | 3.0300 | **3.0000** | 3.0900 | **GA / ABC** |
| **S6** | 100 | 100 | **6.0000** | 6.2400 | **6.0000** | 6.1500 | **GA / ABC** |

### DDoS Attack-Burst Workload ($10\%$ Volumetric Inflation)
- Theoretical Optimal Makespan (50 Tasks): **$32.7000\text{ s}$**
- Theoretical Optimal Makespan (100 Tasks): **$65.4000\text{ s}$**

| Scenario | Tasks | Iter | GA Makespan (s) | PSO Makespan (s) | ABC Makespan (s) | Hybrid GA-PSO (s) | Best Algorithm |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **S1** | 50 | 10 | **32.7000** | 33.0600 | 32.9400 | 33.0300 | **GA** |
| **S2** | 100 | 10 | **65.4600** | 66.4200 | 66.2100 | 66.3600 | **GA** |
| **S3** | 50 | 50 | **32.7000** | 33.0000 | **32.7000** | 32.9400 | **GA / ABC** |
| **S4** | 100 | 50 | **65.4000** | 66.2100 | 65.4600 | 66.3300 | **GA** |
| **S5** | 50 | 100 | **32.7000** | 32.9100 | **32.7000** | 32.8500 | **GA / ABC** |
| **S6** | 100 | 100 | **65.4000** | 66.0000 | **65.4000** | 66.2400 | **GA / ABC** |

---

## 🚀 How to Run Tests and Experiments

### Prerequisites
- **Java Development Kit (JDK):** JDK 21 (or JDK 8+)
- **PowerShell:** Windows PowerShell 5.1+ or PowerShell Core
- **Python:** Python 3.8+ (for chart generation; uses standard library only, zero pip installs)
- **CloudSim 3.0.3:** Bundled in `cloudsim-3.0.3/jars/cloudsim-3.0.3.jar`

### 1. Run All Test Suites
Compiles all 32 Java sources and runs all 12 regression, validation, and integration tests:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run_tests.ps1
```

### 2. Run the Full Experiment Pipeline
Executes all 480 experiment runs, calculates statistical summaries, and generates SVG charts and HTML dashboard:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run_experiments.ps1
```

### 3. View Results and Charts
- Open `results/figures/dashboard.html` in any web browser to explore interactive comparison charts.
- Inspect statistical CSV files in `results/summary/` and raw trial data in `results/raw/raw_experiment_results.csv`.

---

## 📁 Repository Structure

```
cloud-scheduling-research-/
├── cloudsim-3.0.3/               # CloudSim toolkit JAR and library sources
├── docs/                        # Research documentation and methodology
│   ├── experiment_results.md    # Full statistical tables and analysis
│   ├── implementation_details.md# System architecture and CloudSim pipeline
│   ├── limitations.md           # Research assumptions and threat to validity
│   ├── progress_log.md          # Chronological development log
│   ├── research_methodology.md  # Mathematical problem formulation
│   └── run_guide.md             # Detailed execution instructions
├── results/                     # Experimental outputs
│   ├── figures/                 # Generated SVG charts and HTML dashboard
│   ├── raw/                     # 480 raw trial records (raw_experiment_results.csv)
│   └── summary/                 # Summary statistics (experiment_summary.csv)
├── scripts/                     # Operational automation scripts
│   ├── generate_charts.py       # Python SVG chart and dashboard generator
│   ├── run_experiments.ps1      # Automated experiment execution pipeline
│   └── run_tests.ps1            # Automated test runner
├── src/                         # Source code
│   ├── cloudsim/                # CloudSim adapters and datacenter setup
│   │   ├── CloudSimScheduleAdapter.java
│   │   └── ResearchCloudSimEnvironment.java
│   └── core/                    # Schedulers, models, and experiment runners
│       ├── ArtificialBeeColonyScheduler.java  (Joy)
│       ├── ExperimentAnalyzer.java
│       ├── ExperimentRunner.java
│       ├── FitnessEvaluator.java
│       ├── GeneticAlgorithmScheduler.java     (Kabeer)
│       ├── HybridGaPsoScheduler.java          (Joy)
│       ├── PaperScenarios.java
│       ├── ParticleSwarmScheduler.java        (Kabeer)
│       ├── UnifiedExperimentRunner.java       (Jatin)
│       ├── WorkloadGenerator.java
│       └── WorkloadTask.java
├── PROJECT_STATUS_AND_REMAINING_WORK.md       # Comprehensive audit report
└── README.md                                  # Project overview and quickstart
```

---

## 📄 Key Research Deliverables

- 📘 [PROJECT_STATUS_AND_REMAINING_WORK.md](PROJECT_STATUS_AND_REMAINING_WORK.md): Full audit report covering completed tasks, remaining work, paper alignment, and prioritization.
- 📊 [docs/experiment_results.md](docs/experiment_results.md): Detailed comparative tables across all 6 scenarios and both workload types.
- 📐 [docs/research_methodology.md](docs/research_methodology.md): Mathematical formulations, objective functions, and parameter citations.
- 🖥️ [results/figures/dashboard.html](results/figures/dashboard.html): Interactive visual dashboard of research results.
