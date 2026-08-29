<#
.SYNOPSIS
  Installs a `clamped` command on PATH that works from any shell
  (PowerShell, cmd.exe, Git Bash) - not just PowerShell profiles.

.USAGE
  .\scripts\install-cli.ps1
#>

$RepoRoot = Split-Path $PSScriptRoot -Parent
$JarPath = Join-Path $RepoRoot "clamped-cli\target\clamped-cli-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $JarPath)) {
    Write-Error "$JarPath not found - build first: .\mvnw.cmd -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests"
    exit 1
}

$BinDir = Join-Path $env:USERPROFILE "bin"
New-Item -ItemType Directory -Force -Path $BinDir | Out-Null

$ShimPath = Join-Path $BinDir "clamped.cmd"
@"
@echo off
java -jar "$JarPath" %*
"@ | Set-Content -Path $ShimPath -Encoding ascii

Write-Output "Installed shim: $ShimPath"

$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if (($UserPath -split ";") -notcontains $BinDir) {
    $NewPath = if ([string]::IsNullOrEmpty($UserPath)) { $BinDir } else { "$UserPath;$BinDir" }
    [Environment]::SetEnvironmentVariable("Path", $NewPath, "User")
    Write-Output "Added $BinDir to your User PATH."
    Write-Warning "Open a NEW terminal window for the PATH change to take effect."
} else {
    Write-Output "$BinDir is already on PATH."
}

Write-Output ""
Write-Output "Once in a fresh terminal, 'clamped list' works from PowerShell, cmd.exe, or Git Bash."
