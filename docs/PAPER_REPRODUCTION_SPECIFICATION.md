# Paper Reproduction Specification

**Reference Paper:**  
*Performance Analysis of Cloud Computing Task Scheduling Using Metaheuristic Algorithms in DDoS and Normal Environments*  
**Authors:** Fatih Kaplan and Ahmet Babalik  
**Journal:** *Electronics* 2025, Volume 14, Issue 10, Article 1988  
**DOI:** [10.3390/electronics14101988](https://doi.org/10.3390/electronics14101988)

---

## 1. Problem Definition & Scope

- **Problem Being Solved:** Independent task scheduling in Infrastructure-as-a-Service (IaaS) cloud computing, mapping computational tasks (Cloudlets) to virtual machines (VMs) to minimize makespan ($C_{max}$) while evaluating resource utilization and execution stability under both normal operating conditions and high-stress DDoS attack conditions.
- **Complexity:** NP-hard combinatorial optimization problem (Section 1, Page 2; Section 3.1, Page 7).
- **Target Algorithms Evaluated:**
  1. Genetic Algorithm (GA)
  2. Particle Swarm Optimization (PSO)
  3. Artificial Bee Colony (ABC)
  4. Proposed Hybrid GA–PSO Algorithm

---

## 2. Experimental Environment & Cloud Infrastructure Specifications

| Parameter / Feature | Paper Specification Value | Paper Citation | Explicit or Inferred | Current Code Status | Current Code Value |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Simulation Framework** | CloudSim v3.0.3 (Java) | Sec 3, Page 6, Line 319 | Explicit | Follows paper | CloudSim 3.0.3 referenced |
| **Hardware Platform** | DELL Latitude 3540, Intel Core i5-1335U (1.30 GHz), 8 GB RAM, 500 GB SSD | Sec 3, Page 6, Lines 316–318 | Explicit | Informational (Host OS: Windows 11) | Local development machine |
| **Operating System** | Windows 11 Pro | Sec 3, Page 6, Line 318 | Explicit | Follows paper | Windows 11 |
| **Number of Data Centers** | 1 | Table 1, Page 7 | Explicit | Follows paper | 1 Data Center |
| **Data Center Storage** | 1 TB ($1,000,000$ MB) | Table 1, Page 7 | Explicit | Deviates | Host storage = $VM_{count} \times 10,240$ MB |
| **Data Center RAM** | 100 GB ($102,400$ MB) | Table 1, Page 7 | Explicit | Deviates | Host RAM = $VM_{count} \times 1,024$ MB |
| **Data Center Bandwidth** | $1024 \times 100$ GB/s | Table 1, Page 7 | Explicit | Deviates | Host BW = $VM_{count} \times 1,000$ |
| **Data Center OS / VMM** | Linux / XEN | Table 1, Page 7 | Explicit | Follows paper | Linux / Xen |
| **Data Center Total MIPS** | 1000 MIPS | Table 1, Page 7 | Explicit | Deviates | 5,000 MIPS (5 VMs $\times$ 1,000 MIPS) |
| **Number of Physical Hosts** | 1 physical host | Sec 3.3, Page 16, Line 832 | Explicit | Follows paper | 1 Host |
| **Host Processing Elements (PE)**| 10 PEs totaling 1000 MIPS (or 1 PE per VM @ 100 MIPS) | NOT EXPLICITLY STATED IN PAPER | Inferred from Table 1 & Sec 3.3 | Deviates | 5 PEs @ 1000 MIPS = 5000 MIPS |
| **Number of Virtual Machines** | 10 VMs | Table 1, Page 7; Sec 3.3, Line 831 | Explicit | **MISMATCH** | 5 VMs (`DEFAULT_VM_COUNT = 5`) |
| **VM Computational Capacity** | 100 MIPS per VM (Total 1000 MIPS / 10 VMs) | Table 1 (DC MIPS: 1000, VM: 10) & Sec 3.3 (Eq 7–9) | Inferred from DC capacity / VM count | **MISMATCH** | 1000 MIPS per VM (`VM_MIPS = 1000`) |
| **VM Storage Area** | 10 GB ($10,240$ MB) | Table 1, Page 7 | Explicit | Follows paper | 10,240 MB |
| **VM RAM** | 1 GB ($1,024$ MB) | Table 1, Page 7 | Explicit | Follows paper | 1,024 MB |
| **VM Bandwidth** | 1 GB/s | Table 1, Page 7 | Explicit | Follows paper | 1,000 |
| **VM Provisioning / Policy** | Time Shared (`VmSchedulerTimeShared`) | Table 1, Page 7 | Explicit | Follows paper | `VmSchedulerTimeShared` |
| **Cloudlet Scheduling Policy** | Time Shared (`CloudletSchedulerTimeShared`)| Table 1, Page 7; Sec 3.3 | Explicit | Follows paper | `CloudletSchedulerTimeShared` |
| **VM Allocation Policy** | Simple VM Allocation Policy | NOT SPECIFIED IN ORIGINAL PAPER | Inferred | Follows standard CloudSim | `VmAllocationPolicySimple` |

---

## 3. Workload & Task Specifications

| Parameter / Feature | Paper Specification Value | Paper Citation | Explicit or Inferred | Current Code Status | Current Code Value |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Task Count Scenarios** | 50 tasks and 100 tasks | Table 1, Table 3, Sec 3.3 | Explicit | Follows paper | 50 and 100 tasks |
| **Normal Cloudlet Length ($MI$)** | 300 MI | Table 1, Page 7, Line 364 | Explicit | Follows paper | 300 MI (`NORMAL_TASK_LENGTH_MI = 300`) |
| **Cloudlet Length Distribution** | Uniform distribution (nominally 300 MI baseline) | Sec 3.3, Page 16, Line 829 | Explicit | Follows paper | Constant 300 MI |
| **Normal Input File Size** | 300 MB | Sec 3.2.1, Page 13, Line 678 | Explicit | Follows paper | 300 MB |
| **Normal Output File Size** | 300 MB | Sec 3.2.1, Page 13, Line 678 | Explicit | Follows paper | 300 MB |
| **Number of PEs per Task** | 1 PE | Inferred from standard Cloudlet | Inferred | Follows paper | 1 PE |
| **DDoS Cloudlet Length ($MI$)** | $300 \times 100 = 30,000$ MI | Sec 3.2.1, Page 13, Line 677 | Explicit | Follows paper | 30,000 MI |
| **DDoS Input File Size** | 3000 MB | Sec 3.2.1, Page 13, Line 678 | Explicit | Follows paper | 3000 MB |
| **DDoS Output File Size** | 3000 MB | Sec 3.2.1, Page 13, Line 678 | Explicit | Follows paper | 3000 MB |
| **Attack-Task Percentage** | NOT SPECIFIED IN ORIGINAL PAPER (0 mentions of attack percentage in paper) | Whole paper audit | Unspecified | **MISMATCH** | 10% attack tasks (`DEFAULT_DDOS_ATTACK_RATIO = 0.10`) |
| **DDoS Workload Flooding Model**| All cloudlets in DDoS scenario scaled to 30,000 MI & 3000 MB (volumetric flood scaling) | Sec 3.2.1, Line 676–678; Tables 4–9 | Inferred from ~100$\times$ makespan & equal RU | **MISMATCH** | 10% ratio caused DDoS makespan of 33s instead of ~1500s |
| **Task Arrival Time** | Static / Batch arrival at time $t=0$ | Sec 3.2.3, Eq 1, 7–9 | Explicit | Follows paper | All submitted at $t=0$ |
| **Task Dependencies** | None (Independent tasks) | Sec 2, Ref [15], Sec 3.3 | Explicit | Follows paper | Independent tasks |

---

## 4. Optimization Algorithms & Parameter Configurations

### 4.1 Genetic Algorithm (GA)

| Parameter | Paper Specification | Paper Citation | Status in Current Code | Current Code Value |
| :--- | :--- | :--- | :--- | :--- |
| **Population Size** | 75 | Table 2, Page 9 | Follows paper | 75 |
| **Crossover Rate ($p_c$)** | 0.5 | Table 2, Page 9 | Follows paper | 0.5 (One-point crossover) |
| **Mutation Rate ($p_m$)** | 0.015 | Table 2, Page 9 | Follows paper | 0.015 (Uniform mutation) |
| **Elitism** | Enabled (Yes) | Table 2, Page 9 | Follows paper | Yes (1 best preserved) |
| **Tournament Size** | 5 (Table 2) / 15 (Text line 876) | Table 2 vs Sec 3.3 | Follows Table 2 | 5 |
| **Stagnation Threshold** | 0.5 | Table 2, Page 9 | **MISSING** | Not implemented in GA |
| **Stagnation Count** | 15 | Table 2, Page 9 | **MISSING** | Not implemented in GA |
| **Encoding** | Discrete integer vector: chromosome$[i] = VM_{id}$ | Sec 3.1, Page 8 | Follows paper | `int[taskCount]` |
| **Selection Method** | Tournament Selection | Table 2 & Sec 3.1.3 | Follows paper | Tournament Selection |
| **Crossover Operator** | Single-point / standard crossover | Sec 3.1.3, Line 561 | Follows paper | One-point crossover |
| **Mutation Operator** | Gene-level random VM reassignment | Sec 3.1.3, Line 562 | Follows paper | Gene mutation with $p_m$ |

### 4.2 Particle Swarm Optimization (PSO)

| Parameter | Paper Specification | Paper Citation | Status in Current Code | Current Code Value |
| :--- | :--- | :--- | :--- | :--- |
| **Swarm Size** | 75 particles | Table 2, Page 9 | Follows paper | 75 |
| **Inertia Factor ($w$)** | 0.9 | Table 2, Page 9 | Follows paper | 0.9 (Constant) |
| **Cognitive Parameter ($c_1$)**| 1.5 | Table 2, Page 9 | Follows paper | 1.5 |
| **Social Parameter ($c_2$)** | 1.5 | Table 2, Page 9 | Follows paper | 1.5 |
| **Position Mapping** | Discrete integer VM indices $[0, VM-1]$ | Sec 3.1.1, Sec 3.3 | Follows paper | $x = \text{clamp}(\text{round}(x+v), 0, VM-1)$ |
| **Velocity Clamping** | NOT SPECIFIED IN ORIGINAL PAPER | Literature default | Assumption | Clamped to $[-9, 9]$ ($v_{max} = VM-1$) |
| **Initial Velocity** | NOT SPECIFIED IN ORIGINAL PAPER | Literature default | Assumption | Uniform in $[-1.0, 1.0]$ |

### 4.3 Artificial Bee Colony (ABC)

| Parameter | Paper Specification | Paper Citation | Status in Current Code | Current Code Value |
| :--- | :--- | :--- | :--- | :--- |
| **Colony Size / Population** | 75 | Table 2, Page 9 | Follows paper | 75 |
| **Food Number** | Cloudlet (50 or 100) | Table 2, Page 9 | Deviates | Fixed 75 (`FOOD_SOURCES = 75`) |
| **Lower Bound (LB)** | 0 | Table 2, Page 9 | Follows paper | 0 |
| **Upper Bound (UB)** | VMs (10) | Table 2, Page 9 | Follows paper | 10 ($VM_{count}$) |
| **Abandonment Limit** | NOT SPECIFIED IN ORIGINAL PAPER | Literature default | Assumption | 20 (`LIMIT = 20`) |
| **Employed Bee Phase** | Perturbation via difference vector | Sec 1, 3.1 | Follows literature | One-task difference perturbation |
| **Onlooker Bee Phase** | Roulette wheel selection based on fitness | Sec 1, 3.1 | Follows literature | $fit = \frac{1}{1 + makespan}$ roulette |
| **Scout Bee Phase** | Random reinitialization upon exceeding limit | Sec 1, 3.1 | Follows literature | Random reinitialization |

### 4.4 Proposed Hybrid GA–PSO Algorithm

| Parameter / Step | Paper Specification | Paper Citation | Status in Current Code | Current Code Value |
| :--- | :--- | :--- | :--- | :--- |
| **Architecture** | Dual-population co-evolution: separate GA population ($N=75$) and separate PSO swarm ($N=75$) | Sec 3.1, Alg 1, Alg 2, Alg 5 | **MAJOR MISMATCH** | Single swarm memetic / local search |
| **Iterations** | $n/2$ iterations in total (e.g., 5, 25, 50 iterations) | Sec 3.1, Page 8, Lines 420–423 | **MAJOR MISMATCH** | Full $n$ iterations (10, 50, 100) |
| **GA Population Size** | 75 | Table 2, Page 9 | Missing | Only 1 swarm of 75 |
| **PSO Swarm Size** | 75 | Table 2, Page 9 | Follows paper | 75 particles |
| **Crossover Rate ($p_c$)** | 0.5 | Table 2, Page 9 | Follows paper | 0.5 |
| **Mutation Rate ($p_m$)** | 0.0015 (Note: 10$\times$ smaller than pure GA) | Table 2, Page 9 | Deviates | 0.015 (`MUTATION_PROBABILITY = 0.015`) |
| **Elitism** | Enabled (Yes) | Table 2, Page 9 | Follows paper | Elitism in GA |
| **Tournament Size** | 5 | Table 2, Page 9 | Follows paper | 5 |
| **Inertia Factor ($w$)** | 0.9 | Table 2, Page 9 | Follows paper | 0.9 |
| **$c_1$ and $c_2$** | 1.5 and 1.5 | Table 2, Page 9 | Follows paper | 1.5 and 1.5 |
| **Stagnation Threshold** | 0.5 | Table 2, Page 9; Alg 5 | **MISSING** | No stagnation detection |
| **Stagnation Count** | 15 | Table 2, Page 9; Alg 3 | **MISSING** | No stagnation counter |
| **Hybridization Trigger** | When PSO is stagnant ($\Delta \text{best} < 0.5$ for 15 iters) | Sec 3.1.2, Alg 3, Alg 5 | **MISSING** | Greedy crossover on every particle every iter |
| **Hybridization Operation**| Inject fittest GA individual to replace worst PSO particle | Sec 3.1, Alg 1, Alg 5 (Line 639)| **MISSING** | In-place replacement |
| **Global Best Update** | $gBest = \min(gBest, fittestGA)$ | Alg 5, Line 641 | Partial | PSO gBest tracking |

---

## 5. Performance Metrics & Mathematical Definitions

### 5.1 Makespan ($C_{max}$)
- **Analytical Execution Time per Task $k$ on VM $m$:**
  $$ET_{km} = \frac{MI_k}{MIPS_m}$$
  *(Equation 7, Page 17)*
- **Total Execution Time on VM $m$:**
  $$TET_m = \sum_{k \in T_m} ET_{km}$$
  *(Equation 8, Page 17)*
- **Optimization Objective (Makespan):**
  $$Makespan = \max_{m \in \{VM_1, \dots, VM_M\}} TET_m$$
  *(Equation 9, Page 17)*
- **CloudSim Simulation Makespan:**
  $$C_{max} = T_{finish\_max} - T_{start\_min}$$
  *(Equation 1, Page 14)*
- **Consistency between Optimization & Simulation:**
  In CloudSim with `CloudletSchedulerTimeShared` and homogeneous VMs, when tasks are released at $t=0$, analytical makespan and simulation makespan evaluate the exact same completion timeline. Candidate evaluations during heuristic search evaluate the objective function (Eq 9), and the resulting optimal schedule is executed inside CloudSim to verify and record simulated completion timestamps.

### 5.2 Resource Utilization ($RU$)
- **Formula:**
  $$Resource\ Utilization\ (\%) = \frac{\sum_{m=1}^M TET_m}{Makespan \times M} \times 100\%$$
  *(Equation 6, Page 14 — "$\frac{\sum(VM\ time\ to\ life)}{Makespan \times total\ VM\ number}$")*
- **Status in Current Code:** **Completely absent from calculations and CSV output.**

### 5.3 Computational Execution Time
- **Definition:** Optimization run time of the heuristic algorithm on the host CPU.
- **Reporting Format:** Elapsed wall-clock time formatted as `HH:MM:SS.ss` (Tables 10–12).
- **Status in Current Code:** Recorded in milliseconds, not formatted or compared against Tables 10–12.

### 5.4 Statistical Summary Indicators
- **Minimum Makespan ($C_{min}$):** $\min \{C_1, \dots, C_N\}$ (Equation 2, Page 14)
- **Maximum Makespan ($C_{max}$):** $\max \{C_1, \dots, C_N\}$ (Equation 3, Page 14)
- **Average Makespan ($Avg$):** $\frac{1}{N} \sum_{i=1}^N C_i$ (Equation 4, Page 14)
- **Standard Deviation ($Std$):**
  $$Std = \sqrt{\frac{1}{N} \sum_{i=1}^N (C_i - Avg)^2}$$
  *(Equation 5, Page 14 — Population standard deviation with division by $N$)*
- **Status in Current Code:** Currently uses sample standard deviation ($N-1$).

---

## 6. Simulation Scenarios Matrix

Table 3 in the original paper defines the 6 core scenarios, each executed under two environments: **Normal** and **DDoS**, across 10 independent trials ($N=10$):

| Scenario | Iterations ($n$) | Tasks ($K$) | VMs ($M$) | Trials ($N$) | Hybrid GA-PSO Iterations ($n/2$) |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **Scenario 1** | 10 | 50 | 10 | 10 | 5 |
| **Scenario 2** | 10 | 100 | 10 | 10 | 5 |
| **Scenario 3** | 50 | 50 | 10 | 10 | 25 |
| **Scenario 4** | 50 | 100 | 10 | 10 | 25 |
| **Scenario 5** | 100 | 50 | 10 | 10 | 50 |
| **Scenario 6** | 100 | 100 | 10 | 10 | 50 |

Total experiment runs: $6 \text{ scenarios} \times 2 \text{ environments (Normal/DDoS)} \times 4 \text{ algorithms} \times 10 \text{ trials} = 480 \text{ runs}$.

---

## 7. Randomization & Reproducibility Protocol

- **Base Seed:** Fixed base seed = 0 (Sec 3, Page 6, Line 324).
- **Independent Run Seeding:** $Seed = BaseSeed + RepetitionIndex$ (where $RepetitionIndex \in \{0, 1, \dots, 9\}$) (Sec 3, Page 6, Lines 325–328).
- **Status in Current Code:** Already follows this exact seeding scheme (`seed = BASE_SEED + trialIndex`).
