# Final Comparative Experimental Analysis: 5 Cloud Schedulers

Comprehensive matched experimental evaluation comparing Baseline Hybrid GA-PSO against all four research extensions across 600 total executions.

## NORMAL Workload Evaluation

| Scenario | Tasks | Iters | Hybrid-GA-PSO (s) | SA-Hybrid (s) | OBL-Hybrid (s) | Memetic-Hybrid (s) | Chaos-PSO (s) |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| S1 | 50 | 10 | 12.295 | 12.409 | 12.399 | **11.239** | 12.227 |
| S2 | 100 | 10 | 23.124 | 22.708 | 22.748 | **21.437** | 23.364 |
| S3 | 50 | 50 | 11.658 | 11.821 | 11.765 | **11.107** | 11.545 |
| S4 | 100 | 50 | 21.792 | 21.554 | 21.514 | **20.767** | 22.160 |
| S5 | 50 | 100 | 11.337 | 11.253 | 11.451 | **10.919** | 11.315 |
| S6 | 100 | 100 | 21.156 | 21.119 | 21.040 | **20.721** | 21.564 |

## DDOS Workload Evaluation

| Scenario | Tasks | Iters | Hybrid-GA-PSO (s) | SA-Hybrid (s) | OBL-Hybrid (s) | Memetic-Hybrid (s) | Chaos-PSO (s) |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| S1 | 50 | 10 | 1229.500 | 1240.900 | 1239.900 | **1123.900** | 1222.700 |
| S2 | 100 | 10 | 2312.400 | 2270.800 | 2274.800 | **2139.400** | 2336.400 |
| S3 | 50 | 50 | 1179.800 | 1187.700 | 1172.500 | **1114.200** | 1154.500 |
| S4 | 100 | 50 | 2158.300 | 2153.600 | 2151.400 | **2078.000** | 2210.300 |
| S5 | 50 | 100 | 1135.400 | 1139.300 | 1142.300 | **1098.600** | 1129.800 |
| S6 | 100 | 100 | 2110.100 | 2105.500 | 2105.400 | **2072.900** | 2158.000 |

