# Implementation Gap Analysis

This document provides a systematic audit comparing the existing Java codebase (`cloud-scheduling-research-`) against the published paper:
**“Performance Analysis of Cloud Computing Task Scheduling Using Metaheuristic Algorithms in DDoS and Normal Environments”** (Electronics 2025, 14, 1988).

Each discrepancy is categorized under one of the formal classifications:
- **A. Confirmed implementation error**
- **B. Experimental configuration mismatch**
- **C. Algorithmic difference from the paper**
- **D. Paper does not specify enough information**
- **E. Correct implementation**
- **F. Unverified and requires investigation**

---

## Summary of Findings

| ID | Component / Area | Classification | Impact on Reported Results | Correction Required |
|---|---|---|---|---|
| **GAP-01** | VM Count ($M=10$ vs $M=5$) | **B. Experimental configuration mismatch** | Critical (~$2\times$ makespan inflation in baseline capacity) | Set `VM_COUNT = 10` globally |
| **GAP-02** | VM MIPS (100 MIPS vs 1000 MIPS) | **B. Experimental configuration mismatch** | Critical ($10\times$ makespan difference; makespan was ~3s instead of ~15–21s) | Change `VM_MIPS` from 1000.0 to 100.0 in `FitnessEvaluator` & `CloudSimScheduleAdapter` |
| **GAP-03** | DDoS Workload Generation (Volumetric flood scaling vs 10% ratio) | **B. Experimental configuration mismatch** | Critical (DDoS makespan was ~33s instead of ~1500–4000s, missing the $100\times$ flood effect) | Scale all tasks in DDoS mode by $100\times$ length and $10\times$ file size |
| **GAP-04** | Hybrid GA–PSO Architecture (Dual population vs Single swarm) | **C. Algorithmic difference from the paper** | High (Violates Algorithm 1 & Algorithm 5, missing co-evolutionary interaction) | Implement true dual-population architecture (GA pop 75 + PSO swarm 75) |
| **GAP-05** | Hybrid GA–PSO Iteration Count ($n/2$ vs $n$) | **C. Algorithmic difference from the paper** | High (Paper explicitly runs $n/2$ iterations for Hybrid GA-PSO; our code ran $n$) | Set Hybrid iterations to $n/2$ |
| **GAP-06** | Stagnation & Injection Mechanism in Hybrid GA-PSO | **C. Algorithmic difference from the paper** | High (Core novelty of the paper is stagnation detection and replacing worst particle with fittest GA individual) | Implement stagnation tracking ($\Delta < 0.5$ for 15 iters) and worst-particle replacement |
| **GAP-07** | GA Stagnation Mechanism | **C. Algorithmic difference from the paper** | Moderate (Paper reports stagnation threshold 0.5 and count 15 for GA) | Add stagnation handling to pure GA |
| **GAP-08** | Resource Utilization Calculation | **A. Confirmed implementation error** | Critical (Entire Tables 7–9 in paper report RU; our code had zero implementation) | Implement Equation (6) across all trial results and summary outputs |
| **GAP-09** | Standard Deviation Formula (Population vs Sample) | **A. Confirmed implementation error** | Low (Minor numerical deviation in Std reporting) | Change sample Std ($N-1$) to population Std ($N$) matching Equation (5) |
| **GAP-10** | Hybrid GA-PSO Mutation Probability ($0.0015$ vs $0.015$) | **C. Algorithmic difference from the paper** | Moderate (Table 2 specifies $p_m = 0.0015$ for Hybrid, but our code used $0.015$) | Update $p_m$ to $0.0015$ for Hybrid GA-PSO |
| **GAP-11** | ABC Food Source Count ($FOOD\_SOURCES$) | **D. Paper does not specify enough information** | Low-Moderate (Table 2 says `Food number: Cloudlet`; code fixed it to 75) | Set food sources dynamically to task count or document assumption |
| **GAP-12** | CloudSim Datacenter Resource Configuration | **B. Experimental configuration mismatch** | Low (Datacenter RAM, Storage, BW allocated per host rather than DC total) | Set Datacenter characteristics to match Table 1 (1 TB, 100 GB RAM, $1024 \times 100$ GB/s BW) |
| **GAP-13** | Random Seeding Scheme | **E. Correct implementation** | None (Code matches paper: `seed = baseSeed + trialIndex`) | Retain existing logic |
| **GAP-14** | Analytical Makespan Objective | **E. Correct implementation** | None (Code matches Equations 7–9: $\max_m \sum \frac{MI_k}{MIPS_m}$) | Retain existing logic |

---

## Detailed Gap Analyses

### GAP-01 & GAP-02: VM Count and MIPS Capacity Mismatch
- **Classification:** **B. Experimental configuration mismatch**
- **What the paper does:**
  - Table 1 (Page 7) specifies `VM: 10` and `Data Center MIPS: 1000`.
  - Section 3.3 (Page 16, Line 831–848) states: *"consists of 10 VMs, a single data center, and one physical host... Each VM in the cloud possesses a specific computational capacity, quantified in MIPS."*
  - The aggregate datacenter compute is 1000 MIPS across 10 VMs, giving $1000 / 10 = 100\text{ MIPS}$ per VM.
  - With 50 tasks of 300 MI, total instruction volume is $15,000\text{ MI}$. Across 10 VMs of 100 MIPS each, the theoretical lower bound makespan is:
    $$\frac{15,000\text{ MI}}{10 \times 100\text{ MIPS}} = 15.0\text{ seconds}$$
    This matches the paper's reported values in Table 4: GA-PSO ($15.877\text{ s}$), PSO ($16.453\text{ s}$), ABC ($18.435\text{ s}$), GA ($21.085\text{ s}$).
  - For 100 tasks of 300 MI, total instruction volume is $30,000\text{ MI}$. Theoretical lower bound is $30,000 / 1000 = 30.0\text{ s}$, which perfectly aligns with Table 4 Scenario 2: GA-PSO ($32.481\text{ s}$), PSO ($32.518\text{ s}$), GA ($39.604\text{ s}$), ABC ($43.007\text{ s}$).
- **What our code currently does:**
  - `UnifiedExperimentRunner.java` has `DEFAULT_VM_COUNT = 5`.
  - `FitnessEvaluator.java` has `public static final double VM_MIPS = 1000.0`.
  - `CloudSimScheduleAdapter.java` has `private static final int VM_MIPS = 1000`.
  - Total host capacity was $5 \times 1000 = 5000\text{ MIPS}$, resulting in Scenario 1 makespans of $\approx 3.0\text{ s}$ and Scenario 2 makespans of $\approx 6.0\text{ s}$ (off by roughly $5\times$ to $7\times$).
- **Why it matters:** This is the primary reason our previous numerical results were ~3s instead of the ~15–43s published in the paper.
- **Exact correction required:**
  1. Set default VM count to 10 across all runners (`UnifiedExperimentRunner`, `PaperScenarios`, `GARunExperiments`, `PSORunExperiments`).
  2. In `FitnessEvaluator.java`, change `VM_MIPS` to `100.0`.
  3. In `CloudSimScheduleAdapter.java`, change `VM_MIPS` to `100`.
  4. Ensure 1 Host in Datacenter has 10 PEs of 100 MIPS each (total 1000 MIPS).

---

### GAP-03: DDoS Workload Generation
- **Classification:** **B. Experimental configuration mismatch**
- **What the paper does:**
  - Section 3.2.1 (Page 13, Lines 676–681) states:
    > *"In our DDoS simulation, volumetric flooding was emulated purely by scaling cloudlet resource demands: each 'attack' cloudlet's execution length was multiplied by 100 and both its input and output file sizes were increased to 3000 MB (from 300 MB)."*
  - The paper specifies 0 attack percentages. There is no mention of 10% anywhere in the text.
  - Examining Tables 4, 5, and 6 reveals that the DDoS makespan across every single scenario and algorithm is approximately $98\times$ to $103\times$ the Normal makespan:
    - Scenario 1 GA: Normal $21.085\text{ s}$ vs DDoS $2026.626\text{ s}$ (ratio: $96.1\times$)
    - Scenario 1 PSO: Normal $16.453\text{ s}$ vs DDoS $1625.146\text{ s}$ (ratio: $98.8\times$)
    - Scenario 1 GA-PSO: Normal $15.877\text{ s}$ vs DDoS $1565.606\text{ s}$ (ratio: $98.6\times$)
    - Scenario 2 GA-PSO: Normal $32.481\text{ s}$ vs DDoS $3219.766\text{ s}$ (ratio: $99.1\times$)
    - Scenario 4 GA-PSO: Normal $28.415\text{ s}$ vs DDoS $2807.655\text{ s}$ (ratio: $98.8\times$)
    - Scenario 6 GA-PSO: Normal $27.954\text{ s}$ vs DDoS $2757.200\text{ s}$ (ratio: $98.6\times$)
  - Furthermore, Resource Utilization in Tables 7, 8, and 9 is virtually identical between Normal and DDoS (e.g. GA-PSO Scenario 1: 62.43% Normal vs 62.18% DDoS; PSO Scenario 1: 69.87% Normal vs 69.52% DDoS). This equality is mathematically only possible when all tasks are uniformly scaled by $100\times$.
- **What our code currently does:**
  - `WorkloadGenerator.java` introduced an undocumented constant: `DEFAULT_DDOS_ATTACK_RATIO = 0.10`.
  - In a 50-task scenario, only 5 tasks were scaled to $30,000\text{ MI}$, while the other 45 tasks remained $300\text{ MI}$.
  - This resulted in DDoS makespans of $\approx 33\text{ s}$ (instead of $\sim 1500–2000\text{ s}$) and for 100 tasks $\approx 66\text{ s}$ (instead of $\sim 2800–4200\text{ s}$).
- **Why it matters:** The previous workload generation produced DDoS makespans that were $50\times$ to $60\times$ lower than the paper, masking the volumetric flooding stress.
- **Exact correction required:**
  - In `WorkloadGenerator.java`, for the paper DDoS scenario, scale all cloudlets to $30,000\text{ MI}$ (length $\times 100$) and $3000\text{ MB}$ file/output size.

---

### GAP-04, GAP-05, GAP-06, GAP-10: Hybrid GA–PSO Architecture
- **Classification:** **C. Algorithmic difference from the paper**
- **What the paper does:**
  - **Dual Population Architecture (Algorithm 1, Algorithm 2, Algorithm 5):**
    - The hybrid maintains two separate populations: a GA population ($N_{GA}=75$) and a PSO swarm ($N_{PSO}=75$).
    - Both populations are initialized independently (`CreateRandomPopulation(75)` and `CreateRandomSwarm(75)`).
  - **Iteration Reduction (Section 3.1, Lines 420–423):**
    > *"For GAs, PSO, and the ABC, the number of iterations is denoted as n. However, to maintain computational efficiency and ensure a balanced comparison across methods, the hybrid GA–PSO algorithm adopts n/2 iterations."*
    - For $n=10$, Hybrid runs 5 iterations.
    - For $n=50$, Hybrid runs 25 iterations.
    - For $n=100$, Hybrid runs 50 iterations.
  - **Hybridization & Stagnation Trigger (Algorithm 3, Algorithm 5, lines 638–640):**
    - PSO particles update velocity and position; GA evolves via tournament selection, crossover, and mutation ($p_m = 0.0015$).
    - Stagnation check: If PSO has not improved by at least the stagnation threshold ($0.5$) over the stagnation count ($15$ iterations):
      $$\text{ReplaceWorstParticle}(PSO\_swarm, fittestGA)$$
    - Fittest GA individual replaces the worst particle in the PSO swarm.
    - Global best is updated: $gBest = \min(gBest, fittestGA)$.
- **What our code currently does:**
  - `HybridGaPsoScheduler.java` maintains only a **single swarm of 75 particles**.
  - In each iteration, it runs standard PSO velocity and position updates on all particles.
  - Then, it runs tournament selection and crossover between particles of the same swarm, with greedy acceptance.
  - It runs the full $n$ iterations (not $n/2$).
  - It has no separate GA population, no stagnation counter, no stagnation threshold ($0.5$), and no worst-particle injection.
  - Its mutation probability was $0.015$ instead of $0.0015$.
- **Why it matters:** The current implementation is an ad-hoc memetic PSO, not the co-evolutionary dual-population algorithm with stagnation-triggered migration proposed and highlighted in the paper's title and contributions.
- **Exact correction required:**
  - Completely rewrite `HybridGaPsoScheduler.java` to implement Algorithm 5 faithfully:
    1. Initialize `GA_pop` (75 individuals) and `PSO_swarm` (75 particles).
    2. Evaluate fitness for both.
    3. Loop for $n/2$ iterations.
    4. Update PSO velocities, positions, and fitness.
    5. Evolve GA population: tournament selection (size 5), crossover ($p_c = 0.5$), mutation ($p_m = 0.0015$).
    6. Check stagnation: track best PSO makespan. If stagnation count reaches 15 without improvement $> 0.5$, replace the particle with the highest makespan (worst) with the fittest GA individual.
    7. Update $gBest = \min(gBest, fittestGA)$.
    8. Return $gBest$ schedule.

---

### GAP-07: GA Stagnation Control
- **Classification:** **C. Algorithmic difference from the paper**
- **What the paper does:**
  - Table 2 (Page 9) and Section 3.3 (Lines 877–879) state:
    > *"Additionally, an enhancement was introduced by incorporating a stagnation threshold of 0.5 and a maximum stagnation count of 15. These modifications led to improved performance compared to the traditional GA..."*
- **What our code currently does:**
  - `GeneticAlgorithmScheduler.java` does not track stagnation and runs unchanged until the iteration limit.
- **Why it matters:** The paper explicitly mentions modifying traditional GA with stagnation control to improve performance.
- **Exact correction required:**
  - Introduce stagnation tracking in `GeneticAlgorithmScheduler.java`: if the best fitness does not improve by $>0.5$ over 15 generations, apply diversity restoration (e.g., partial population re-initialization or adaptive mutation boost) or document the exact stagnation mechanism.

---

### GAP-08: Resource Utilization Calculation
- **Classification:** **A. Confirmed implementation error**
- **What the paper does:**
  - Section 3.2.3, Equation 6 (Page 14):
    $$Resource\ Utilization\ (\%) = \frac{\sum_{m=1}^M TET_m}{Makespan \times M} \times 100\%$$
  - Reports Resource Utilization in Tables 7, 8, and 9 for all 4 algorithms across all 6 scenarios in both Normal and DDoS environments.
- **What our code currently does:**
  - Neither `ExperimentRunner`, `UnifiedExperimentRunner`, `ExperimentAnalyzer`, nor any result CSV files compute or record resource utilization.
- **Why it matters:** Resource utilization is one of the two primary performance metrics reported throughout the entire paper (alongside makespan). Its complete absence in the codebase is a critical omission.
- **Exact correction required:**
  - Add `calculateResourceUtilization(List<Task> tasks, Schedule schedule, int vmCount, double makespan)` to `FitnessEvaluator.java`.
  - Add `resource_utilization` column to raw experiment results CSV and summary CSV.
  - Update `ExperimentAnalyzer.java` to compute mean, min, max, and std of resource utilization.

---

### GAP-09: Standard Deviation Formula
- **Classification:** **A. Confirmed implementation error**
- **What the paper does:**
  - Section 3.2.3, Equation 5 (Page 14) explicitly defines standard deviation as:
    $$Std = \sqrt{\frac{1}{N}\sum_{i=1}^N (C_i - Avg)^2}$$
    (Population standard deviation, dividing by $N$).
- **What our code currently does:**
  - `ExperimentAnalyzer.java` uses sample standard deviation, dividing by $N-1$:
    `Math.sqrt(sumSquaredDiff / (n - 1))`
- **Why it matters:** When $N=10$, dividing by 9 instead of 10 causes a $\approx 5.4\%$ upward bias in the reported standard deviation.
- **Exact correction required:**
  - Update `ExperimentAnalyzer.java` to divide by $N$ matching Equation (5).

---

### GAP-11: ABC Parameter Alignment
- **Classification:** **D. Paper does not specify enough information**
- **What the paper does:**
  - Table 2 specifies: `Population size: 75`, `Food number: Cloudlet`, `LB: 0`, `UB: VMs`.
  - The paper notes that ABC exhibits low resource utilization (Tables 7–9) and static behavior under DDoS (Figures 5 & 6).
- **What our code currently does:**
  - `ArtificialBeeColonyScheduler.java` hardcodes `FOOD_SOURCES = 75` and `LIMIT = 20`.
- **Why it matters:** If `Food number = Cloudlet`, then for 50 tasks there are 50 food sources, and for 100 tasks there are 100 food sources.
- **Exact correction required:**
  - Set `foodSources = taskCount` dynamically as stated in Table 2, while preserving colony size 75. Document the abandonment limit ($20$) as an unstated paper parameter assumption.

---

### GAP-12: CloudSim Datacenter Resource Configuration
- **Classification:** **B. Experimental configuration mismatch**
- **What the paper does:**
  - Table 1 specifies Datacenter storage: 1 TB, Datacenter RAM: 100 GB, Datacenter BW: $1024 \times 100$ GB/s.
- **What our code currently does:**
  - Host RAM was set to $VM_{count} \times 1024\text{ MB} = 5120\text{ MB}$, Host BW to $5000$, and Storage to $51200\text{ MB}$.
- **Why it matters:** While Datacenter memory/bandwidth do not constrain CPU-bound makespan under TimeShared scheduling, configuring them to Table 1 ensures full parameter fidelity.
- **Exact correction required:**
  - Update `CloudSimScheduleAdapter.java` host parameters to match Table 1 directly.
