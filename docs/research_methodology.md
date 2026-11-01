# Research Methodology and Problem Formulation

## 1. Document Status and Source Paper Inspection

> [!IMPORTANT]
> **Source Paper Availability Notice:**
> An exhaustive search of the repository root, `docs/`, and related directories identified no standalone PDF, Word document, or raw publication text.
> The research specifications documented below are extracted directly from authoritative codebase references (including commit history, comments in `ResearchCloudSimEnvironment.java`, references to **Table 3** in `WorkloadGeneratorTest.java` and `PaperScenarios.java`, and the mathematical definitions embedded in `FitnessEvaluator.java`).
> Where assumptions or extensions are made to complete the system, they are explicitly tagged as **[Implementation Choice / Unsupported Assumption]**.

---

## 2. Research Objective

The primary research objective is the optimization of independent task scheduling onto heterogeneous or homogeneous cloud virtual machines (VMs) to minimize execution time (**makespan**) under both **normal operating conditions** and **synthetic DDoS attack-burst workloads**.

The study evaluates and compares population-based and swarm-based metaheuristics:
1. **Genetic Algorithm (GA)**
2. **Particle Swarm Optimization (PSO)**
3. **Artificial Bee Colony (ABC)** (metaheuristic baseline/comparator)
4. **Hybrid GA-PSO** (collaborative search combining genetic operators with swarm momentum)

---

## 3. Problem Formulation & System Model

### 3.1 Task and Virtual Machine Model
- Let $T = \{t_0, t_1, \dots, t_{N-1}\}$ be a set of $N$ independent, non-preemptive computational tasks.
- Each task $t_k$ has:
  - Instruction length $MI_k$ (in Million Instructions, MI).
  - Input file size $FileIn_k$ (in MB).
  - Output file size $FileOut_k$ (in MB).
- Let $VM = \{vm_0, vm_1, \dots, vm_{M-1}\}$ be a set of $M$ virtual machines.
- Each virtual machine $vm_m$ has:
  - Processing capability $MIPS_m$ (Million Instructions Per Second).
  - Memory capacity $RAM_m$ (in MB).
  - Storage $Storage_m$ (in MB).
  - Bandwidth $BW_m$ (in Mbps).
  - Processing elements $PE_m = 1$.

### 3.2 Parameter Baseline from Code & Table 3 References

| Entity | Parameter | Documented Value | Source / Traceability |
| :--- | :--- | :--- | :--- |
| **Datacenter** | Processing Speed | 1,000 MIPS | `ResearchCloudSimEnvironment.java:34-36` |
| **VM** | Processing Speed ($MIPS_m$) | 1,000 MIPS | `FitnessEvaluator.java:7`, `CloudSimScheduleAdapter.java:32` |
| **VM** | Memory ($RAM_m$) | 1,024 MB | `CloudSimScheduleAdapter.java:29` |
| **VM** | Bandwidth ($BW_m$) | 1,000 Mbps | `CloudSimScheduleAdapter.java:31` |
| **VM** | Storage | 10,240 MB (10 GB) | `CloudSimScheduleAdapter.java:30` |
| **VM Count** | $M$ | 5 (Experiments) / 10 (Env) | `GARunExperiments.java:11`, `ResearchCloudSimEnvironment.java:29` |
| **Normal Task** | Length ($MI$) | 300 MI | `WorkloadGenerator.java:9` |
| **Normal Task** | File Size ($In/Out$) | 300 MB | `WorkloadGenerator.java:10` |
| **DDoS Attack Task** | Length ($MI$) | 30,000 MI ($100\times$) | `WorkloadGenerator.java:11` |
| **DDoS Attack Task** | File Size ($In/Out$) | 3,000 MB ($10\times$) | `WorkloadGenerator.java:12` |

---

## 4. Optimization Objective & Makespan Definition

### 4.1 Mathematical Execution Time Model
The expected execution time $ET(k, m)$ of task $t_k$ on virtual machine $vm_m$ is defined as:
$$ET(k, m) = \frac{MI_k}{MIPS_m}$$

For homogeneous VMs where $MIPS_m = 1000\text{ MIPS}$ and normal tasks $MI_k = 300\text{ MI}$:
$$ET(k, m) = \frac{300}{1000} = 0.30\text{ seconds}$$

### 4.2 Cumulative VM Load and Makespan
Let $S: T \to VM$ represent a schedule mapping each task $t_k$ to a specific $vm_{S(k)}$. The cumulative completion time (load) of virtual machine $vm_m$ is:
$$L_m = \sum_{k: S(k) = m} ET(k, m) = \sum_{k: S(k) = m} \frac{MI_k}{MIPS_m}$$

The optimization objective is to minimize the **Makespan** ($C_{max}$), defined as the maximum completion time across all virtual machines:
$$C_{max} = \max_{m \in \{0, \dots, M-1\}} L_m$$

Subject to constraints:
1. **Single Assignment:** Every task $t_k$ is assigned to exactly one virtual machine $vm_m$:
   $$\forall k \in \{0, \dots, N-1\}, \quad S(k) \in \{0, \dots, M-1\}$$
2. **Non-preemption:** Tasks execute without interruption until completion.

### 4.3 Analytical Makespan vs. CloudSim Simulation Makespan
- **Analytical Makespan:** Calculated strictly using the cumulative execution formula $\max_m \sum \frac{MI_k}{MIPS_m}$.
- **CloudSim Simulation Makespan:** Measured by instantiating CloudSim datacenter broker entities, submitting Cloudlets and VMs, executing `CloudSim.startSimulation()`, and observing:
  $$\text{Observed Makespan} = \max_k (\text{finishTime}_k) - \min_k (\text{startTime}_k)$$
  Under `CloudletSchedulerTimeShared` with homogeneous single-PE VMs, these two metrics converge within machine floating-point tolerance ($10^{-7}$).

---

## 5. Workload Definitions: Normal vs. Synthetic DDoS

### 5.1 Normal Workload
All tasks are identical standard cloud computing requests:
- Task Length: $300\text{ MI}$
- Input / Output Data: $300\text{ MB}$

### 5.2 Synthetic DDoS Workload vs. Real Network Attack
> [!NOTE]
> **Methodology Distinction:**
> The workload generator models a **synthetic attack-like workload** where target attack tasks experience extreme resource amplification ($100\times$ CPU instruction length of $30,000\text{ MI}$, and $10\times$ I/O transfer volume of $3,000\text{ MB}$).
> This models the resource-exhaustion impact of volumetric attacks on the scheduler, rather than low-level packet flood networking (e.g. TCP SYN/UDP flooding at the IP layer).

In DDoS scenarios, an attack fraction (default 10% of total tasks, matching typical research benchmark configurations) is selected deterministically.

---

## 6. Experimental Matrix (Table 3 Contract)

The repository explicitly enforces the experimental matrix cited as **Table 3** in `WorkloadGeneratorTest.java` and `PaperScenarios.java`:

| Scenario ID | Iteration Budget | Task Count ($N$) | Trials | Workload Types Tested |
| :---: | :---: | :---: | :---: | :---: |
| **Scenario 1** | 10 | 50 | 10 | Normal, DDoS |
| **Scenario 2** | 10 | 100 | 10 | Normal, DDoS |
| **Scenario 3** | 50 | 50 | 10 | Normal, DDoS |
| **Scenario 4** | 50 | 100 | 10 | Normal, DDoS |
| **Scenario 5** | 100 | 50 | 10 | Normal, DDoS |
| **Scenario 6** | 100 | 100 | 10 | Normal, DDoS |

- Total matched trial runs per algorithm: $6 \text{ scenarios} \times 2 \text{ workloads} \times 10 \text{ trials} = 120\text{ runs}$.
- With 4 algorithms (GA, PSO, ABC, Hybrid GA-PSO), the complete experimental execution consists of $4 \times 120 = 480\text{ runs}$.

---

## 7. Algorithm Specifications and Parameters

### 7.1 Genetic Algorithm (GA)
Documented in `GeneticAlgorithmScheduler.java`:
- **Representation:** Integer chromosome of length $N$, where index $k$ holds $vmId \in \{0, \dots, M-1\}$.
- **Population Size ($P$):** 75
- **Selection:** Tournament selection with Tournament Size $K = 5$.
- **Crossover:** One-point crossover with probability $P_c = 0.5$.
- **Mutation:** Random gene reset with probability $P_m = 0.015$.
- **Elitism:** 1 best chromosome unconditionally preserved to the next generation.

### 7.2 Particle Swarm Optimization (PSO)
Documented in `ParticleSwarmScheduler.java`:
- **Representation:** Particle position $X_i = (x_{i, 0}, \dots, x_{i, N-1})$, velocity $V_i = (v_{i, 0}, \dots, v_{i, N-1})$.
- **Swarm Size ($S$):** 75
- **Inertia Weight ($w$):** 0.9
- **Cognitive Coefficient ($c_1$):** 1.5
- **Social Coefficient ($c_2$):** 1.5
- **Velocity Update:**
  $$v_{i, d}^{(t+1)} = w \cdot v_{i, d}^{(t)} + c_1 r_1 (pbest_{i, d} - x_{i, d}^{(t)}) + c_2 r_2 (gbest_d - x_{i, d}^{(t)})$$
- **Velocity Clamping:** $v_{i, d} \in [-V_{max}, V_{max}]$, with $V_{max} = M - 1$.
- **Position Update and Mapping:**
  $$x_{i, d}^{(t+1)} = \text{clamp}\Big(\text{round}\big(x_{i, d}^{(t)} + v_{i, d}^{(t+1)}\big), 0, M - 1\Big)$$

### 7.3 Artificial Bee Colony (ABC) [Proposed Implementation]
- **Colony Size:** 75 (equal to GA and PSO populations for fair evaluation).
- **Food Sources ($SN$):** 38 food sources.
- **Employed Bees:** Search neighborhood $v_{i, d} = x_{i, d} + \phi_{i, d}(x_{i, d} - x_{k, d})$.
- **Onlooker Bees:** Roulette-wheel selection based on fitness $1 / (1 + \text{makespan})$.
- **Scout Bees:** Abandonment threshold $limit = 20$. When a food source cannot be improved for $limit$ trials, the scout reinitializes it with a random valid schedule.

### 7.4 Hybrid GA-PSO [Proposed Implementation]
- **Population/Swarm Size:** 75
- Combines PSO continuous trajectory velocity exploration with GA genetic crossover to prevent stagnation in local minima.
- In each generation, particles update their position using PSO velocity and personal/global bests, followed by tournament-based crossover and mutation on sub-optimal particles.

---

## 8. Random Seed and Fair Comparison Policy

- To guarantee scientific reproducibility and fair comparison, all algorithms evaluated on Scenario $S$ and Trial $T$ receive:
  1. The exact same random seed $seed = \text{BASE\_SEED} + \text{trialIndex}$.
  2. The exact same task set (identical lengths, file sizes, and attack designations).
- The default seed policy uses $\text{BASE\_SEED} = 0$, yielding deterministic seeds $0, 1, \dots, 9$ for the 10 trials of each scenario.
