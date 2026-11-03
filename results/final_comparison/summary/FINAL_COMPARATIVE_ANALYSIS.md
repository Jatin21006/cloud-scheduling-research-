# Final Comparative Experimental Analysis & Scientific Audit

Comprehensive matched experimental evaluation comparing Baseline Hybrid GA-PSO against all four research extensions across 600 total executions on CloudSim 3.0.3.

---

## 1. NORMAL Workload Evaluation (U[100, 300] MI)

| Scenario | Tasks | Iters | Hybrid-GA-PSO (s) | SA-Hybrid (s) | OBL-Hybrid (s) | Memetic-Hybrid (s) | Chaos-PSO (s) | Scenario Winner |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **S1** | 50 | 10 | 12.295 | 12.409 | 12.399 | **11.239** | 12.227 | **Memetic-Hybrid (+8.59%)** |
| **S2** | 100 | 10 | 23.124 | 22.708 | 22.748 | **21.437** | 23.364 | **Memetic-Hybrid (+7.30%)** |
| **S3** | 50 | 50 | 11.658 | 11.821 | 11.765 | **11.107** | 11.545 | **Memetic-Hybrid (+4.73%)** |
| **S4** | 100 | 50 | 21.792 | 21.554 | 21.514 | **20.767** | 22.160 | **Memetic-Hybrid (+4.70%)** |
| **S5** | 50 | 100 | 11.337 | 11.253 | 11.451 | **10.919** | 11.315 | **Memetic-Hybrid (+3.69%)** |
| **S6** | 100 | 100 | 21.156 | 21.119 | 21.040 | **20.721** | 21.564 | **Memetic-Hybrid (+2.06%)** |

---

## 2. Scaled DDoS Workload Evaluation (100x Proportional Scaling)

| Scenario | Tasks | Iters | Hybrid-GA-PSO (s) | SA-Hybrid (s) | OBL-Hybrid (s) | Memetic-Hybrid (s) | Chaos-PSO (s) | Simulated Completion Time Saved |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **S1** | 50 | 10 | 1229.500 | 1240.900 | 1239.900 | **1123.900** | 1222.700 | **105.6 s** |
| **S2** | 100 | 10 | 2312.400 | 2270.800 | 2274.800 | **2139.400** | 2336.400 | **173.0 s** |
| **S3** | 50 | 50 | 1179.800 | 1187.700 | 1172.500 | **1114.200** | 1154.500 | **65.6 s** |
| **S4** | 100 | 50 | 2158.300 | 2153.600 | 2151.400 | **2078.000** | 2210.300 | **80.3 s** |
| **S5** | 50 | 100 | 1135.400 | 1139.300 | 1142.300 | **1098.600** | 1129.800 | **36.8 s** |
| **S6** | 100 | 100 | 2110.100 | 2105.500 | 2105.400 | **2072.900** | 2158.000 | **37.2 s** |

---

## 3. Scientific Audit & Correction Pass Log

The final presentation `FINAL_COMPARATIVE_RESULTS_VERIFIED.pptx` was audited and updated to ensure strict conformity with the actual Java source code and measured experimental CSV datasets:

1. **Chaos-PSO Mathematical Precision**:
   - Updated formulas to reflect the exact 1D Logistic Map implemented in `ChaosParticleSwarmScheduler.java`:
     $$x_{t+1} = 4.0 \cdot x_t \cdot (1 - x_t)$$
     $$w(t) = W_{\min} + (W_{\max} - W_{\min}) \cdot x_{t+1} = 0.4 + 0.5 \cdot x_{t+1}$$
   - Verified swarm size ($N = 75$ particles) and confirmed singularity safeguards ($x_0$ exclusion of $\{0.0, 0.25, 0.50, 0.75, 1.0\}$ and runtime threshold clamping).

2. **Simulated Annealing Parameters & Mechanics**:
   - Corrected initial temperature description to reflect default $T_0 = 10.0$ and adaptive scaling ($T_0 = 0.02 \times \text{initial makespan}$).
   - Verified geometric cooling: $T_{t+1} = \max(10^{-4}, 0.95 \cdot T_t)$.
   - Clarified Metropolis acceptance rule for worse candidates: $P = \exp(-\Delta \text{Makespan} / T)$.

3. **Opposition-Based Learning Strategy**:
   - Accurately articulated the two-phase implementation in `OblHybridGaPsoScheduler.java`:
     - **Phase 1 (OBPI)**: 50% of the initial swarm ($p \in [38, 74]$) initialized diametrically opposite to particles $[0, 36]$: $x_{p, t} = (M - 1) - x_{p-38, t}$.
     - **Phase 2 (OBSB)**: Stagnation-triggered breakthrough (counter $\ge 15$ or periodic interval $\ge 5$ with stagnation $\ge 3$) injecting the opposite of the fittest GA individual into the worst PSO particle.

4. **Memetic Local Search Architecture**:
   - Corrected candidate selection: local refinement is applied specifically to **two elite individuals** per generation—the Global Best ($gBest$) and the fittest GA chromosome—not a generic top 10% pool.
   - Accurately described the 70% bottleneck-relief move (relocating tasks from $VM_{\max}$ to $VM_{\min}$) and 30% random task transfer, under a strict 10-step budget and greedy makespan reduction gating.

5. **Elimination of Cross-Scenario Comparisons**:
   - Removed misleading claims comparing 10-iteration results with 100-iteration results across scenarios. All comparisons now adhere strictly to intra-scenario evaluations.

6. **Distinction Between Computational Workload Scaling and Real DDoS Defense**:
   - Clarified that the DDoS simulation benchmark models volumetric computational batch load ($100\times$ task length scaling, $10,000–30,000$ MI) on the scheduling queue.
   - Removed unsupported claims of "network-level DDoS attack mitigation", "packet filtering", "server crash prevention", and "SLA defense".
   - Accurately framed improvements as simulated schedule completion time reductions under heavy computational loads.

7. **Verification of Computational Runtime Overhead**:
   - Documented actual measured CPU scheduler runtimes from `summary_statistics.csv` ($4.2\text{ ms}$ to $55.0\text{ ms}$).
   - Confirmed that Memetic Hybrid GA-PSO adds negligible overhead ($41.6\text{ ms}$ in S6 vs $42.0\text{ ms}$ for baseline Hybrid).

8. **Numerical Verification Across Tables, Percentages, and Figures**:
   - Cross-verified every single number against `summary_statistics.csv`, `scenario_wise_comparison.csv`, and `percentage_improvement.csv`. Zero discrepancies exist.
