# Comprehensive Reproducibility Audit Report: Discrepancy Analysis, Parameter Reconciliation, and Algorithmic Validation

**Document Version:** 1.0  
**Project:** Cloud Computing Task Scheduling Research Extension  
**Base Study:** Kaplan, F.; Babalik, A. *Performance Analysis of Cloud Computing Task Scheduling Using Metaheuristic Algorithms in DDoS and Normal Environments.* **Electronics**, 2025, Volume 14, Issue 10, Article 1988 ([DOI: 10.3390/electronics14101988](https://doi.org/10.3390/electronics14101988)).  
**Audit Purpose:** Root-cause investigation into why reproduced PSO and Hybrid GA–PSO results diverged from published paper tables, why Chaos-PSO improved the reproduced baseline by up to 4.35% without surpassing the paper's published numbers, and how to scientifically reconcile the experimental framework.

---

## 1. Phase 1: Audit of the Original Research Paper (Direct from PDF)

A direct extraction and audit of the published MDPI PDF was conducted across all 32 pages, tables, and algorithms.

### 1.1 Experimental Configuration (Table 1, Page 7)
- **Datacenter:** 1 Datacenter, 1 Physical Host, Linux / Xen VMM.
- **Compute Infrastructure:** 10 Virtual Machines (VMs), Total Datacenter MIPS = 1,000 ($\implies 100\text{ MIPS}$ per VM).
- **Host Hardware:** DELL Latitude 3540, Intel Core i5-1335U (1.30 GHz), 8 GB RAM, 500 GB SSD, Windows 11 Pro.
- **VM Hardware Allocation:** 1,024 MB RAM, 10,240 MB Storage, 1,000 MB/s Bandwidth per VM.
- **CloudSim Policies:** `VmSchedulerTimeShared` and `CloudletSchedulerTimeShared`.
- **Task/Cloudlet Workload:** 50 and 100 Cloudlets.
- **Nominal Cloudlet Size:** 300 (Table 1).
- **Task Length Distribution:** Section 3.3, Page 16, Line 829 explicitly states:  
  > *"To assess system performance in this study, 50 to 100 cloudlets were generated following a uniform distribution. These cloudlets were assigned to VMs and executed within a simulated cloud infrastructure."*

### 1.2 Algorithm Parameters (Table 2, Page 9)
- **PSO:** Swarm size $N = 75$, Inertia factor $w = 0.9$ (constant), $c_1 = 1.5$, $c_2 = 1.5$.
- **GA:** Population size $N = 75$, Crossover rate $p_c = 0.5$, Mutation rate $p_m = 0.015$, Elitism = Yes (1 individual), Tournament size = 5 (Table 2) / 15 (Section 3.3 text), Stagnation Threshold = 0.5, Stagnation Count = 15.
- **ABC:** Colony size = 75, Food number = Cloudlet (50 or 100), Lower Bound = 0, Upper Bound = VMs (10), Abandonment Limit = 20 (literature default).
- **Proposed GA–PSO Hybrid:** Dual population ($N = 75$ GA individuals + $N = 75$ PSO particles), Crossover $p_c = 0.5$, Mutation $p_m = 0.0015$ ($10\times$ smaller than standalone GA), Elitism = Yes, Tournament size = 5, Inertia $w = 0.9$, $c_1 = c_2 = 1.5$.
- **Hybrid Iterations:** Section 3.1, Page 8, Lines 420–423 explicitly states:  
  > *"For GAs, PSO, and the ABC, the number of iterations is denoted as n. However, to maintain computational efficiency and ensure a balanced comparison across methods, the hybrid GA–PSO algorithm adopts n/2 iterations."*

### 1.3 Published Paper Numerical Results & Ambiguities

#### Makespan Tables (Tables 4, 5, 6):
```
========================================================================================
Scenario | Tasks | Iters | Workload |   GA (s)   |   PSO (s)  |   ABC (s)  | Hybrid GA-PSO (s)
========================================================================================
S1       | 50    | 10    | Normal   |     21.085 |     16.453 |     18.435 |            15.877
S1       | 50    | 10    | DDoS     |   2026.626 |   1625.146 |   2024.243 |          1565.606
S2       | 100   | 10    | Normal   |     39.604 |     32.518 |     43.007 |            32.481
S2       | 100   | 10    | DDoS     |   4050.009 |   3223.181 |   4278.628 |          3219.766
S3       | 50    | 50    | Normal   |     17.851 |     14.432 |     17.532 |            14.237
S3       | 50    | 50    | DDoS     |   1826.130 |  14230.377*|   1732.905 |          1401.912
S4       | 100   | 50    | Normal   |     37.283 |     28.542 |     37.921 |            28.415
S4       | 100   | 50    | DDoS     |   3831.651 |   2824.033 |   3766.493 |          2807.655
S5       | 50    | 100   | Normal   |     17.280 |     13.943 |     16.983 |            13.918
S5       | 50    | 100   | DDoS     |   1753.284 |   1371.998 |   1681.839 |          1368.886
S6       | 100   | 100   | Normal   |     35.766 |     27.662 |     35.803 |            27.954
S6       | 100   | 100   | DDoS     |   3608.696 |   2732.860 |   3551.635 |          2757.200
========================================================================================
```

#### Identified Errors in the Published Paper:
1. **Typographic Error in Table 5:** Scenario 3 DDoS PSO makespan is printed as `14,230.377` seconds. Because Normal makespan is $14.432\text{ s}$, this is an obvious formatting typo for $\approx 1,423.037\text{ s}$.
2. **Scenario Labeling Error in Table 6:** Table 6 labels the rows as "5th Scenario" followed by "4th Scenario". Cross-referencing with Table 3 confirms the second scenario in Table 6 is Scenario 6 (100 tasks, 100 iterations).
3. **Decimal Formatting Ambiguity:** In Table 4, Scenario 2 values are printed as `39,604`, `32,518`, `43,007` using Turkish/European comma decimal separators instead of points.

---

## 2. Phase 2: Audit of Our Implementation vs. Published Paper

A line-by-line comparison between our Java implementation and the paper reveals why reproduced results diverged:

| Investigation Area | Paper Specification | Previous Codebase State | Classification | Root Cause Impact |
| :--- | :--- | :--- | :---: | :--- |
| **1. Task Lengths & Workload** | Uniform distribution (Sec 3.3, Line 829; nominal 300 MI) | Constant 300 MI per task (`NORMAL_TASK_LENGTH_MI = 300`) | **CONFIRMED MISMATCH** | **PRIMARY ROOT CAUSE:** Constant 300 MI forced total workload to $15,000\text{ MI}$ (50 tasks) and $30,000\text{ MI}$ (100 tasks). Theoretical lower bounds were 15.0s and 30.0s, making it mathematically impossible to reach the paper's 13.9s and 27.6s. |
| **2. Fitness Landscape** | Continuous sums over non-identical task lengths | Discrete multiples of 3.0s ($300\text{ MI}/100\text{ MIPS}$) | **CONFIRMED MISMATCH** | Constant lengths created zero-gradient plateaus (18s, 21s, 33s, 36s), trapping PSO in local optima. |
| **3. Datacenter & VM MIPS** | 10 VMs sharing 1000 MIPS pool (100 MIPS each) | 10 VMs @ 100 MIPS each | **CONFIRMED MATCH** | Aligned with paper. |
| **4. CloudSim Scheduling** | `VmSchedulerTimeShared`, `CloudletSchedulerTimeShared` | Identical CloudSim classes used | **CONFIRMED MATCH** | Aligned with paper. |
| **5. PSO Velocity & Position** | Standard discrete-mapped PSO ($w=0.9, c_1=c_2=1.5$) | Exact formula implemented | **CONFIRMED MATCH** | Aligned with paper. |
| **6. Hybrid GA–PSO Procedure** | Dual populations (75 GA + 75 PSO), $n/2$ iters, stagnation injection | Exact dual-population architecture implemented | **CONFIRMED MATCH** | Aligned with paper. |
| **7. Resource Utilization** | Eq. (6): $\frac{\sum TET_m}{\text{Makespan} \times M} \times 100\%$ | Identical Eq. (6) formula used | **CONFIRMED MATCH** | Aligned with paper. |
| **8. Seeding & Trials** | $Seed = 0 + \text{trialIndex}$ across 10 trials | Identical seeding protocol | **CONFIRMED MATCH** | Aligned with paper. |

---

## 3. Mathematical Proof of the Workload Mismatch

From the paper's Resource Utilization Equation (6):
$$\text{Resource Utilization} = \frac{\sum_{m=1}^{10} TET_m}{\text{Makespan} \times 10} \implies \sum_{m=1}^{10} TET_m = \text{Makespan} \times 10 \times \text{RU}$$
And because $TET_m = \sum_{k \in T_m} \frac{MI_k}{100\text{ MIPS}}$, the total instructions executed is:
$$\text{Total MI} = 100 \times \sum_{m=1}^{10} TET_m$$

Let us calculate the total instructions for the paper's published PSO results:
1. **Scenario 5 (50 tasks, 100 iters):**
   - Published Makespan (Table 6) = $13.943\text{ s}$
   - Published RU (Table 9) = $80.99\%$
   - Total Workload $= 13.943 \times 10 \times 0.8099 \times 100 = \mathbf{11,292\text{ MI}}$
   - Average Task Size in Paper $= \frac{11,292\text{ MI}}{50\text{ tasks}} = \mathbf{225.8\text{ MI}}$ (NOT 300 MI!)
2. **Scenario 6 (100 tasks, 100 iters):**
   - Published Makespan (Table 6) = $27.662\text{ s}$
   - Published RU (Table 9) = $72.15\%$
   - Total Workload $= 27.662 \times 10 \times 0.7215 \times 100 = \mathbf{19,958\text{ MI}}$
   - Average Task Size in Paper $= \frac{19,958\text{ MI}}{100\text{ tasks}} = \mathbf{199.58\text{ MI}}$ (NOT 300 MI!)

### The Physical Impossibility in the Previous Reproduction:
- In our previous code, 100 tasks of 300 MI each = $30,000\text{ MI}$.
- On a 1,000 MIPS datacenter, completing $30,000\text{ MI}$ in $27.662\text{ s}$ would require:
  $$\text{Required Speed} = \frac{30,000\text{ MI}}{27.662\text{ s}} = 1,084.5\text{ MIPS} \quad (> 100\% \text{ capacity})$$
- It is physically impossible to complete 30,000 MI on a 1,000 MIPS datacenter in 27.662 seconds. The paper achieved 27.662s because its total workload was $\approx 20,000\text{ MI}$!

---

## 4. Phase 3: Corrective Actions & Implementation Plan

To establish rigorous scientific fidelity:

1. **Implement Authentic Uniform Workload Generation:**
   In [`WorkloadGenerator.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/WorkloadGenerator.java), implement task generation faithful to Section 3.3, Line 829:
   - Generate tasks following a uniform distribution $U[100, 300]\text{ MI}$ with nominal maximum 300 MI and mean $\approx 200\text{ MI}$.
   - Workload seed $Seed = 0 + \text{trialIndex}$ ensures exact reproducibility.
2. **Preserve All Algorithms:**
   - Keep baseline [`ParticleSwarmScheduler.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/ParticleSwarmScheduler.java) unchanged.
   - Run GA, PSO, ABC, Hybrid GA-PSO, LDIW-PSO, and Chaos-PSO across the identical workload.
3. **Execute Full 6-Scenario Matrix (Normal & DDoS, 10 Trials):**
   - Recalculate all 6 algorithms under the corrected workload.
4. **Update Both Presentations & Documentation:**
   - Document the root-cause discovery transparently in all slides, reports, and READMEs.
