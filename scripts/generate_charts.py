"""
Script to generate publication-quality SVG charts from experiment summary statistics.
Uses pure Python standard library (no external dependencies).
"""

import csv
import os
import sys

def load_summary_data(csv_path):
    data = []
    if not os.path.exists(csv_path):
        print(f"Error: {csv_path} does not exist.")
        return data
    with open(csv_path, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            data.append({
                "workload_type": row["workload_type"],
                "scenario_id": int(row["scenario_id"]),
                "task_count": int(row["task_count"]),
                "iterations": int(row["iterations"]),
                "algorithm": row["algorithm"],
                "mean_analytical": float(row["mean_analytical_makespan"]),
                "mean_cloudsim": float(row["mean_cloudsim_makespan"]),
                "std_analytical": float(row["std_analytical_makespan"]),
                "exec_time_ms": float(row["mean_execution_time_ms"]),
                "imprv_vs_pso": float(row["improvement_over_pso_pct"]),
            })
    return data

def generate_grouped_bar_svg(data, workload_type, title, filename, y_label="Makespan (seconds)"):
    # Filter by workload
    rows = [r for r in data if r["workload_type"].upper() == workload_type.upper()]
    if not rows:
        return

    scenarios = sorted(list(set(r["scenario_id"] for r in rows)))
    algorithms = ["GA", "PSO", "ABC", "Hybrid-GA-PSO"]
    colors = {
        "GA": "#2563EB",           # Royal Blue
        "PSO": "#DC2626",          # Crimson Red
        "ABC": "#059669",          # Emerald Green
        "Hybrid-GA-PSO": "#7C3AED" # Violet
    }

    # Dimensions
    svg_width = 850
    svg_height = 450
    margin = {"top": 60, "right": 40, "bottom": 80, "left": 70}
    chart_w = svg_width - margin["left"] - margin["right"]
    chart_h = svg_height - margin["top"] - margin["bottom"]

    # Compute max Y
    max_val = max(r["mean_analytical"] for r in rows) * 1.15
    if max_val == 0:
        max_val = 1.0

    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {svg_width} {svg_height}" width="{svg_width}" height="{svg_height}">')
    svg.append('<style>')
    svg.append('  .title { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 18px; font-weight: 700; fill: #1E293B; }')
    svg.append('  .axis { stroke: #94A3B8; stroke-width: 1.5; }')
    svg.append('  .grid { stroke: #E2E8F0; stroke-dasharray: 4,4; stroke-width: 1; }')
    svg.append('  .label { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 11px; fill: #475569; }')
    svg.append('  .legend { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 12px; font-weight: 600; fill: #334155; }')
    svg.append('  .val-text { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 9px; fill: #1E293B; text-anchor: middle; font-weight: 600; }')
    svg.append('</style>')
    svg.append(f'<rect width="{svg_width}" height="{svg_height}" fill="#FFFFFF" rx="8" />')

    # Title
    svg.append(f'<text x="{svg_width/2}" y="32" class="title" text-anchor="middle">{title}</text>')

    # Legend
    legend_x = margin["left"] + 20
    for idx, algo in enumerate(algorithms):
        lx = legend_x + idx * 160
        svg.append(f'<rect x="{lx}" y="42" width="14" height="14" fill="{colors[algo]}" rx="2" />')
        svg.append(f'<text x="{lx + 20}" y="54" class="legend">{algo}</text>')

    # Y-axis ticks and grid
    num_ticks = 5
    for i in range(num_ticks + 1):
        tick_val = (max_val / num_ticks) * i
        y_pos = margin["top"] + chart_h - (i / num_ticks) * chart_h
        svg.append(f'<line x1="{margin["left"]}" y1="{y_pos}" x2="{margin["left"] + chart_w}" y2="{y_pos}" class="grid" />')
        svg.append(f'<text x="{margin["left"] - 10}" y="{y_pos + 4}" class="label" text-anchor="end">{tick_val:.1f}</text>')

    # Y-axis label
    svg.append(f'<text x="{- (margin["top"] + chart_h/2)}" y="20" class="label" font-weight="700" text-anchor="middle" transform="rotate(-90)">{y_label}</text>')

    # X-axis
    svg.append(f'<line x1="{margin["left"]}" y1="{margin["top"] + chart_h}" x2="{margin["left"] + chart_w}" y2="{margin["top"] + chart_h}" class="axis" />')
    svg.append(f'<line x1="{margin["left"]}" y1="{margin["top"]}" x2="{margin["left"]}" y2="{margin["top"] + chart_h}" class="axis" />')

    # Bars per scenario
    group_w = chart_w / len(scenarios)
    bar_w = (group_w * 0.75) / len(algorithms)

    # Info lookup: scenario -> algo -> row
    lookup = {}
    for r in rows:
        key = (r["scenario_id"], r["algorithm"])
        lookup[key] = r

    for s_idx, scen in enumerate(scenarios):
        gx = margin["left"] + s_idx * group_w
        sample_row = next(r for r in rows if r["scenario_id"] == scen)
        scen_label = f"S{scen} (T:{sample_row['task_count']}, I:{sample_row['iterations']})"

        # Scenario label
        svg.append(f'<text x="{gx + group_w/2}" y="{margin["top"] + chart_h + 22}" class="label" text-anchor="middle" font-weight="600">{scen_label}</text>')

        for a_idx, algo in enumerate(algorithms):
            r = lookup.get((scen, algo))
            if not r:
                continue
            val = r["mean_analytical"]
            bar_h = (val / max_val) * chart_h
            bx = gx + (group_w - (bar_w * len(algorithms))) / 2 + a_idx * bar_w
            by = margin["top"] + chart_h - bar_h

            svg.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{bar_w-2:.1f}" height="{bar_h:.1f}" fill="{colors[algo]}" rx="2" opacity="0.9" />')
            if bar_h > 15:
                svg.append(f'<text x="{bx + (bar_w-2)/2:.1f}" y="{by - 4:.1f}" class="val-text">{val:.2f}</text>')

    svg.append('</svg>')
    with open(filename, "w", encoding="utf-8") as f:
        f.write("\n".join(svg))
    print(f"Generated chart: {filename}")

def generate_convergence_svg(data, filename):
    # Generates makespan vs iterations line plot for 50 and 100 tasks under Normal Workload
    normal_rows = [r for r in data if r["workload_type"].upper() == "NORMAL"]
    if not normal_rows:
        return

    svg_width = 800
    svg_height = 420
    margin = {"top": 60, "right": 50, "bottom": 60, "left": 70}
    chart_w = svg_width - margin["left"] - margin["right"]
    chart_h = svg_height - margin["top"] - margin["bottom"]

    colors = {
        "GA": "#2563EB",
        "PSO": "#DC2626",
        "ABC": "#059669",
        "Hybrid-GA-PSO": "#7C3AED"
    }

    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {svg_width} {svg_height}" width="{svg_width}" height="{svg_height}">')
    svg.append('<style>')
    svg.append('  .title { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 17px; font-weight: 700; fill: #1E293B; }')
    svg.append('  .axis { stroke: #94A3B8; stroke-width: 1.5; }')
    svg.append('  .grid { stroke: #E2E8F0; stroke-dasharray: 4,4; stroke-width: 1; }')
    svg.append('  .label { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 11px; fill: #475569; }')
    svg.append('  .legend { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 12px; font-weight: 600; fill: #334155; }')
    svg.append('</style>')
    svg.append(f'<rect width="{svg_width}" height="{svg_height}" fill="#FFFFFF" rx="8" />')

    svg.append(f'<text x="{svg_width/2}" y="32" class="title" text-anchor="middle">Makespan Convergence vs. Iterations (Normal Workload: 50 &amp; 100 Tasks)</text>')

    # Legend
    legend_x = margin["left"] + 30
    for idx, (algo, col) in enumerate(colors.items()):
        lx = legend_x + idx * 160
        svg.append(f'<line x1="{lx}" y1="46" x2="{lx+25}" y2="46" stroke="{col}" stroke-width="3" />')
        svg.append(f'<circle cx="{lx+12}" cy="46" r="4" fill="{col}" />')
        svg.append(f'<text x="{lx + 32}" y="50" class="legend">{algo}</text>')

    # Max Y
    max_val = 7.0
    min_val = 2.5

    # Grid & Y ticks
    num_ticks = 5
    for i in range(num_ticks + 1):
        tick_val = min_val + ((max_val - min_val) / num_ticks) * i
        y_pos = margin["top"] + chart_h - (i / num_ticks) * chart_h
        svg.append(f'<line x1="{margin["left"]}" y1="{y_pos}" x2="{margin["left"] + chart_w}" y2="{y_pos}" class="grid" />')
        svg.append(f'<text x="{margin["left"] - 10}" y="{y_pos + 4}" class="label" text-anchor="end">{tick_val:.2f} s</text>')

    # X axis points: 10, 50, 100 iterations
    iters = [10, 50, 100]
    iter_x = {10: margin["left"] + chart_w * 0.15, 50: margin["left"] + chart_w * 0.50, 100: margin["left"] + chart_w * 0.85}

    for it, x_pos in iter_x.items():
        svg.append(f'<line x1="{x_pos}" y1="{margin["top"]}" x2="{x_pos}" y2="{margin["top"]+chart_h}" class="grid" />')
        svg.append(f'<text x="{x_pos}" y="{margin["top"] + chart_h + 20}" class="label" text-anchor="middle" font-weight="700">{it} Iterations</text>')

    svg.append(f'<line x1="{margin["left"]}" y1="{margin["top"] + chart_h}" x2="{margin["left"] + chart_w}" y2="{margin["top"] + chart_h}" class="axis" />')
    svg.append(f'<line x1="{margin["left"]}" y1="{margin["top"]}" x2="{margin["left"]}" y2="{margin["top"] + chart_h}" class="axis" />')

    # Draw lines for 50 tasks (solid) and 100 tasks (dashed)
    for tc, dash in [(50, "none"), (100, "5,5")]:
        for algo in colors:
            points = []
            for it in iters:
                match = next((r for r in normal_rows if r["task_count"] == tc and r["iterations"] == it and r["algorithm"] == algo), None)
                if match:
                    val = match["mean_analytical"]
                    y = margin["top"] + chart_h - ((val - min_val) / (max_val - min_val)) * chart_h
                    points.append((iter_x[it], y, val))

            if len(points) == 3:
                pts_str = " ".join([f"{p[0]},{p[1]}" for p in points])
                dash_attr = f'stroke-dasharray="{dash}"' if dash != "none" else ''
                svg.append(f'<polyline points="{pts_str}" fill="none" stroke="{colors[algo]}" stroke-width="2.5" {dash_attr} opacity="0.85" />')
                for p in points:
                    svg.append(f'<circle cx="{p[0]}" cy="{p[1]}" r="4.5" fill="{colors[algo]}" />')

    # Annotation for solid vs dashed
    svg.append(f'<text x="{margin["left"] + 20}" y="{margin["top"] + 25}" class="label" font-weight="600">— Solid: 50 Tasks (Ideal: 3.0s)</text>')
    svg.append(f'<text x="{margin["left"] + 20}" y="{margin["top"] + 42}" class="label" font-weight="600">--- Dashed: 100 Tasks (Ideal: 6.0s)</text>')

    svg.append('</svg>')
    with open(filename, "w", encoding="utf-8") as f:
        f.write("\n".join(svg))
    print(f"Generated chart: {filename}")

def generate_execution_time_svg(data, filename):
    # Generates bar chart of mean execution time (ms) by algorithm
    svg_width = 750
    svg_height = 380
    margin = {"top": 60, "right": 40, "bottom": 60, "left": 70}
    chart_w = svg_width - margin["left"] - margin["right"]
    chart_h = svg_height - margin["top"] - margin["bottom"]

    algorithms = ["GA", "PSO", "ABC", "Hybrid-GA-PSO"]
    colors = {"GA": "#2563EB", "PSO": "#DC2626", "ABC": "#059669", "Hybrid-GA-PSO": "#7C3AED"}

    # Average exec time per algorithm
    avg_times = {}
    for algo in algorithms:
        times = [r["exec_time_ms"] for r in data if r["algorithm"] == algo]
        avg_times[algo] = sum(times) / len(times) if times else 0

    max_val = max(avg_times.values()) * 1.25 if avg_times else 10.0

    svg = []
    svg.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {svg_width} {svg_height}" width="{svg_width}" height="{svg_height}">')
    svg.append('<style>')
    svg.append('  .title { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 17px; font-weight: 700; fill: #1E293B; }')
    svg.append('  .axis { stroke: #94A3B8; stroke-width: 1.5; }')
    svg.append('  .grid { stroke: #E2E8F0; stroke-dasharray: 4,4; stroke-width: 1; }')
    svg.append('  .label { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 12px; fill: #475569; }')
    svg.append('  .val-text { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 13px; fill: #1E293B; font-weight: 700; text-anchor: middle; }')
    svg.append('</style>')
    svg.append(f'<rect width="{svg_width}" height="{svg_height}" fill="#FFFFFF" rx="8" />')
    svg.append(f'<text x="{svg_width/2}" y="34" class="title" text-anchor="middle">Algorithm Computational Efficiency (Mean Runtime per Trial)</text>')

    # Grid
    num_ticks = 5
    for i in range(num_ticks + 1):
        tick_val = (max_val / num_ticks) * i
        y_pos = margin["top"] + chart_h - (i / num_ticks) * chart_h
        svg.append(f'<line x1="{margin["left"]}" y1="{y_pos}" x2="{margin["left"] + chart_w}" y2="{y_pos}" class="grid" />')
        svg.append(f'<text x="{margin["left"] - 10}" y="{y_pos + 4}" class="label" text-anchor="end">{tick_val:.1f} ms</text>')

    svg.append(f'<line x1="{margin["left"]}" y1="{margin["top"] + chart_h}" x2="{margin["left"] + chart_w}" y2="{margin["top"] + chart_h}" class="axis" />')
    svg.append(f'<line x1="{margin["left"]}" y1="{margin["top"]}" x2="{margin["left"]}" y2="{margin["top"] + chart_h}" class="axis" />')

    bar_w = (chart_w / len(algorithms)) * 0.55
    spacing = chart_w / len(algorithms)

    for idx, algo in enumerate(algorithms):
        t = avg_times[algo]
        bar_h = (t / max_val) * chart_h
        bx = margin["left"] + idx * spacing + (spacing - bar_w) / 2
        by = margin["top"] + chart_h - bar_h

        svg.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{bar_w:.1f}" height="{bar_h:.1f}" fill="{colors[algo]}" rx="4" />')
        svg.append(f'<text x="{bx + bar_w/2:.1f}" y="{by - 8:.1f}" class="val-text">{t:.2f} ms</text>')
        svg.append(f'<text x="{bx + bar_w/2:.1f}" y="{margin["top"] + chart_h + 24}" class="label" text-anchor="middle" font-weight="700">{algo}</text>')

    svg.append('</svg>')
    with open(filename, "w", encoding="utf-8") as f:
        f.write("\n".join(svg))
    print(f"Generated chart: {filename}")

def generate_html_dashboard(figures_dir, data):
    # Generates a unified HTML dashboard embedding the SVG figures and summary table
    html = []
    html.append('<!DOCTYPE html>')
    html.append('<html lang="en">')
    html.append('<head>')
    html.append('  <meta charset="UTF-8">')
    html.append('  <title>Cloud Task Scheduling Research Results</title>')
    html.append('  <style>')
    html.append('    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #F8FAFC; color: #1E293B; margin: 0; padding: 24px; }')
    html.append('    .container { max-width: 1200px; margin: 0 auto; }')
    html.append('    h1 { font-size: 28px; color: #0F172A; margin-bottom: 8px; }')
    html.append('    .subtitle { color: #64748B; font-size: 16px; margin-bottom: 24px; }')
    html.append('    .card { background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); padding: 24px; margin-bottom: 24px; }')
    html.append('    .card h2 { font-size: 20px; color: #1E293B; margin-top: 0; margin-bottom: 16px; border-bottom: 1px solid #E2E8F0; padding-bottom: 8px; }')
    html.append('    .chart-container { text-align: center; overflow-x: auto; margin: 16px 0; }')
    html.append('    table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 12px; }')
    html.append('    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #E2E8F0; }')
    html.append('    th { background: #F1F5F9; font-weight: 700; color: #334155; }')
    html.append('    tr:hover { background: #F8FAFC; }')
    html.append('    .badge { display: inline-block; padding: 3px 8px; border-radius: 9999px; font-size: 11px; font-weight: 600; }')
    html.append('    .badge-ga { background: #DBEAFE; color: #1E40AF; }')
    html.append('    .badge-pso { background: #FEE2E2; color: #991B1B; }')
    html.append('    .badge-abc { background: #D1FAE5; color: #065F46; }')
    html.append('    .badge-hybrid { background: #EDE9FE; color: #5B21B6; }')
    html.append('    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }')
    html.append('    @media (max-width: 900px) { .grid-2 { grid-template-columns: 1fr; } }')
    html.append('  </style>')
    html.append('</head>')
    html.append('<body>')
    html.append('  <div class="container">')
    html.append('    <h1>Cloud Task Scheduling Research Evaluation</h1>')
    html.append('    <div class="subtitle">Comprehensive Experimental Results across GA, PSO, ABC, and Hybrid-GA-PSO on CloudSim &amp; Analytical Models</div>')

    html.append('    <div class="card">')
    html.append('      <h2>Normal Workload Makespan Comparison</h2>')
    html.append('      <div class="chart-container"><img src="makespan_comparison_normal.svg" alt="Normal Workload Comparison" style="max-width:100%;"></div>')
    html.append('    </div>')

    html.append('    <div class="card">')
    html.append('      <h2>Synthetic DDoS Workload Makespan Comparison</h2>')
    html.append('      <div class="chart-container"><img src="makespan_comparison_ddos.svg" alt="DDoS Workload Comparison" style="max-width:100%;"></div>')
    html.append('    </div>')

    html.append('    <div class="grid-2">')
    html.append('      <div class="card">')
    html.append('        <h2>Convergence Trajectory</h2>')
    html.append('        <div class="chart-container"><img src="convergence_iterations.svg" alt="Convergence" style="max-width:100%;"></div>')
    html.append('      </div>')
    html.append('      <div class="card">')
    html.append('        <h2>Algorithm Execution Runtime (ms)</h2>')
    html.append('        <div class="chart-container"><img src="execution_time_comparison.svg" alt="Execution Time" style="max-width:100%;"></div>')
    html.append('      </div>')
    html.append('    </div>')

    html.append('    <div class="card">')
    html.append('      <h2>Complete Experimental Summary (Table 3 Scenarios)</h2>')
    html.append('      <table>')
    html.append('        <thead>')
    html.append('          <tr><th>Workload</th><th>Scenario</th><th>Tasks</th><th>Iter</th><th>Algorithm</th><th>Mean Makespan</th><th>CloudSim Makespan</th><th>Std Dev</th><th>Runtime</th><th>Imprv vs PSO</th></tr>')
    html.append('        </thead>')
    html.append('        <tbody>')

    for r in data:
        badge_cls = "badge-ga" if r["algorithm"] == "GA" else ("badge-pso" if r["algorithm"] == "PSO" else ("badge-abc" if r["algorithm"] == "ABC" else "badge-hybrid"))
        html.append(f'          <tr><td><b>{r["workload_type"]}</b></td><td>Scenario {r["scenario_id"]}</td><td>{r["task_count"]}</td><td>{r["iterations"]}</td><td><span class="badge {badge_cls}">{r["algorithm"]}</span></td><td><b>{r["mean_analytical"]:.4f} s</b></td><td>{r["mean_cloudsim"]:.4f} s</td><td>{r["std_analytical"]:.4f}</td><td>{r["exec_time_ms"]:.2f} ms</td><td><b>{r["imprv_vs_pso"]:+.2f}%</b></td></tr>')

    html.append('        </tbody>')
    html.append('      </table>')
    html.append('    </div>')
    html.append('  </div>')
    html.append('</body>')
    html.append('</html>')

    dash_path = os.path.join(figures_dir, "dashboard.html")
    with open(dash_path, "w", encoding="utf-8") as f:
        f.write("\n".join(html))
    print(f"Generated dashboard: {dash_path}")

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    summary_csv = os.path.join(repo_root, "results", "summary", "experiment_summary.csv")
    figures_dir = os.path.join(repo_root, "results", "figures")
    os.makedirs(figures_dir, exist_ok=True)

    data = load_summary_data(summary_csv)
    if not data:
        print("No summary data available to generate charts.")
        sys.exit(1)

    generate_grouped_bar_svg(
        data,
        "NORMAL",
        "Normal Workload Task Scheduling: Makespan Across Table 3 Scenarios",
        os.path.join(figures_dir, "makespan_comparison_normal.svg")
    )

    generate_grouped_bar_svg(
        data,
        "DDOS",
        "Synthetic DDoS Attack Workload: Makespan Across Table 3 Scenarios",
        os.path.join(figures_dir, "makespan_comparison_ddos.svg")
    )

    generate_convergence_svg(
        data,
        os.path.join(figures_dir, "convergence_iterations.svg")
    )

    generate_execution_time_svg(
        data,
        os.path.join(figures_dir, "execution_time_comparison.svg")
    )

    generate_html_dashboard(figures_dir, data)
    print("All figures and dashboard successfully generated!")

if __name__ == "__main__":
    main()
