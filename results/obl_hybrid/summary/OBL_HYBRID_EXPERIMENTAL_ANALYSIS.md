# Experimental Evaluation: OBL-Hybrid GA-PSO vs. Baseline Hybrid GA-PSO

Empirical comparison across 6 scenarios, Normal and DDoS workloads (10 trials each, 240 runs total).

## NORMAL Workload Evaluation

| Scenario | Tasks | Iters | Metric | Baseline Hybrid GA-PSO | OBL-Hybrid GA-PSO | Delta | Impr (%) |
|---|---|---|---|---|---|---|---|
| S1 | 50 | 10 | **Mean Makespan** | 12.295 s | **12.399 s** | -0.104 s | **-0.85%** |
| | | | Best Makespan | 11.550 s | 11.740 s | -0.190 s | - |
| | | | Worst Makespan | 13.330 s | **13.260 s** | +0.070 s | - |
| | | | Std. Dev | 0.524 s | 0.462 s | - | - |
| | | | Resource Util. | 84.07% | **83.34%** | -0.72% | - |
| | | | Host Exec Time | 2.9 ms | 2.5 ms | - | - |
| S2 | 100 | 10 | **Mean Makespan** | 23.124 s | **22.748 s** | +0.376 s | **+1.63%** |
| | | | Best Makespan | 21.360 s | 21.330 s | +0.030 s | - |
| | | | Worst Makespan | 24.620 s | **24.160 s** | +0.460 s | - |
| | | | Std. Dev | 1.016 s | 0.875 s | - | - |
| | | | Resource Util. | 87.41% | **88.83%** | +1.42% | - |
| | | | Host Exec Time | 3.9 ms | 4.0 ms | - | - |
| S3 | 50 | 50 | **Mean Makespan** | 11.658 s | **11.765 s** | -0.107 s | **-0.92%** |
| | | | Best Makespan | 10.880 s | 10.880 s | +0.000 s | - |
| | | | Worst Makespan | 12.030 s | **12.330 s** | -0.300 s | - |
| | | | Std. Dev | 0.345 s | 0.401 s | - | - |
| | | | Resource Util. | 88.62% | **87.84%** | -0.78% | - |
| | | | Host Exec Time | 9.3 ms | 9.0 ms | - | - |
| S4 | 100 | 50 | **Mean Makespan** | 21.792 s | **21.514 s** | +0.278 s | **+1.28%** |
| | | | Best Makespan | 20.730 s | 20.240 s | +0.490 s | - |
| | | | Worst Makespan | 23.730 s | **23.470 s** | +0.260 s | - |
| | | | Std. Dev | 0.931 s | 0.824 s | - | - |
| | | | Resource Util. | 92.77% | **93.92%** | +1.16% | - |
| | | | Host Exec Time | 17.1 ms | 17.2 ms | - | - |
| S5 | 50 | 100 | **Mean Makespan** | 11.337 s | **11.451 s** | -0.114 s | **-1.01%** |
| | | | Best Makespan | 10.440 s | 10.580 s | -0.140 s | - |
| | | | Worst Makespan | 11.950 s | **12.330 s** | -0.380 s | - |
| | | | Std. Dev | 0.451 s | 0.532 s | - | - |
| | | | Resource Util. | 91.16% | **90.28%** | -0.87% | - |
| | | | Host Exec Time | 16.8 ms | 16.1 ms | - | - |
| S6 | 100 | 100 | **Mean Makespan** | 21.156 s | **21.040 s** | +0.116 s | **+0.55%** |
| | | | Best Makespan | 20.000 s | 20.100 s | -0.100 s | - |
| | | | Worst Makespan | 22.260 s | **22.230 s** | +0.030 s | - |
| | | | Std. Dev | 0.622 s | 0.655 s | - | - |
| | | | Resource Util. | 95.48% | **96.00%** | +0.52% | - |
| | | | Host Exec Time | 30.1 ms | 32.4 ms | - | - |

## DDOS Workload Evaluation

| Scenario | Tasks | Iters | Metric | Baseline Hybrid GA-PSO | OBL-Hybrid GA-PSO | Delta | Impr (%) |
|---|---|---|---|---|---|---|---|
| S1 | 50 | 10 | **Mean Makespan** | 1229.500 s | **1239.900 s** | -10.400 s | **-0.85%** |
| | | | Best Makespan | 1155.000 s | 1174.000 s | -19.000 s | - |
| | | | Worst Makespan | 1333.000 s | **1326.000 s** | +7.000 s | - |
| | | | Std. Dev | 52.378 s | 46.157 s | - | - |
| | | | Resource Util. | 84.07% | **83.34%** | -0.72% | - |
| | | | Host Exec Time | 1.0 ms | 1.0 ms | - | - |
| S2 | 100 | 10 | **Mean Makespan** | 2312.400 s | **2274.800 s** | +37.600 s | **+1.63%** |
| | | | Best Makespan | 2136.000 s | 2133.000 s | +3.000 s | - |
| | | | Worst Makespan | 2462.000 s | **2416.000 s** | +46.000 s | - |
| | | | Std. Dev | 101.551 s | 87.453 s | - | - |
| | | | Resource Util. | 87.41% | **88.83%** | +1.42% | - |
| | | | Host Exec Time | 3.1 ms | 3.0 ms | - | - |
| S3 | 50 | 50 | **Mean Makespan** | 1179.800 s | **1172.500 s** | +7.300 s | **+0.62%** |
| | | | Best Makespan | 1088.000 s | 1088.000 s | +0.000 s | - |
| | | | Worst Makespan | 1261.000 s | **1233.000 s** | +28.000 s | - |
| | | | Std. Dev | 44.468 s | 42.410 s | - | - |
| | | | Resource Util. | 87.61% | **88.14%** | +0.53% | - |
| | | | Host Exec Time | 6.8 ms | 7.0 ms | - | - |
| S4 | 100 | 50 | **Mean Makespan** | 2158.300 s | **2151.400 s** | +6.900 s | **+0.32%** |
| | | | Best Makespan | 2040.000 s | 2024.000 s | +16.000 s | - |
| | | | Worst Makespan | 2373.000 s | **2347.000 s** | +26.000 s | - |
| | | | Std. Dev | 95.118 s | 82.353 s | - | - |
| | | | Resource Util. | 93.67% | **93.92%** | +0.25% | - |
| | | | Host Exec Time | 14.7 ms | 15.9 ms | - | - |
| S5 | 50 | 100 | **Mean Makespan** | 1135.400 s | **1142.300 s** | -6.900 s | **-0.61%** |
| | | | Best Makespan | 1044.000 s | 1058.000 s | -14.000 s | - |
| | | | Worst Makespan | 1221.000 s | **1233.000 s** | -12.000 s | - |
| | | | Std. Dev | 51.391 s | 51.391 s | - | - |
| | | | Resource Util. | 91.04% | **90.50%** | -0.54% | - |
| | | | Host Exec Time | 13.8 ms | 14.0 ms | - | - |
| S6 | 100 | 100 | **Mean Makespan** | 2110.100 s | **2105.400 s** | +4.700 s | **+0.22%** |
| | | | Best Makespan | 2000.000 s | 2024.000 s | -24.000 s | - |
| | | | Worst Makespan | 2226.000 s | **2223.000 s** | +3.000 s | - |
| | | | Std. Dev | 65.407 s | 63.623 s | - | - |
| | | | Resource Util. | 95.74% | **95.94%** | +0.20% | - |
| | | | Host Exec Time | 50.6 ms | 54.6 ms | - | - |

