import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

plt.style.use('seaborn-v0_8-whitegrid' if 'seaborn-v0_8-whitegrid' in plt.style.available else 'default')
plt.rcParams['font.sans-serif'] = 'Arial'
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['figure.dpi'] = 300

repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
summary_csv = os.path.join(repo_root, 'results', 'final_comparison', 'summary', 'summary_statistics.csv')
impr_csv = os.path.join(repo_root, 'results', 'final_comparison', 'summary', 'percentage_improvement.csv')
fig_dir = os.path.join(repo_root, 'results', 'final_comparison', 'figures')
os.makedirs(fig_dir, exist_ok=True)

df = pd.read_csv(summary_csv)
df_impr = pd.read_csv(impr_csv)

algo_order = ['Hybrid-GA-PSO', 'SA-Hybrid-GA-PSO', 'OBL-Hybrid-GA-PSO', 'Memetic-Hybrid-GA-PSO', 'Chaos-PSO']
algo_labels = ['Baseline Hybrid', 'SA-Hybrid', 'OBL-Hybrid', 'Memetic-Hybrid', 'Chaos-PSO']
algo_colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd']
color_map = dict(zip(algo_order, algo_colors))
label_map = dict(zip(algo_order, algo_labels))

print("Generating Chart 1: Normal Workload Makespan Comparison (S1-S6)...")
fig, ax = plt.subplots(figsize=(11, 6))
scenarios = range(1, 7)
x = np.arange(len(scenarios))
width = 0.16

for idx, algo in enumerate(algo_order):
    sub = df[(df['workload'] == 'NORMAL') & (df['algorithm'] == algo)].sort_values('scenario_id')
    offset = (idx - 2) * width
    bars = ax.bar(x + offset, sub['mean_makespan_s'], width, label=label_map[algo], color=color_map[algo], edgecolor='black', linewidth=0.5)

ax.set_xlabel('Scenario (Tasks, Paper Iterations)', fontsize=12, fontweight='bold')
ax.set_ylabel('Mean Makespan (seconds)', fontsize=12, fontweight='bold')
ax.set_title('Normal Workload Makespan Across Scenarios S1 - S6', fontsize=14, fontweight='bold', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(['S1 (50T, 10I)', 'S2 (100T, 10I)', 'S3 (50T, 50I)', 'S4 (100T, 50I)', 'S5 (50T, 100I)', 'S6 (100T, 100I)'], fontsize=10)
ax.legend(frameon=True, fontsize=10, loc='upper left')
plt.tight_layout()
fig.savefig(os.path.join(fig_dir, 'chart1_normal_makespan.png'))
plt.close(fig)

print("Generating Chart 2: DDoS Workload Makespan Comparison (S1-S6)...")
fig, ax = plt.subplots(figsize=(11, 6))
for idx, algo in enumerate(algo_order):
    sub = df[(df['workload'] == 'DDOS') & (df['algorithm'] == algo)].sort_values('scenario_id')
    offset = (idx - 2) * width
    ax.bar(x + offset, sub['mean_makespan_s'], width, label=label_map[algo], color=color_map[algo], edgecolor='black', linewidth=0.5)

ax.set_xlabel('Scenario (Tasks, Paper Iterations)', fontsize=12, fontweight='bold')
ax.set_ylabel('Mean Makespan (seconds)', fontsize=12, fontweight='bold')
ax.set_title('DDoS Workload Makespan Across Scenarios S1 - S6 (100x Scaled)', fontsize=14, fontweight='bold', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(['S1 (50T, 10I)', 'S2 (100T, 10I)', 'S3 (50T, 50I)', 'S4 (100T, 50I)', 'S5 (50T, 100I)', 'S6 (100T, 100I)'], fontsize=10)
ax.legend(frameon=True, fontsize=10, loc='upper left')
plt.tight_layout()
fig.savefig(os.path.join(fig_dir, 'chart2_ddos_makespan.png'))
plt.close(fig)

print("Generating Chart 3: Percentage Improvement Relative to Baseline Hybrid GA-PSO...")
fig, ax = plt.subplots(figsize=(11, 6))
ext_cols = ['sa_hybrid_impr_pct', 'obl_hybrid_impr_pct', 'memetic_hybrid_impr_pct', 'chaos_pso_impr_pct']
ext_labels = ['SA-Hybrid', 'OBL-Hybrid', 'Memetic-Hybrid', 'Chaos-PSO']
ext_colors = ['#ff7f0e', '#2ca02c', '#d62728', '#9467bd']
w_sub = 0.20

sub_norm_impr = df_impr[df_impr['workload'] == 'NORMAL'].sort_values('scenario_id')
for idx, col in enumerate(ext_cols):
    offset = (idx - 1.5) * w_sub
    ax.bar(x + offset, sub_norm_impr[col], w_sub, label=ext_labels[idx], color=ext_colors[idx], edgecolor='black', linewidth=0.5)

ax.axhline(0, color='gray', linestyle='--', linewidth=1)
ax.set_xlabel('Scenario (Normal Workload)', fontsize=12, fontweight='bold')
ax.set_ylabel('Makespan Improvement (%)', fontsize=12, fontweight='bold')
ax.set_title('Percentage Makespan Improvement Over Baseline Hybrid GA-PSO', fontsize=14, fontweight='bold', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(['S1 (50T, 10I)', 'S2 (100T, 10I)', 'S3 (50T, 50I)', 'S4 (100T, 50I)', 'S5 (50T, 100I)', 'S6 (100T, 100I)'], fontsize=10)
ax.legend(frameon=True, fontsize=10, loc='upper right')
plt.tight_layout()
fig.savefig(os.path.join(fig_dir, 'chart3_percentage_improvement.png'))
plt.close(fig)

print("Generating Chart 4: Resource Utilization Comparison...")
fig, ax = plt.subplots(figsize=(11, 6))
for idx, algo in enumerate(algo_order):
    sub = df[(df['workload'] == 'NORMAL') & (df['algorithm'] == algo)].sort_values('scenario_id')
    offset = (idx - 2) * width
    ax.bar(x + offset, sub['mean_ru_pct'], width, label=label_map[algo], color=color_map[algo], edgecolor='black', linewidth=0.5)

ax.set_xlabel('Scenario', fontsize=12, fontweight='bold')
ax.set_ylabel('Resource Utilization (%)', fontsize=12, fontweight='bold')
ax.set_title('Normal Workload Resource Utilization Across Algorithms', fontsize=14, fontweight='bold', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(['S1 (50T)', 'S2 (100T)', 'S3 (50T)', 'S4 (100T)', 'S5 (50T)', 'S6 (100T)'], fontsize=10)
ax.set_ylim(75, 102)
ax.legend(frameon=True, fontsize=10, loc='lower right')
plt.tight_layout()
fig.savefig(os.path.join(fig_dir, 'chart4_resource_utilization.png'))
plt.close(fig)

print("Generating Chart 5: Standard Deviation / Stability Comparison...")
fig, ax = plt.subplots(figsize=(11, 6))
for idx, algo in enumerate(algo_order):
    sub = df[(df['workload'] == 'NORMAL') & (df['algorithm'] == algo)].sort_values('scenario_id')
    offset = (idx - 2) * width
    ax.bar(x + offset, sub['std_sample_s'], width, label=label_map[algo], color=color_map[algo], edgecolor='black', linewidth=0.5)

ax.set_xlabel('Scenario', fontsize=12, fontweight='bold')
ax.set_ylabel('Standard Deviation (seconds)', fontsize=12, fontweight='bold')
ax.set_title('Schedule Variability Across 10 Independent Trials (Normal Workload)', fontsize=14, fontweight='bold', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(['S1', 'S2', 'S3', 'S4', 'S5', 'S6'], fontsize=10)
ax.legend(frameon=True, fontsize=10, loc='upper right')
plt.tight_layout()
fig.savefig(os.path.join(fig_dir, 'chart5_standard_deviation.png'))
plt.close(fig)

print("Generating Chart 6: Execution Time / Computational Overhead...")
fig, ax = plt.subplots(figsize=(11, 6))
for idx, algo in enumerate(algo_order):
    sub = df[(df['workload'] == 'NORMAL') & (df['algorithm'] == algo)].sort_values('scenario_id')
    offset = (idx - 2) * width
    ax.bar(x + offset, sub['mean_time_ms'], width, label=label_map[algo], color=color_map[algo], edgecolor='black', linewidth=0.5)

ax.set_xlabel('Scenario', fontsize=12, fontweight='bold')
ax.set_ylabel('Host Execution Time (milliseconds)', fontsize=12, fontweight='bold')
ax.set_title('Host Execution Runtime Overhead by Scenario', fontsize=14, fontweight='bold', pad=15)
ax.set_xticks(x)
ax.set_xticklabels(['S1 (10I)', 'S2 (10I)', 'S3 (50I)', 'S4 (50I)', 'S5 (100I)', 'S6 (100I)'], fontsize=10)
ax.legend(frameon=True, fontsize=10, loc='upper left')
plt.tight_layout()
fig.savefig(os.path.join(fig_dir, 'chart6_execution_time.png'))
plt.close(fig)

print("All 6 comparative charts generated successfully in:", fig_dir)

