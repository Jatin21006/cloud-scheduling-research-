import os
import csv
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np

# Output directories
BASE_RESULTS = r'results\chaos_pso'
CHARTS_DIR = os.path.join(BASE_RESULTS, 'charts')
SUMMARY_CSV = os.path.join(BASE_RESULTS, 'summary', 'chaos_pso_comparison_summary.csv')
MEAN_CONV_CSV = os.path.join(BASE_RESULTS, 'convergence', 'mean_convergence_trajectories.csv')
TRIAL_CONV_CSV = os.path.join(BASE_RESULTS, 'convergence', 'individual_trial_convergence.csv')

os.makedirs(CHARTS_DIR, exist_ok=True)

# Styling
plt.rcParams['font.sans-serif'] = 'Arial'
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['axes.edgecolor'] = '#CCCCCC'
plt.rcParams['axes.linewidth'] = 0.8

COLOR_BASE = '#EA580C'    # Orange for Baseline PSO
COLOR_LDIW = '#0284C7'    # Sky Blue for LDIW-PSO
COLOR_CHAOS = '#16A34A'   # Green for Chaos-PSO

# 1. Load summary data
rows = []
with open(SUMMARY_CSV, 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for r in reader:
        rows.append(r)

normal_rows = [r for r in rows if r['workload_type'] == 'NORMAL']
ddos_rows = [r for r in rows if r['workload_type'] == 'DDOS']

scenarios_labels = [
    'S1\n(50t, 10i)', 'S2\n(100t, 10i)',
    'S3\n(50t, 50i)', 'S4\n(100t, 50i)',
    'S5\n(50t, 100i)', 'S6\n(100t, 100i)'
]

x = np.arange(len(scenarios_labels))
width = 0.25

# -------------------------------------------------------------
# Chart 1: Normal Workload Makespan Comparison
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(9, 5), dpi=300)
base_normal = [float(r['base_pso_mean']) for r in normal_rows]
ldiw_normal = [float(r['ldiw_pso_mean']) for r in normal_rows]
chaos_normal = [float(r['chaos_pso_mean']) for r in normal_rows]

rects1 = ax.bar(x - width, base_normal, width, label='Baseline PSO (w=0.9)', color=COLOR_BASE, edgecolor='black', linewidth=0.5)
rects2 = ax.bar(x, ldiw_normal, width, label='LDIW-PSO (Linear Decay)', color=COLOR_LDIW, edgecolor='black', linewidth=0.5)
rects3 = ax.bar(x + width, chaos_normal, width, label='Chaos-PSO (Logistic Map)', color=COLOR_CHAOS, edgecolor='black', linewidth=0.5)

ax.set_ylabel('Makespan (seconds)', fontsize=11, fontweight='bold')
ax.set_title('Makespan Comparison Across 6 Scenarios — Normal Workload', fontsize=13, fontweight='bold', pad=12)
ax.set_xticks(x)
ax.set_xticklabels(scenarios_labels, fontsize=10)
ax.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=10)
ax.grid(axis='y', linestyle='--', alpha=0.5)

# Value labels on top of bars
for rects in [rects1, rects2, rects3]:
    for bar in rects:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2.0, yval + 0.3, f"{yval:.1f}s", ha='center', va='bottom', fontsize=7.5)

ax.set_ylim(0, max(base_normal) * 1.15)
plt.tight_layout()
chart1_path = os.path.join(CHARTS_DIR, 'makespan_comparison_normal.png')
plt.savefig(chart1_path)
plt.close()
print("Saved:", chart1_path)

# -------------------------------------------------------------
# Chart 2: DDoS Workload Makespan Comparison
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(9, 5), dpi=300)
base_ddos = [float(r['base_pso_mean']) for r in ddos_rows]
ldiw_ddos = [float(r['ldiw_pso_mean']) for r in ddos_rows]
chaos_ddos = [float(r['chaos_pso_mean']) for r in ddos_rows]

rects1 = ax.bar(x - width, base_ddos, width, label='Baseline PSO (w=0.9)', color=COLOR_BASE, edgecolor='black', linewidth=0.5)
rects2 = ax.bar(x, ldiw_ddos, width, label='LDIW-PSO (Linear Decay)', color=COLOR_LDIW, edgecolor='black', linewidth=0.5)
rects3 = ax.bar(x + width, chaos_ddos, width, label='Chaos-PSO (Logistic Map)', color=COLOR_CHAOS, edgecolor='black', linewidth=0.5)

ax.set_ylabel('Makespan (seconds)', fontsize=11, fontweight='bold')
ax.set_title('Makespan Comparison Across 6 Scenarios — DDoS Workload (100x Scale)', fontsize=13, fontweight='bold', pad=12)
ax.set_xticks(x)
ax.set_xticklabels(scenarios_labels, fontsize=10)
ax.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=10)
ax.grid(axis='y', linestyle='--', alpha=0.5)

for rects in [rects1, rects2, rects3]:
    for bar in rects:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2.0, yval + 30, f"{yval:.0f}s", ha='center', va='bottom', fontsize=7.5)

ax.set_ylim(0, max(base_ddos) * 1.15)
plt.tight_layout()
chart2_path = os.path.join(CHARTS_DIR, 'makespan_comparison_ddos.png')
plt.savefig(chart2_path)
plt.close()
print("Saved:", chart2_path)

# -------------------------------------------------------------
# Chart 3: Chaos-PSO Improvement Percentage over Baseline & LDIW
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(9, 4.8), dpi=300)
imp_base = [float(r['improvement_over_base_pct']) for r in normal_rows]
imp_ldiw = [float(r['improvement_over_ldiw_pct']) for r in normal_rows]

w2 = 0.35
r1 = ax.bar(x - w2/2, imp_base, w2, label='Improvement over Baseline PSO (%)', color='#2563EB', edgecolor='black', linewidth=0.5)
r2 = ax.bar(x + w2/2, imp_ldiw, w2, label='Improvement over LDIW-PSO (%)', color='#7C3AED', edgecolor='black', linewidth=0.5)

ax.set_ylabel('Makespan Improvement (%)', fontsize=11, fontweight='bold')
ax.set_title('Chaos-PSO Relative Makespan Improvement by Scenario', fontsize=13, fontweight='bold', pad=12)
ax.set_xticks(x)
ax.set_xticklabels(scenarios_labels, fontsize=10)
ax.axhline(0, color='gray', linewidth=0.8)
ax.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=10)
ax.grid(axis='y', linestyle='--', alpha=0.5)

for bar in r1:
    h = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2, h + 0.15, f"{h:+.2f}%", ha='center', va='bottom', fontsize=8, fontweight='bold')
for bar in r2:
    h = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2, h + 0.15, f"{h:+.2f}%", ha='center', va='bottom', fontsize=8)

ax.set_ylim(-0.5, max(imp_base) * 1.3)
plt.tight_layout()
chart3_path = os.path.join(CHARTS_DIR, 'chaos_pso_improvement_percentage.png')
plt.savefig(chart3_path)
plt.close()
print("Saved:", chart3_path)

# -------------------------------------------------------------
# Chart 4: Resource Utilization Comparison
# -------------------------------------------------------------
fig, ax = plt.subplots(figsize=(9, 4.8), dpi=300)
base_ru = [float(r['base_pso_ru']) for r in normal_rows]
chaos_ru = [float(r['chaos_pso_ru']) for r in normal_rows]

r1 = ax.bar(x - w2/2, base_ru, w2, label='Baseline PSO Resource Utilization', color=COLOR_BASE, edgecolor='black', linewidth=0.5)
r2 = ax.bar(x + w2/2, chaos_ru, w2, label='Chaos-PSO Resource Utilization', color=COLOR_CHAOS, edgecolor='black', linewidth=0.5)

ax.set_ylabel('Resource Utilization (%)', fontsize=11, fontweight='bold')
ax.set_title('Cloud Datacenter Resource Utilization (RU %) Across Scenarios', fontsize=13, fontweight='bold', pad=12)
ax.set_xticks(x)
ax.set_xticklabels(scenarios_labels, fontsize=10)
ax.set_ylim(70, 100)
ax.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=10, loc='lower right')
ax.grid(axis='y', linestyle='--', alpha=0.5)

for bar in r1:
    h = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2, h + 0.4, f"{h:.1f}%", ha='center', va='bottom', fontsize=8)
for bar in r2:
    h = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2, h + 0.4, f"{h:.1f}%", ha='center', va='bottom', fontsize=8, fontweight='bold')

plt.tight_layout()
chart4_path = os.path.join(CHARTS_DIR, 'resource_utilization_comparison.png')
plt.savefig(chart4_path)
plt.close()
print("Saved:", chart4_path)

# -------------------------------------------------------------
# Chart 5: Convergence Curves for Representative Scenarios (S2 & S4)
# -------------------------------------------------------------
# Load mean convergence curves
mean_conv = {}
with open(MEAN_CONV_CSV, 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        key = (row['workload_type'], int(row['scenario_id']), row['algorithm'])
        if key not in mean_conv:
            mean_conv[key] = []
        mean_conv[key].append((int(row['iteration']), float(row['mean_makespan'])))

# Load individual trial points
trial_conv = {}
with open(TRIAL_CONV_CSV, 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        key = (row['workload_type'], int(row['scenario_id']), row['algorithm'], int(row['trial_number']))
        if key not in trial_conv:
            trial_conv[key] = []
        trial_conv[key].append((int(row['iteration']), float(row['makespan'])))

fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5), dpi=300)

# Subplot 1: Scenario 2 (100 tasks, 10 iterations) - Normal Workload
sc_id = 2
wl = 'NORMAL'
# Plot individual trials faintly
for t in range(1, 11):
    b_pts = trial_conv.get((wl, sc_id, 'Baseline-PSO', t), [])
    c_pts = trial_conv.get((wl, sc_id, 'Chaos-PSO', t), [])
    if b_pts:
        ax1.plot([p[0] for p in b_pts], [p[1] for p in b_pts], color=COLOR_BASE, alpha=0.15, linewidth=0.8)
    if c_pts:
        ax1.plot([p[0] for p in c_pts], [p[1] for p in c_pts], color=COLOR_CHAOS, alpha=0.15, linewidth=0.8)

# Plot aggregated mean lines boldly
b_mean = mean_conv.get((wl, sc_id, 'Baseline-PSO'), [])
l_mean = mean_conv.get((wl, sc_id, 'LDIW-PSO'), [])
c_mean = mean_conv.get((wl, sc_id, 'Chaos-PSO'), [])

if b_mean:
    ax1.plot([p[0] for p in b_mean], [p[1] for p in b_mean], color=COLOR_BASE, linewidth=2.5, marker='o', markersize=4, label='Baseline PSO Mean')
if l_mean:
    ax1.plot([p[0] for p in l_mean], [p[1] for p in l_mean], color=COLOR_LDIW, linewidth=2.2, linestyle='--', marker='s', markersize=4, label='LDIW-PSO Mean')
if c_mean:
    ax1.plot([p[0] for p in c_mean], [p[1] for p in c_mean], color=COLOR_CHAOS, linewidth=2.5, marker='^', markersize=4, label='Chaos-PSO Mean')

ax1.set_xlabel('Iteration', fontsize=11, fontweight='bold')
ax1.set_ylabel('Makespan (seconds)', fontsize=11, fontweight='bold')
ax1.set_title('Convergence: Scenario 2 (100 Tasks, 10 Iterations)', fontsize=12, fontweight='bold')
ax1.legend(frameon=True, facecolor='white', framealpha=0.9)
ax1.grid(True, linestyle='--', alpha=0.5)

# Subplot 2: Scenario 4 (100 tasks, 50 iterations) - Normal Workload
sc_id = 4
for t in range(1, 11):
    b_pts = trial_conv.get((wl, sc_id, 'Baseline-PSO', t), [])
    c_pts = trial_conv.get((wl, sc_id, 'Chaos-PSO', t), [])
    if b_pts:
        ax2.plot([p[0] for p in b_pts], [p[1] for p in b_pts], color=COLOR_BASE, alpha=0.15, linewidth=0.8)
    if c_pts:
        ax2.plot([p[0] for p in c_pts], [p[1] for p in c_pts], color=COLOR_CHAOS, alpha=0.15, linewidth=0.8)

b_mean4 = mean_conv.get((wl, sc_id, 'Baseline-PSO'), [])
l_mean4 = mean_conv.get((wl, sc_id, 'LDIW-PSO'), [])
c_mean4 = mean_conv.get((wl, sc_id, 'Chaos-PSO'), [])

if b_mean4:
    ax2.plot([p[0] for p in b_mean4], [p[1] for p in b_mean4], color=COLOR_BASE, linewidth=2.5, label='Baseline PSO Mean')
if l_mean4:
    ax2.plot([p[0] for p in l_mean4], [p[1] for p in l_mean4], color=COLOR_LDIW, linewidth=2.2, linestyle='--', label='LDIW-PSO Mean')
if c_mean4:
    ax2.plot([p[0] for p in c_mean4], [p[1] for p in c_mean4], color=COLOR_CHAOS, linewidth=2.5, label='Chaos-PSO Mean')

ax2.set_xlabel('Iteration', fontsize=11, fontweight='bold')
ax2.set_ylabel('Makespan (seconds)', fontsize=11, fontweight='bold')
ax2.set_title('Convergence: Scenario 4 (100 Tasks, 50 Iterations)', fontsize=12, fontweight='bold')
ax2.legend(frameon=True, facecolor='white', framealpha=0.9)
ax2.grid(True, linestyle='--', alpha=0.5)

plt.tight_layout()
chart5_path = os.path.join(CHARTS_DIR, 'convergence_curves_s2_s4.png')
plt.savefig(chart5_path)
plt.close()
print("Saved:", chart5_path)

# -------------------------------------------------------------
# Chart 6: Logistic Map Chaotic Sequence & Dynamic Inertia Trajectory
# -------------------------------------------------------------
# Generate 100 steps of Logistic map and resulting inertia weight
x_vals = []
w_vals = []
x_curr = 0.70
for i in range(100):
    x_curr = 4.0 * x_curr * (1.0 - x_curr)
    w_curr = 0.40 + 0.50 * x_curr
    x_vals.append(x_curr)
    w_vals.append(w_curr)

fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 6), dpi=300, sharex=True)

ax1.plot(range(1, 101), x_vals, color='#7C3AED', linewidth=1.5)
ax1.set_ylabel('Chaotic State x(t)', fontsize=11, fontweight='bold')
ax1.set_title('Non-Linear Chaotic Trajectory & Dynamic Inertia Weight Dynamics', fontsize=13, fontweight='bold')
ax1.grid(True, linestyle='--', alpha=0.5)
ax1.set_ylim(0, 1)

ax2.plot(range(1, 101), w_vals, color='#16A34A', linewidth=1.8, label='Chaos-PSO w(t)')
ax2.axhline(0.9, color='#EA580C', linestyle=':', linewidth=2, label='Baseline PSO (w=0.9 constant)')
ax2.plot(range(1, 101), [0.9 - (t/99.0)*0.5 for t in range(100)], color='#0284C7', linestyle='--', linewidth=1.8, label='LDIW-PSO (Linear Decay)')
ax2.set_xlabel('Iteration (t)', fontsize=11, fontweight='bold')
ax2.set_ylabel('Inertia Weight w(t)', fontsize=11, fontweight='bold')
ax2.set_ylim(0.3, 1.0)
ax2.legend(loc='upper right', frameon=True, facecolor='white', framealpha=0.9)
ax2.grid(True, linestyle='--', alpha=0.5)

plt.tight_layout()
chart6_path = os.path.join(CHARTS_DIR, 'logistic_map_inertia_dynamics.png')
plt.savefig(chart6_path)
plt.close()
print("Saved:", chart6_path)

print("All research charts successfully generated in:", CHARTS_DIR)

