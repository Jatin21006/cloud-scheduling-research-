# Research Design Document (Revised v2.0): Chaos-Enhanced Particle Swarm Optimization (Chaos-PSO) for Cloud Task Scheduling

**Document Version:** 2.0 (Peer-Review Revised)  
**Project:** Cloud Task Scheduling Research Extension  
**Base Study:** Kaplan & Babalik, *Electronics* 2025, 14, 1988 ([DOI: 10.3390/electronics14101988](https://doi.org/10.3390/electronics14101988))  
**Proposed Extension:** Logistic Map Chaotic Inertia Weight Modulation in Discrete PSO  
**Status:** Under Final Design Review (Phase 2)

---

## 1. Executive Summary & Research Motivation

In the reproduction of Kaplan & Babalik (2025), canonical Particle Swarm Optimization (PSO) demonstrated rapid initial convergence but was susceptible to stagnation in local optima, particularly in large search spaces (e.g., 100 tasks on 10 VMs: search space cardinality $10^{100}$). This behavior is closely tied to the **constant inertia weight** ($w = 0.9$) and pseudorandom perturbation parameters ($r_1, r_2 \sim U(0, 1)$).

The original paper explicitly suggested **chaos-based optimization** as a potential direction for future research (Section 4). This extension investigates the integration of chaotic dynamics via the **one-dimensional Logistic Map** to modulate the PSO inertia weight dynamically throughout the optimization process.

To evaluate whether observed performance variations are truly due to **chaotic ergodicity** rather than merely the effect of **dynamic/varying inertia weights**, our experimental framework establishes a three-way comparative study:
1. **Canonical Baseline PSO:** Fixed inertia weight $w = 0.9$ (Kaplan & Babalik, 2025 reproduction).
2. **Linearly Decreasing Inertia Weight PSO (LDIW-PSO):** Non-chaotic linear dynamic control $w(t) = w_{max} - \frac{t}{T}(w_{max} - w_{min})$ (Shi & Eberhart, 1998 benchmark).
3. **Chaos-Enhanced PSO (Chaos-PSO):** Non-linear chaotic dynamic modulation $w(t) = w_{min} + (w_{max} - w_{min}) \cdot x(t)$ driven by the Logistic Map.

---

## 2. Mathematical Formulation & Rigorous Analysis of the Logistic Map

### 2.1 The Logistic Map Formulation

The classical 1D Logistic Map is defined by the non-linear recurrence relation:

$$x(t+1) = \mu \cdot x(t) \cdot \big(1 - x(t)\big)$$

When the control parameter is set to $\mu = 4.0$, the map operates in the fully developed chaotic regime.

### 2.2 Mathematical Realities, Singularities, and Finite-Precision Limitations

It is critical to avoid overstated theoretical claims regarding chaos and ergodicity in digital implementations:

1. **Not All Initial States are Chaotic:**
   - **Fixed Points:**
     - $x^* = 0.0 \implies x(t) = 0.0, \; \forall t \ge 0$.
     - $x^* = 0.75 \implies 4.0 \times 0.75 \times (1 - 0.75) = 4.0 \times 0.75 \times 0.25 = 0.75$ (stationary fixed point).
   - **Pre-Images of Fixed Points (Extinction & Periodic Cycles):**
     - $x(0) = 0.50 \implies x(1) = 4.0 \times 0.50 \times 0.50 = 1.0 \implies x(2) = 0.0$ (rapid extinction to zero).
     - $x(0) = 0.25 \implies x(1) = 4.0 \times 0.25 \times 0.75 = 0.75$ (collapses immediately into the fixed point $0.75$).
     - Analytical rational cycles: Any initial condition of the form $x(0) = \sin^2(k \pi / 2^m)$ will eventually collapse to 0 or enter a short periodic cycle.

2. **Finite-Precision Digital Limitations (IEEE 754 Floating-Point):**
   - On a digital computer using 64-bit double precision, the state space is finite ($2^{64}$ states). True continuous ergodicity is impossible.
   - Due to floating-point truncation and round-off errors, chaotic orbits will eventually become periodic (pseudo-chaos) or can slowly drift toward 0.0 or fixed cycles over very long time horizons.

3. **Engineering Mitigation and Sequence Validation:**
   - **Deterministic Validated Initialization:** 
     We select a well-characterized irrational-approximated starting seed:
     $$x(0) = 0.70$$
     *(Note: $0.70$ is well outside the neighborhoods of $0.0$, $0.25$, $0.50$, $0.75$, and $1.0$).*
     Alternatively, if derived from trial seed $S$, we set:
     $$x(0) = 0.15 + 0.70 \cdot \left(\frac{(S \times 1103515245L + 12345L) \pmod{2^{31}}}{2^{31}}\right)$$
     with explicit exclusion guards: if $|x(0) - k| < 0.01$ for $k \in \{0.0, 0.25, 0.50, 0.75, 1.0\}$, nudge $x(0) \leftarrow x(0) + 0.03$.
   - **Runtime Safeguards:**
     During the iteration loop, if numerical drift causes $x(t) \le 10^{-6}$ or $x(t) \ge 1.0 - 10^{-6}$ or $|x(t) - x(t-1)| < 10^{-6}$ (stagnation), the chaotic variable is automatically re-seeded to $0.70 + 0.01 \cdot (t \pmod{17})$ to guarantee continuous search perturbation without algorithmic failure.

---

### 2.3 Inertia Weight Modulation Schemes

#### Scheme 1: Baseline Canonical PSO (Reproduced from Paper)
$$w(t) = 0.9 \quad (\text{constant for all } t)$$

#### Scheme 2: Linearly Decreasing Inertia Weight (LDIW-PSO Control)
$$w_{LDIW}(t) = w_{max} - \left(\frac{t}{T}\right) \cdot (w_{max} - w_{min})$$
- Linearly transitions from global exploration ($w_{max} = 0.9$) at $t = 0$ to fine-grained exploitation ($w_{min} = 0.4$) at $t = T$.
- Serves as the control to measure how much improvement is purely due to decreasing inertia.

#### Scheme 3: Chaos-Enhanced PSO (Chaos-PSO)
$$w_{chaos}(t) = w_{min} + (w_{max} - w_{min}) \cdot x(t)$$
- Bound selection: $w_{min} = 0.4$, $w_{max} = 0.9$.
- Parameter Rationale:
  - $w_{max} = 0.9$ preserves the maximum exploration momentum of the original paper.
  - $w_{min} = 0.4$ provides substantial damping, allowing personal and social cognitive attractors to perform refined local scheduling.
  - The chaotic sequence $x(t) \in (0, 1)$ provides non-linear oscillations across $[0.4, 0.9]$, allowing particles to break free from local optima even in later iterations.

---

### 2.4 Discrete Problem Mapping & Assignment Validity

1. **Velocity Computation:**
   $$v_{p, k}^{(t+1)} = w(t) \cdot v_{p, k}^{(t)} + c_1 \cdot r_1 \cdot (pbest_{p, k} - x_{p, k}^{(t)}) + c_2 \cdot r_2 \cdot (gbest_k - x_{p, k}^{(t)})$$
   with $c_1 = 1.5, c_2 = 1.5$, and $r_1, r_2 \sim U(0, 1)$.

2. **Velocity Clamping:**
   $$v_{p, k}^{(t+1)} \leftarrow \max\Big(-v_{max}, \min\big(v_{max}, v_{p, k}^{(t+1)}\big)\Big), \quad v_{max} = VM_{count} - 1 = 9$$

3. **Discrete Position Update:**
   $$x_{p, k}^{(t+1)} = \max\Big(0, \min\big(VM_{count} - 1, x_{p, k}^{(t)} + \text{round}(v_{p, k}^{(t+1)})\big)\Big)$$

4. **Validity Guarantee:**
   Because each task $k \in \{0, \dots, K-1\}$ is assigned independently to $x_{p, k} \in \{0, \dots, M-1\}$, the resulting schedule represents a total surjective/injective mapping where:
   - Every task is assigned to exactly one valid VM ID ($0 \le m \le 9$).
   - Boundary checks via `Math.max(0, Math.min(vmCount - 1, ...))` strictly prevent `IndexOutOfBoundsException` or invalid VM IDs.

---

## 3. Preserving the Original Baseline & Telemetry Architecture

To ensure documentation and experimental integrity:

1. **`ParticleSwarmScheduler.java` Remains Completely Untouched:**
   - The file [`src/core/ParticleSwarmScheduler.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/ParticleSwarmScheduler.java) will **NOT** be modified.
   - All existing regression tests ([`ParticleSwarmSchedulerTest.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/ParticleSwarmSchedulerTest.java)) will continue running against the untouched file.

2. **Zero-Impact Convergence Telemetry Architecture:**
   - Rather than altering the baseline class, we create:
     - `src/core/ChaosParticleSwarmScheduler.java`: Implements `SchedulingAlgorithm`, incorporates Logistic Map inertia modulation, and provides an internal telemetry accessor `getConvergenceHistory()`.
     - `src/core/LdiwParticleSwarmScheduler.java`: Implements `SchedulingAlgorithm`, incorporates linear decreasing inertia weight, and provides `getConvergenceHistory()`.
     - `src/core/InstrumentedBaselinePsoScheduler.java`: A separate helper class that mirrors the exact execution logic of `ParticleSwarmScheduler` byte-for-byte, adding only the per-iteration best makespan array for convergence plotting.
   - In [`UnifiedExperimentRunner.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/UnifiedExperimentRunner.java), the original `ParticleSwarmScheduler` instance is invoked directly, ensuring that baseline results are strictly identical to the paper reproduction.

---

## 4. Experimental Fairness & Random Seed Policy

### 4.1 Reproducibility vs. Random Sequence Alignment

- **What Controlled Seeds Guarantee:**
  - Running a trial with `seed = BASE_SEED + trialIndex` guarantees that the generated task workload (task lengths, input/output file sizes) is **identical** across all algorithms.
  - The initial random positions and initial velocities of the particles are generated identically across all PSO variants when using the same seed for initialization.
- **What Seeds Do Not Guarantee:**
  - As iterations proceed, different velocity dynamics and chaotic map updates cause the number and order of subsequent pseudo-random number generator calls to diverge.
  - Therefore, matching seeds guarantees **identical experimental conditions and repeatable trials**, but does **not** imply that particles follow identical random perturbation trajectories across different algorithm variants.

---

## 5. Experimental Matrix & Multi-Algorithm Comparison

### 5.1 The Experimental Matrix

All evaluations use the exact CloudSim v3.0.3 environment defined in Paper Reproduction Specification:
- 1 Datacenter, 1 Host, 10 VMs @ 100 MIPS each ($M = 10$).
- 2 Workload Types: Normal ($300\text{ MI}$) and DDoS ($30,000\text{ MI}$).
- 6 Scenarios (Table 3):
  - Scenario 1: 50 tasks, 10 iterations, 10 trials
  - Scenario 2: 100 tasks, 10 iterations, 10 trials
  - Scenario 3: 50 tasks, 50 iterations, 10 trials
  - Scenario 4: 100 tasks, 50 iterations, 10 trials
  - Scenario 5: 50 tasks, 100 iterations, 10 trials
  - Scenario 6: 100 tasks, 100 iterations, 10 trials
- Total trial executions across 3 variants: $6 \text{ scenarios} \times 2 \text{ workloads} \times 3 \text{ algorithms} \times 10 \text{ trials} = 360 \text{ runs}$.

### 5.2 Metrics Evaluated

1. **Analytical Makespan ($C_{max}$):** $\max_{m} \sum_{k \in T_m} \frac{MI_k}{MIPS_m}$.
2. **CloudSim Observed Makespan:** Verification inside CloudSim simulation engine.
3. **Resource Utilization ($RU$):** $\frac{\sum TET_m}{Makespan \times M} \times 100\%$.
4. **Convergence Curve:** Best makespan recorded after each iteration $t \in [0, T]$.
5. **Execution Time:** Algorithm optimization time in milliseconds.
6. **Relative Improvement Formulation:**
   $$\text{Improvement Over Baseline PSO (\%)} = \frac{\text{Makespan}_{\text{PSO}} - \text{Makespan}_{\text{Chaos-PSO}}}{\text{Makespan}_{\text{PSO}}} \times 100\%$$
   $$\text{Improvement Over LDIW-PSO (\%)} = \frac{\text{Makespan}_{\text{LDIW}} - \text{Makespan}_{\text{Chaos-PSO}}}{\text{Makespan}_{\text{LDIW}}} \times 100\%$$

---

## 6. Implementation and Execution Plan

### 6.1 Code Assets to Create
1. [`src/core/ChaosParticleSwarmScheduler.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/ChaosParticleSwarmScheduler.java): The new Chaos-Enhanced PSO implementation.
2. [`src/core/LdiwParticleSwarmScheduler.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/LdiwParticleSwarmScheduler.java): The LDIW-PSO control implementation.
3. [`src/core/InstrumentedBaselinePsoScheduler.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/InstrumentedBaselinePsoScheduler.java): Baseline PSO wrapper for convergence telemetry.
4. [`src/core/ChaosParticleSwarmSchedulerTest.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/ChaosParticleSwarmSchedulerTest.java): Comprehensive unit tests covering chaotic sequences, numerical bounds, schedule validity, and determinism.
5. [`src/core/ChaosPsoExperimentRunner.java`](file:///c:/Users/jatin/OneDrive/Desktop/Cloud%20Project/cloud-scheduling-research-/src/core/ChaosPsoExperimentRunner.java): Experiment runner coordinating the 3-way evaluation and logging convergence trajectories.

### 6.2 Scripts and Deliverables
1. `scripts/run_chaos_experiments.ps1`: Automated PowerShell script to compile, run tests, execute experiments, and invoke chart generation.
2. `scripts/generate_chaos_charts.py`: Generates publication-ready figures for makespan, convergence, and resource utilization.
3. `scripts/create_chaos_presentation.py`: Python PPTX script generating the university viva presentation `Chaos_Enhanced_PSO_Research.pptx`.
4. `CHAOS_PSO_RESEARCH_REPORT.md`: Comprehensive academic report with complete result tables, statistical significance, and analysis.
