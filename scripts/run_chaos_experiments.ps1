# Chaos-Enhanced PSO Experiment Pipeline Runner
# Compiles Java, runs validation tests, executes 3-way matched experiments, and generates charts.

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "       Chaos-Enhanced PSO Cloud Scheduling Experiment Pipeline  " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Repository Root: $RepoRoot"

# 1. Locate Java tools
$javac = "javac"
$java = "java"
if (Test-Path "C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe") {
    $javac = "C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe"
    $java = "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe"
}

$cloudSimJar = Join-Path $RepoRoot "cloudsim-3.0.3\jars\cloudsim-3.0.3.jar"
$outDir = Join-Path $RepoRoot "out"
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

# 2. Compile Java sources
$sourceFiles = Get-ChildItem -Path (Join-Path $RepoRoot "src") -Filter *.java -Recurse | ForEach-Object { $_.FullName }
Write-Host "`n[1/4] Compiling $($sourceFiles.Count) Java source files..." -ForegroundColor Yellow

$classpath = "$cloudSimJar;$outDir"
& $javac -cp $classpath -d $outDir $sourceFiles
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed with exit code $LASTEXITCODE"
    exit 1
}
Write-Host "Compilation successful!" -ForegroundColor Green

# 3. Run Validation Tests
Write-Host "`n[2/4] Running Chaos-PSO and Baseline Equivalence Tests..." -ForegroundColor Yellow
& $java -cp $classpath core.ChaosParticleSwarmSchedulerTest
if ($LASTEXITCODE -ne 0) {
    Write-Error "Validation tests failed!"
    exit 1
}

# 4. Execute 3-way Experiment Pipeline
Write-Host "`n[3/4] Executing 360 Matched Experiment Trials (Baseline vs LDIW vs Chaos-PSO)..." -ForegroundColor Yellow
& $java -cp $classpath core.ChaosPsoExperimentRunner
if ($LASTEXITCODE -ne 0) {
    Write-Error "Experiment execution failed!"
    exit 1
}

# 5. Generate Charts (if python script exists)
$chartScript = Join-Path $RepoRoot "scripts\generate_chaos_charts.py"
if (Test-Path $chartScript) {
    Write-Host "`n[4/4] Generating publication-quality comparison charts..." -ForegroundColor Yellow
    python $chartScript
} else {
    Write-Host "`n[4/4] Chart script not yet present; skipping charts." -ForegroundColor Gray
}

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "            CHAOS-PSO EXPERIMENT PIPELINE COMPLETE              " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Raw CSV: $RepoRoot\results\raw\chaos_pso_experiment_results.csv" -ForegroundColor Green
Write-Host "Trajectories CSV: $RepoRoot\results\raw\chaos_pso_convergence_trajectories.csv" -ForegroundColor Green
Write-Host "Summary CSV: $RepoRoot\results\summary\chaos_pso_comparison_summary.csv" -ForegroundColor Green

