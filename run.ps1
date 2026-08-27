<#
.SYNOPSIS
  Loads .env into the process environment, then runs one of the Clamped jars.

.USAGE
  .\run.ps1 server              # clamped-server, http://localhost:8080
  .\run.ps1 demo                # clamped-demo (throws sample errors at Clamped.add)
  .\run.ps1 cli list             # clamped-cli, extra args passed through verbatim
  .\run.ps1 cli show 42
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("server", "demo", "cli")]
    [string]$Target,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Args
)

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
} else {
    Write-Warning ".env not found - copy .env.example to .env and fill in real values first."
    exit 1
}

$jars = @{
    server = "clamped-server\target\clamped-server-1.0.0-SNAPSHOT.jar"
    demo   = "clamped-demo\target\clamped-demo.jar"
    cli    = "clamped-cli\target\clamped-cli-1.0.0-SNAPSHOT.jar"
}
$jarPath = Join-Path $PSScriptRoot $jars[$Target]

if (-not (Test-Path $jarPath)) {
    Write-Error "$jarPath not found - build first: .\mvnw.cmd -pl clamped-core,clamped-server,clamped-cli,clamped-demo -am package -DskipTests"
    exit 1
}

& java -jar $jarPath @Args
exit $LASTEXITCODE
