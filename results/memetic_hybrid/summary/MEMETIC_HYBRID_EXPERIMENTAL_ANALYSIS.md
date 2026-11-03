# Experimental Evaluation: Memetic Hybrid GA-PSO vs. Baseline Hybrid GA-PSO

Empirical comparison across 6 scenarios, Normal and DDoS workloads (10 trials each, 240 runs total).

## NORMAL Workload Evaluation

| Scenario | Tasks | Iters | Metric | Baseline Hybrid GA-PSO | Memetic-Hybrid-GA-PSO | Delta | Impr (%) |
|---|---|---|---|---|---|---|---|
| S1 | 50 | 10 | **Mean Makespan** | 12.295 s | **11.239 s** | +1.056 s | **+8.59%** |
| | | | Best Makespan | 11.550 s | **10.710 s** | +0.840 s | - |
| | | | Worst Makespan | 13.330 s | **12.350 s** | +0.980 s | - |
| | | | Std. Dev | 0.524 s | 0.492 s | - | - |
| | | | Resource Util. | 84.07% | **92.01%** | +7.94% | - |
| | | | Host Exec Time | 4.6 ms | 4.6 ms | - | - |
| | | | Local Search Evals | 0 | 16 evals | - | - |
| S2 | 100 | 10 | **Mean Makespan** | 23.124 s | **21.437 s** | +1.687 s | **+7.30%** |
| | | | Best Makespan | 21.360 s | **19.980 s** | +1.380 s | - |
| | | | Worst Makespan | 24.620 s | **22.670 s** | +1.950 s | - |
| | | | Std. Dev | 1.016 s | 0.769 s | - | - |
| | | | Resource Util. | 87.41% | **94.27%** | +6.86% | - |
| | | | Host Exec Time | 5.2 ms | 6.4 ms | - | - |
| | | | Local Search Evals | 0 | 18 evals | - | - |
| S3 | 50 | 50 | **Mean Makespan** | 11.658 s | **11.107 s** | +0.551 s | **+4.73%** |
| | | | Best Makespan | 10.880 s | **10.530 s** | +0.350 s | - |
| | | | Worst Makespan | 12.030 s | **12.350 s** | -0.320 s | - |
| | | | Std. Dev | 0.345 s | 0.527 s | - | - |
| | | | Resource Util. | 88.62% | **93.12%** | +4.49% | - |
| | | | Host Exec Time | 12.1 ms | 11.2 ms | - | - |
| | | | Local Search Evals | 0 | 59 evals | - | - |
| S4 | 100 | 50 | **Mean Makespan** | 21.792 s | **20.767 s** | +1.025 s | **+4.70%** |
| | | | Best Makespan | 20.730 s | **19.980 s** | +0.750 s | - |
| | | | Worst Makespan | 23.730 s | **21.770 s** | +1.960 s | - |
| | | | Std. Dev | 0.931 s | 0.550 s | - | - |
| | | | Resource Util. | 92.77% | **97.25%** | +4.49% | - |
| | | | Host Exec Time | 20.9 ms | 22.5 ms | - | - |
| | | | Local Search Evals | 0 | 64 evals | - | - |
| S5 | 50 | 100 | **Mean Makespan** | 11.337 s | **10.919 s** | +0.418 s | **+3.69%** |
| | | | Best Makespan | 10.440 s | **10.190 s** | +0.250 s | - |
| | | | Worst Makespan | 11.950 s | **11.500 s** | +0.450 s | - |
| | | | Std. Dev | 0.451 s | 0.377 s | - | - |
| | | | Resource Util. | 91.16% | **94.62%** | +3.46% | - |
| | | | Host Exec Time | 21.0 ms | 21.0 ms | - | - |
| | | | Local Search Evals | 0 | 110 evals | - | - |
| S6 | 100 | 100 | **Mean Makespan** | 21.156 s | **20.721 s** | +0.435 s | **+2.06%** |
| | | | Best Makespan | 20.000 s | **19.980 s** | +0.020 s | - |
| | | | Worst Makespan | 22.260 s | **21.770 s** | +0.490 s | - |
| | | | Std. Dev | 0.622 s | 0.553 s | - | - |
| | | | Resource Util. | 95.48% | **97.47%** | +1.98% | - |
| | | | Host Exec Time | 38.0 ms | 38.0 ms | - | - |
| | | | Local Search Evals | 0 | 115 evals | - | - |

## DDOS Workload Evaluation

| Scenario | Tasks | Iters | Metric | Baseline Hybrid GA-PSO | Memetic-Hybrid-GA-PSO | Delta | Impr (%) |
|---|---|---|---|---|---|---|---|
| S1 | 50 | 10 | **Mean Makespan** | 1229.500 s | **1123.900 s** | +105.600 s | **+8.59%** |
| | | | Best Makespan | 1155.000 s | **1071.000 s** | +84.000 s | - |
| | | | Worst Makespan | 1333.000 s | **1235.000 s** | +98.000 s | - |
| | | | Std. Dev | 52.378 s | 49.211 s | - | - |
| | | | Resource Util. | 84.07% | **92.01%** | +7.94% | - |
| | | | Host Exec Time | 2.0 ms | 2.0 ms | - | - |
| | | | Local Search Evals | 0 | 16 evals | - | - |
| S2 | 100 | 10 | **Mean Makespan** | 2312.400 s | **2139.400 s** | +173.000 s | **+7.48%** |
| | | | Best Makespan | 2136.000 s | **1998.000 s** | +138.000 s | - |
| | | | Worst Makespan | 2462.000 s | **2235.000 s** | +227.000 s | - |
| | | | Std. Dev | 101.551 s | 70.871 s | - | - |
| | | | Resource Util. | 87.41% | **94.44%** | +7.03% | - |
| | | | Host Exec Time | 3.9 ms | 4.3 ms | - | - |
| | | | Local Search Evals | 0 | 18 evals | - | - |
| S3 | 50 | 50 | **Mean Makespan** | 1179.800 s | **1114.200 s** | +65.600 s | **+5.56%** |
| | | | Best Makespan | 1088.000 s | **1053.000 s** | +35.000 s | - |
| | | | Worst Makespan | 1261.000 s | **1235.000 s** | +26.000 s | - |
| | | | Std. Dev | 44.468 s | 54.350 s | - | - |
| | | | Resource Util. | 87.61% | **92.83%** | +5.22% | - |
| | | | Host Exec Time | 10.7 ms | 10.9 ms | - | - |
| | | | Local Search Evals | 0 | 58 evals | - | - |
| S4 | 100 | 50 | **Mean Makespan** | 2158.300 s | **2078.000 s** | +80.300 s | **+3.72%** |
| | | | Best Makespan | 2040.000 s | **1998.000 s** | +42.000 s | - |
| | | | Worst Makespan | 2373.000 s | **2177.000 s** | +196.000 s | - |
| | | | Std. Dev | 95.118 s | 54.905 s | - | - |
| | | | Resource Util. | 93.67% | **97.19%** | +3.52% | - |
| | | | Host Exec Time | 19.6 ms | 20.3 ms | - | - |
| | | | Local Search Evals | 0 | 64 evals | - | - |
| S5 | 50 | 100 | **Mean Makespan** | 1135.400 s | **1098.600 s** | +36.800 s | **+3.24%** |
| | | | Best Makespan | 1044.000 s | **1021.000 s** | +23.000 s | - |
| | | | Worst Makespan | 1221.000 s | **1155.000 s** | +66.000 s | - |
| | | | Std. Dev | 51.391 s | 42.148 s | - | - |
| | | | Resource Util. | 91.04% | **94.06%** | +3.02% | - |
| | | | Host Exec Time | 20.1 ms | 19.6 ms | - | - |
| | | | Local Search Evals | 0 | 109 evals | - | - |
| S6 | 100 | 100 | **Mean Makespan** | 2110.100 s | **2072.900 s** | +37.200 s | **+1.76%** |
| | | | Best Makespan | 2000.000 s | **1998.000 s** | +2.000 s | - |
| | | | Worst Makespan | 2226.000 s | **2177.000 s** | +49.000 s | - |
| | | | Std. Dev | 65.407 s | 55.779 s | - | - |
| | | | Resource Util. | 95.74% | **97.43%** | +1.70% | - |
| | | | Host Exec Time | 39.5 ms | 40.8 ms | - | - |
| | | | Local Search Evals | 0 | 115 evals | - | - |

