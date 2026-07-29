param(
  [switch]$RunApiSmoke,
  [switch]$SkipBackend,
  [switch]$SkipFrontend,
  [switch]$SkipMiniProgram
)

$ErrorActionPreference = "Continue"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$BackendDir = Join-Path $Root "BiteSmart"
$FrontendDir = Join-Path $Root "BiteSmart_front"
$MiniDir = Join-Path $Root "BiteSmartMin"
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ReportDir = Join-Path $Root "outputs/test-runs/$Timestamp"
$ReportPath = Join-Path $ReportDir "report.md"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

$Results = New-Object System.Collections.Generic.List[object]

function Add-ReportLine {
  param([string]$Text)
  Add-Content -LiteralPath $ReportPath -Encoding UTF8 -Value $Text
}

function Find-CommandPath {
  param([string[]]$Candidates)
  foreach ($candidate in $Candidates) {
    if ([System.IO.Path]::IsPathRooted($candidate)) {
      if (Test-Path -LiteralPath $candidate) { return $candidate }
    } else {
      $cmd = Get-Command $candidate -ErrorAction SilentlyContinue
      if ($cmd) { return $cmd.Source }
    }
  }
  return $null
}

function Invoke-Step {
  param(
    [string]$Name,
    [string]$Command,
    [string]$WorkingDirectory,
    [hashtable]$ExtraEnv = @{}
  )

  Write-Host ""
  Write-Host "==> $Name"
  $LogPath = Join-Path $ReportDir (($Name -replace '[\\/:*?"<>| ]+', '_') + ".log")

  $oldEnv = @{}
  foreach ($key in $ExtraEnv.Keys) {
    $oldEnv[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
    [Environment]::SetEnvironmentVariable($key, [string]$ExtraEnv[$key], "Process")
  }

  Push-Location $WorkingDirectory
  try {
    # Run in this process so local Maven/Node caches and workspace permissions are preserved on Windows.
    $output = Invoke-Expression $Command 2>&1
    $exitCode = $LASTEXITCODE
    if ($null -eq $exitCode) { $exitCode = 0 }
    $output | Out-File -LiteralPath $LogPath -Encoding UTF8
  } finally {
    Pop-Location
    foreach ($key in $ExtraEnv.Keys) {
      [Environment]::SetEnvironmentVariable($key, $oldEnv[$key], "Process")
    }
  }

  $status = if ($exitCode -eq 0) { "PASS" } else { "FAIL" }
  $Results.Add([pscustomobject]@{
    Name = $Name
    Status = $status
    ExitCode = $exitCode
    Log = $LogPath
  }) | Out-Null

  Write-Host "$status $Name"
  return ($exitCode -eq 0)
}

function Quote-Arg {
  param([string]$Value)
  return "'" + ($Value -replace "'", "''") + "'"
}

$Python = Find-CommandPath @(
  "C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe",
  "python",
  "python.exe",
  "py"
)
$Node = Find-CommandPath @(
  "C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe",
  "node",
  "node.exe"
)
$Maven = Find-CommandPath @("mvn.cmd", "mvn", (Join-Path $BackendDir "mvnw.cmd"))
$Npm = Find-CommandPath @("npm.cmd", "npm")
$VueTsc = Join-Path $FrontendDir "node_modules/.bin/vue-tsc.cmd"
$Vite = Join-Path $FrontendDir "node_modules/.bin/vite.cmd"
$MavenRepo = Join-Path $env:USERPROFILE ".m2\repository"

Set-Content -LiteralPath $ReportPath -Encoding UTF8 -Value "# BiteSmart Test Run Report"
Add-ReportLine ""
Add-ReportLine "- Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Add-ReportLine "- Root: $Root"
Add-ReportLine "- Backend: $BackendDir"
Add-ReportLine "- PC frontend: $FrontendDir"
Add-ReportLine "- Mini program: $MiniDir"
Add-ReportLine ""

if (-not $SkipBackend) {
  if ($Maven) {
    $mavenEnv = @{}
    if (Test-Path -LiteralPath $MavenRepo) {
      # Maven 3.9 reads MAVEN_ARGS before CLI arguments and honors the repository consistently on Windows.
      $mavenEnv["MAVEN_ARGS"] = "-Dmaven.repo.local=$MavenRepo"
    }
    Invoke-Step -Name "backend_maven_test" -Command ("& " + (Quote-Arg $Maven) + " test") -WorkingDirectory $BackendDir -ExtraEnv $mavenEnv | Out-Null
  } else {
    $Results.Add([pscustomobject]@{ Name = "backend_maven_test"; Status = "SKIP"; ExitCode = ""; Log = "Maven not found" }) | Out-Null
  }
}

if ($Python) {
  Invoke-Step -Name "db_static_coverage_audit" -Command ("& " + (Quote-Arg $Python) + " " + (Quote-Arg (Join-Path $Root "tools/audit_db_coverage.py")) + " --out " + (Quote-Arg (Join-Path $ReportDir "db-coverage.md"))) -WorkingDirectory $Root | Out-Null
} else {
  $Results.Add([pscustomobject]@{ Name = "db_static_coverage_audit"; Status = "SKIP"; ExitCode = ""; Log = "Python not found" }) | Out-Null
}

if (-not $SkipFrontend) {
  $nodeDir = if ($Node) { Split-Path -Parent $Node } else { "" }
  $frontendEnv = @{ BITESMART_TEST_WORKFLOW = "1" }
  if ($nodeDir) {
    $frontendEnv["Path"] = $nodeDir + ";" + $env:Path
  }
  if ((Test-Path -LiteralPath $VueTsc) -and (Test-Path -LiteralPath $Vite) -and $Node) {
    Invoke-Step -Name "frontend_type_check" -Command ("& " + (Quote-Arg $VueTsc) + " --noEmit") -WorkingDirectory $FrontendDir -ExtraEnv $frontendEnv | Out-Null
    $frontendDist = Join-Path $ReportDir "frontend-dist"
    Invoke-Step -Name "frontend_vite_build" -Command ("& " + (Quote-Arg $Vite) + " build --configLoader runner --emptyOutDir false --outDir " + (Quote-Arg $frontendDist)) -WorkingDirectory $FrontendDir -ExtraEnv $frontendEnv | Out-Null
  } else {
    $Results.Add([pscustomobject]@{ Name = "frontend_build"; Status = "SKIP"; ExitCode = ""; Log = "node or local node_modules/.bin tools not found" }) | Out-Null
  }
}

if (-not $SkipMiniProgram) {
  if ($Python) {
    Invoke-Step -Name "miniprogram_page_static_check" -Command ("& " + (Quote-Arg $Python) + " " + (Quote-Arg (Join-Path $Root "tools/check_pages.py"))) -WorkingDirectory $Root | Out-Null
  } else {
    $Results.Add([pscustomobject]@{ Name = "miniprogram_page_static_check"; Status = "SKIP"; ExitCode = ""; Log = "Python not found" }) | Out-Null
  }

  if ($Node) {
    Invoke-Step -Name "miniprogram_ts_syntax_check" -Command ("& " + (Quote-Arg $Node) + " " + (Quote-Arg (Join-Path $Root "tools/ts_syntax_check.js"))) -WorkingDirectory $Root | Out-Null
  } else {
    $Results.Add([pscustomobject]@{ Name = "miniprogram_ts_syntax_check"; Status = "SKIP"; ExitCode = ""; Log = "Node not found" }) | Out-Null
  }
}

if ($RunApiSmoke) {
  if ($Python) {
    Invoke-Step -Name "api_smoke_plan" -Command ("& " + (Quote-Arg $Python) + " " + (Quote-Arg (Join-Path $Root "tools/smoke_plan_api.py"))) -WorkingDirectory $Root | Out-Null
    Invoke-Step -Name "api_smoke_rider" -Command ("& " + (Quote-Arg $Python) + " " + (Quote-Arg (Join-Path $Root "tools/smoke_rider_api.py"))) -WorkingDirectory $Root | Out-Null
    Invoke-Step -Name "api_smoke_shop_plandays" -Command ("& " + (Quote-Arg $Python) + " " + (Quote-Arg (Join-Path $Root "tools/smoke_shop_plandays.py"))) -WorkingDirectory $Root | Out-Null
  } else {
    $Results.Add([pscustomobject]@{ Name = "api_smoke"; Status = "SKIP"; ExitCode = ""; Log = "Python not found" }) | Out-Null
  }
}

Add-ReportLine "## Results"
Add-ReportLine ""
Add-ReportLine "| Stage | Status | Exit code | Log |"
Add-ReportLine "|---|---:|---:|---|"
foreach ($r in $Results) {
  $logValue = if ($r.Log -and (Test-Path -LiteralPath $r.Log)) { $r.Log } else { $r.Log }
  Add-ReportLine "| $($r.Name) | $($r.Status) | $($r.ExitCode) | $logValue |"
}

$failed = @($Results | Where-Object { $_.Status -eq "FAIL" })
$skipped = @($Results | Where-Object { $_.Status -eq "SKIP" })

Add-ReportLine ""
Add-ReportLine "## Summary"
Add-ReportLine ""
if ($failed.Count -eq 0) {
  Add-ReportLine "- No failed automated checks."
} else {
  Add-ReportLine "- Failed checks: $($failed.Count). Review the linked logs first."
}
if ($skipped.Count -gt 0) {
  Add-ReportLine "- Skipped checks: $($skipped.Count). This usually means a local tool is missing or a skip flag was used."
}

Write-Host ""
Write-Host "Report: $ReportPath"
if ($failed.Count -gt 0) {
  exit 1
}
exit 0
