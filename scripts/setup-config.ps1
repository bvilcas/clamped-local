<#
.SYNOPSIS
  One-time setup: derives ~/.clamped/config.properties from .env, so the
  server/CLI/demo jars can be run directly with plain `java -jar ...` -
  no wrapper script, no env vars needed per-session.

.USAGE
  .\scripts\setup-config.ps1
#>

$RepoRoot = Split-Path $PSScriptRoot -Parent
$EnvFile = Join-Path $RepoRoot ".env"

if (-not (Test-Path $EnvFile)) {
    Write-Error ".env not found - copy .env.example to .env and fill in real values first."
    exit 1
}

$EnvVars = @{}
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { return }
    $EnvVars[$line.Substring(0, $idx).Trim()] = $line.Substring($idx + 1).Trim()
}

if (-not $EnvVars.ContainsKey("POSTGRES_PASSWORD") -or [string]::IsNullOrEmpty($EnvVars["POSTGRES_PASSWORD"])) {
    Write-Error "POSTGRES_PASSWORD not set in .env"
    exit 1
}

$Db = if ($EnvVars.ContainsKey("POSTGRES_DB")) { $EnvVars["POSTGRES_DB"] } else { "clamped_db" }
$User = if ($EnvVars.ContainsKey("POSTGRES_USER")) { $EnvVars["POSTGRES_USER"] } else { "postgres" }
$Password = $EnvVars["POSTGRES_PASSWORD"]

$ConfigDir = Join-Path $HOME ".clamped"
New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null
$ConfigFile = Join-Path $ConfigDir "config.properties"

@"
jdbcUrl=jdbc:postgresql://localhost:5432/$Db
username=$User
password=$Password
"@ | Set-Content -Path $ConfigFile -Encoding ascii

Write-Output "Wrote $ConfigFile"
Write-Output "You can now run any of the jars directly, e.g.:"
Write-Output "  java -jar clamped-server/target/clamped-server-1.0.0-SNAPSHOT.jar"
