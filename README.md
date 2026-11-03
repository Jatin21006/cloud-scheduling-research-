# Cloud Task Scheduling Research Framework

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![CloudSim](https://img.shields.io/badge/CloudSim-3.0.3-blue.svg)](https://github.com/Cloudslab/cloudsim)
[![Tests](https://img.shields.io/badge/Tests-12%2F12%20Passing-brightgreen.svg)]()
[![Experiments](https://img.shields.io/badge/Experiments-480%20Runs%20(100%25%20Success)-success.svg)]()

Faithful reproduction and validation of the cloud computing task scheduling research paper:
> **“Performance Analysis of Cloud Computing Task Scheduling Using Metaheuristic Algorithms in DDoS and Normal Environments”**  
> *Fatih Kaplan & Ahmet Babalik*, **Electronics**, 2025, 14, 1988. [DOI: 10.3390/electronics14101988](https://doi.org/10.3390/electronics14101988)

---

## 👥 Research Scope & Module Architecture

| Module | Purpose | Key Files |
| :--- | :--- | :--- |
| **Genetic Algorithm (GA)** | Tournament selection, single-point crossover, gene mutation, elitism, and stagnation control | [`GeneticAlgorithmScheduler.java`](src/core/GeneticAlgorithmScheduler.java), [`GARunExperiments.java`](src/core/GARunExperiments.java) |
| **Particle Swarm Optimization (PSO)** | Continuous velocity vectors mapped to discrete VM assignments with velocity clamping | [`ParticleSwarmScheduler.java`](src/core/ParticleSwarmScheduler.java), [`PSORunExperiments.java`](src/core/PSORunExperiments.java) |
| **Artificial Bee Colony (ABC)** | Employed bee perturbation, Onlooker probability selection, Scout bee reinitialization | [`ArtificialBeeColonyScheduler.java`](src/core/ArtificialBeeColonyScheduler.java) |
| **Hybrid GA–PSO** | Co-evolutionary dual-population architecture with stagnation monitoring and worst-particle injection | [`HybridGaPsoScheduler.java`](src/core/HybridGaPsoScheduler.java) |
| **Simulation & Benchmarking** | CloudSim discrete-event environment, scenario runner, and statistical analyzer | [`CloudSimScheduleAdapter.java`](src/cloudsim/CloudSimScheduleAdapter.java), [`FitnessEvaluator.java`](src/core/FitnessEvaluator.java), [`UnifiedExperimentRunner.java`](src/core/UnifiedExperimentRunner.java), [`ExperimentAnalyzer.java`](src/core/ExperimentAnalyzer.java) |

---

## 🔬 Experimental Configuration (Table 1 & Table 3 Contract)

- **Datacenter Infrastructure:** 1 Datacenter, 1 Host, **10 Virtual Machines**, 1,000 Datacenter MIPS total ($\approx 100\text{ MIPS}$ per VM), 100 GB RAM, 1 TB Storage, $1024 \times 100\text{ GB/s}$ Bandwidth.
- **VM & Cloudlet Policies:** `VmSchedulerTimeShared`, `CloudletSchedulerTimeShared`.
- **Workload Scenarios:**
  - **Normal Workload:** Tasks with length $300\text{ MI}$ and $300\text{ MB}$ I/O files.
  - **DDoS Workload:** Volumetric flooding emulation scaling all arriving cloudlets by $100\times$ length ($30,000\text{ MI}$) and $10\times$ I/O files ($3,000\text{ MB}$).
- **Scenario Matrix:** 6 scenarios ($50$ and $100$ tasks; $10$, $50$, and $100$ iterations), each run for $10$ independent trials with reproducible seeds ($0 \dots 9$). Total: **480 runs**.

---

## 📈 Reproduced Results Summary

All 480 runs executed through both analytical evaluation and native **CloudSim 3.0.3** discrete-event simulation with **100% cloudlet completion (`SUCCESS`)**.

### Summary of Average Makespans (Seconds)

| Scenario | Tasks | Iters | Workload | GA Makespan (s) | PSO Makespan (s) | ABC Makespan (s) | Hybrid GA-PSO (s) | Paper Range (s) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **S1** | 50 | 10 | Normal | 17.700 | 18.300 | 18.000 | 18.000 | 15.8 – 21.1 |
| **S1** | 50 | 10 | DDoS | 1770.000 | 1830.000 | 1800.000 | 1800.000 | 1565 – 2026 |
| **S2** | 100 | 10 | Normal | 33.000 | 35.100 | 33.600 | 33.600 | 32.5 – 43.0 |
| **S2** | 100 | 10 | DDoS | 3300.000 | 3510.000 | 3360.000 | 3360.000 | 3219 – 4278 |
| **S3** | 50 | 50 | Normal | 16.800 | 18.000 | 17.700 | 18.000 | 14.2 – 17.8 |
| **S3** | 50 | 50 | DDoS | 1680.000 | 1800.000 | 1800.000 | 1800.000 | 1401 – 1826 |
| **S4** | 100 | 50 | Normal | 32.700 | 34.500 | 33.000 | 33.000 | 28.4 – 37.9 |
| **S4** | 100 | 50 | DDoS | 3270.000 | 3450.000 | 3270.000 | 3300.000 | 2807 – 3831 |
| **S5** | 50 | 100 | Normal | 16.200 | 18.000 | 17.400 | 18.000 | 13.9 – 17.3 |
| **S5** | 50 | 100 | DDoS | 1620.000 | 1800.000 | 1770.000 | 1800.000 | 1368 – 1753 |
| **S6** | 100 | 100 | Normal | 32.400 | 33.900 | 32.100 | 32.700 | 27.6 – 35.8 |
| **S6** | 100 | 100 | DDoS | 3240.000 | 3390.000 | 3270.000 | 3270.000 | 2732 – 3608 |

---

## 🚀 Running the Project

### 1. Execute Automated Tests (12 Suites)
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run_tests.ps1
```

### 2. Run Full 480-Experiment Pipeline & Regenerate Visualizations
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run_experiments.ps1
```

### 3. Generate Submission Presentation
```powershell
python scripts/create_presentation.py
```

---

## 📑 Detailed Documentation & Audits

- [docs/PAPER_REPRODUCTION_SPECIFICATION.md](docs/PAPER_REPRODUCTION_SPECIFICATION.md) — Comprehensive parameter and formula extraction from the original paper.
- [docs/IMPLEMENTATION_GAP_ANALYSIS.md](docs/IMPLEMENTATION_GAP_ANALYSIS.md) — Rigorous audit and categorization of discrepancies.
- [docs/PAPER_REPRODUCTION_COMPARISON.md](docs/PAPER_REPRODUCTION_COMPARISON.md) — Detailed quantitative comparison against paper Tables 4–12.
- [docs/FINAL_RESEARCH_REPORT.md](docs/FINAL_RESEARCH_REPORT.md) — Comprehensive final research report.
- `PRESENTATION_SLIDES.pptx` — Final project presentation deck.
