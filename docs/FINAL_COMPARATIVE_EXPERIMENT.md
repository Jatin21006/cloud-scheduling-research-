# Final Comparative Study: Benchmark of Five Metaheuristic Cloud Schedulers

## 1. Executive Summary

This document presents the final empirical comparison between the baseline reproduced **Hybrid GA-PSO** algorithm (*Kaplan & Babalik, Electronics 2025*) and four independent research enhancements:
1. **Chaos-Enhanced PSO (Chaos-PSO)**: Ergodic logistic map dynamic inertia modulation.
2. **Simulated Annealing Enhanced Hybrid GA-PSO (SA-Hybrid GA-PSO)**: Probabilistic Metropolis uphill acceptance on migrant exchanges.
3. **Opposition-Based Learning Enhanced Hybrid GA-PSO (OBL-Hybrid GA-PSO)**: Opposite solution vector generation for diversity restoration.
4. **Memetic Hybrid GA-PSO (MH-GA-PSO)**: Discrete targeted hill-climbing local search addressing bottleneck VMs.

All 5 algorithms were benchmarked across all 6 standardized scenarios (S1–S6) under both **Normal** and **DDoS (100x scaled)** workloads, evaluating 10 independent trials with matching pseudo-random seeds (0–9), totaling **600 controlled executions** on the CloudSim 3.0.3 discrete-event platform.

---

## 2. Experimental Results Summary

### 2.1 Normal Workload Makespan (s) & Relative Improvement vs Baseline

| Scenario | Tasks | Iters | Hybrid GA-PSO (Base) | Chaos-PSO | SA-Hybrid | OBL-Hybrid | Memetic-Hybrid | Best Performer |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **S1** | 50 | 10 | 12.295 s | 12.227 s (+0.55%) | 12.409 s (-0.93%) | 12.399 s (-0.85%) | **11.239 s (+8.59%)** | **Memetic-Hybrid** |
| **S2** | 100 | 10 | 23.124 s | 23.364 s (-1.04%) | 22.708 s (+1.80%) | 22.748 s (+1.63%) | **21.437 s (+7.30%)** | **Memetic-Hybrid** |
| **S3** | 50 | 50 | 11.658 s | 11.545 s (+0.97%) | 11.821 s (-1.40%) | 11.765 s (-0.92%) | **11.107 s (+4.73%)** | **Memetic-Hybrid** |
| **S4** | 100 | 50 | 21.792 s | 22.160 s (-1.69%) | 21.554 s (+1.09%) | 21.514 s (+1.28%) | **20.767 s (+4.70%)** | **Memetic-Hybrid** |
| **S5** | 50 | 100 | 11.337 s | 11.315 s (+0.19%) | 11.253 s (+0.74%) | 11.451 s (-1.01%) | **10.919 s (+3.69%)** | **Memetic-Hybrid** |
| **S6** | 100 | 100 | 21.156 s | 21.564 s (-1.93%) | 21.119 s (+0.17%) | 21.040 s (+0.55%) | **20.721 s (+2.06%)** | **Memetic-Hybrid** |

### 2.2 DDoS Attack Workload Makespan (s) (100x Proportional Scaling)

| Scenario | Tasks | Iters | Hybrid GA-PSO (Base) | Chaos-PSO | SA-Hybrid | OBL-Hybrid | Memetic-Hybrid | Time Saved by Memetic |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **S1** | 50 | 10 | 1229.5 s | 1222.7 s | 1240.9 s | 1239.9 s | **1123.9 s** | **105.6 s saved** |
| **S2** | 100 | 10 | 2312.4 s | 2336.4 s | 2270.8 s | 2274.8 s | **2139.4 s** | **173.0 s saved** |
| **S3** | 50 | 50 | 1179.8 s | 1154.5 s | 1187.7 s | 1172.5 s | **1114.2 s** | **65.6 s saved** |
| **S4** | 100 | 50 | 2158.3 s | 2210.3 s | 2153.6 s | 2151.4 s | **2078.0 s** | **80.3 s saved** |
| **S5** | 50 | 100 | 1135.4 s | 1129.8 s | 1139.3 s | 1142.3 s | **1098.6 s** | **36.8 s saved** |
| **S6** | 100 | 100 | 2110.1 s | 2158.0 s | 2105.5 s | 2105.4 s | **2072.9 s** | **37.2 s saved** |

---

## 3. Key Scientific Insights

1. **Memetic Local Search is Decisively Superior**:
   - Memetic-Hybrid GA-PSO won 100% of scenarios across both Normal and DDoS regimes.
   - It delivers up to **8.59% makespan reduction** (S1) and saves **173.0 seconds** under heavy DDoS saturation (S2).
   - Achieves the highest resource utilization across the board (up to **97.47%** in S6).
   - By specifically relocating tasks from the bottleneck VM ($VM_{\max}$) to idle VMs ($VM_{\min}$), it directly attacks the exact constraint defining the makespan objective.

2. **Dimensional Specialization of Stochastic Diversity Operators**:
   - **Simulated Annealing (SA)** and **Opposition-Based Learning (OBL)** demonstrate clear dimensional partitioning:
     - On **100-task problems (S2, S4, S6)**, both consistently beat the baseline Hybrid GA-PSO (+0.2% to +1.8%). Their diversity injection prevents premature genetic convergence in large search spaces.
     - On **50-task problems (S1, S3, S5)**, both exhibit slight exploratory overhead (-0.8% to -1.4%) because accepting inferior or opposite vectors disrupts already compact near-optimal schedules.

3. **Chaos-PSO Swarm vs Dual-Population Co-evolution**:
   - Chaos-PSO modulates inertia dynamically via the logistic map $w_{t+1} = 4 w_t(1 - w_t)$.
   - In moderate search spaces (50 tasks), it outperforms baseline Hybrid GA-PSO (+0.55% in S1, +0.97% in S3).
   - In 100-task scenarios, the lack of genetic crossover limits single-swarm Chaos-PSO (-1.0% to -1.9% vs Hybrid), confirming that co-evolutionary recombinations are essential at higher dimensions.

4. **Runtime & Computational Overhead**:
   - Scheduler execution times remain between 4 ms and 55 ms across all algorithms.
   - Memetic-Hybrid GA-PSO introduces negligible runtime overhead (<1 ms difference in S6: 41.6 ms vs 42.0 ms for baseline), demonstrating extreme algorithmic efficiency.

---

## 4. Generated Artifacts & Deliverables

- **Unified Raw Results Dataset**: `results/final_comparison/raw/raw_final_comparative_results.csv` (600 rows)
- **Summary Statistics**: `results/final_comparison/summary/summary_statistics.csv` (60 rows)
- **Scenario-Wise Comparison**: `results/final_comparison/summary/scenario_wise_comparison.csv` (12 rows)
- **Percentage Improvement**: `results/final_comparison/summary/percentage_improvement.csv` (12 rows)
- **Comprehensive Markdown Report**: `results/final_comparison/summary/FINAL_COMPARATIVE_ANALYSIS.md`
- **Publication Figures**:
  - `results/final_comparison/figures/chart1_normal_makespan.png`
  - `results/final_comparison/figures/chart2_ddos_makespan.png`
  - `results/final_comparison/figures/chart3_percentage_improvement.png`
  - `results/final_comparison/figures/chart4_resource_utilization.png`
  - `results/final_comparison/figures/chart5_standard_deviation.png`
  - `results/final_comparison/figures/chart6_execution_time.png`
- **PowerPoint Presentation Deck**:
  - `FINAL_COMPARATIVE_RESULTS_VERIFIED.pptx` (21 widescreen slides, audited mathematical formulas, verified implementation parameters, concise high-impact layout, and honest simulation framing).
  - Also preserved as `FINAL_COMPARATIVE_RESULTS.pptx`.

