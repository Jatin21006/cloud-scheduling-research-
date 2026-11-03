# Run Experiments Script for Cloud Task Scheduling Research
# Builds code, runs matched experiments, generates statistical summaries and figures.

param(
    [string]$Scenario = "ALL",
    [string]$Workload = "ALL",
    [string]$Algorithm = "ALL",
    [switch]$SkipCloudSim,
    [int]$VmCount = 10
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
Set-Location $RepoRoot

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "       Cloud Task Scheduling Research - Experiment Pipeline     " -ForegroundColor Cyan
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

$sourceFiles = Get-ChildItem -Path (Join-Path $RepoRoot "src") -Filter *.java -Recurse | ForEach-Object { $_.FullName }
Write-Host "`n[1/3] Compiling $($sourceFiles.Count) Java source files..." -ForegroundColor Yellow

$classpath = "$cloudSimJar;$outDir"
& $javac -cp $classpath -d $outDir $sourceFiles
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed with exit code $LASTEXITCODE"
    exit 1
}
Write-Host "Build complete!" -ForegroundColor Green

# 2. Run Experiments via UnifiedExperimentRunner
Write-Host "`n[2/3] Executing experiments matrix..." -ForegroundColor Yellow
$runnerArgs = @()

if ($Scenario -ne "ALL") {
    $runnerArgs += "--scenario"
    $runnerArgs += $Scenario
}
if ($Workload -ne "ALL") {
    $runnerArgs += "--workload"
    $runnerArgs += $Workload
}
if ($Algorithm -ne "ALL") {
    $runnerArgs += "--algorithm"
    $runnerArgs += $Algorithm
}
if ($SkipCloudSim) {
    $runnerArgs += "--cloudsim"
    $runnerArgs += "false"
}
$runnerArgs += "--vmCount"
$runnerArgs += "$VmCount"
$runnerArgs += "--outputDir"
$runnerArgs += "results"

& $java -cp $classpath core.UnifiedExperimentRunner $runnerArgs
if ($LASTEXITCODE -ne 0) {
    Write-Error "Experiment execution failed with exit code $LASTEXITCODE"
    exit 1
}

# 3. Generate Charts & Figures
Write-Host "`n[3/3] Generating statistical charts and figures..." -ForegroundColor Yellow
$python = "python"
if (Get-Command python -ErrorAction SilentlyContinue) {
    & $python (Join-Path $RepoRoot "scripts\generate_charts.py")
} else {
    Write-Host "Warning: python not found on PATH; skipping chart generation." -ForegroundColor Yellow
}

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "                   EXPERIMENT RUN COMPLETE                      " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Raw CSV Results : $RepoRoot\results\raw\raw_experiment_results.csv" -ForegroundColor Green
Write-Host "Summary CSV     : $RepoRoot\results\summary\experiment_summary.csv" -ForegroundColor Green
Write-Host "Figures & HTML  : $RepoRoot\results\figures\" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Cyan
