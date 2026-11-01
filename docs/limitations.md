# Research Limitations & Threats to Validity

## 1. Workload Modeling: Synthetic Volumetric vs. Real Network-Level DDoS

> [!WARNING]
> **Key Methodological Distinction:**
> The DDoS scenario modeled in this project is a **synthetic task-level workload inflation** ($100\times$ instruction length and $10\times$ file size on 10% of tasks) designed to evaluate scheduler load-balancing under severe computational skew.
> It does **not** simulate network-layer packet attacks (such as TCP SYN floods, UDP amplification, or ICMP floods) or network interface queue drop behaviors.

In actual production cloud environments, DDoS attacks consume edge router bandwidth, saturate ingress firewalls, and trigger packet drops before requests ever arrive as scheduled compute tasks. Consequently:
- Schedulers in real clouds must collaborate with ingress traffic filters and rate limiters.
- Task completion delays in reality would be dominated by socket connection timeouts rather than CPU processing time alone.

---

## 2. Infrastructure Homogeneity

The current implementation assumes a homogeneous computing infrastructure:
- All $M = 5$ virtual machines operate at identical clock rates of $1,000\text{ MIPS}$ on single processing elements ($PE = 1$).
- In heterogeneous cloud environments (e.g., AWS EC2 instances with diverse vCPU counts and microarchitectures), execution time is non-uniform across VM types:
  $$ET(k, m) = \frac{MI_k}{MIPS_m}$$
  While the mathematical model and `CloudSimScheduleAdapter` support heterogeneous MIPS parameters, evaluating solely on homogeneous VMs means the combinatorial bin-packing difficulty is simplified.

---

## 3. Network Latency & Inter-Task Data Transfer

- The analytical fitness function computes execution time solely from CPU instruction lengths ($ET(k, m) = \frac{MI_k}{MIPS_m}$) and assumes zero data transmission delay:
  $$\text{Makespan}_{\text{analytical}} = \max_m \sum \frac{MI_k}{MIPS_m}$$
- Although Cloudlet file sizes ($300\text{ MB}$ normal, $3,000\text{ MB}$ DDoS) are submitted to CloudSim, the datacenter is modeled with internal host-to-VM provisioning that abstracts wide-area WAN latencies.
- Inter-task communications (directed acyclic graph / workflow task dependencies) are not present in this "bag-of-tasks" benchmark.

---

## 4. Source Paper Traceability

- As documented in `research_methodology.md`, the actual publication PDF was not included in the repository.
- While experimental scenarios (**Table 3**), workload parameters ($300\text{ MI}$ / $30,000\text{ MI}$), and algorithm hyperparameters (GA population 75, PSO swarm 75) are verified directly from regression tests and existing code contracts, secondary details (such as whether ABC or a specific Hybrid variant was the proposed novel contribution in the original paper) cannot be verified without the original paper text.
- To maintain scientific integrity, ABC and Hybrid GA-PSO are explicitly designated in this codebase as **proposed implementations** following standard optimization engineering principles.

---

## 5. Discrete Discretization of Continuous PSO Trajectories

- PSO was originally formulated for continuous search spaces ($\mathbb{R}^N$).
- Mapping continuous position vectors $x_{p, t} \in \mathbb{R}$ to discrete VM assignments $m \in \{0, \dots, M-1\}$ using $\text{clamp}(\text{round}(x + v), 0, M-1)$ introduces discretization round-off.
- In low-iteration regimes (e.g. 10 iterations), particles can suffer from velocity quantization loss, which explains why GA and ABC outperformed PSO on Scenarios 1 and 2.
