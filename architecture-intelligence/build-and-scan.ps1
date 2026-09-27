param(
    [switch]$SkipBuild,
    [switch]$Serve
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$services = @(
    'gateway',
    'user-service',
    'customer-service',
    'customer-service-command',
    'customer-service-query',
    'money-service',
    'money-service-command',
    'money-service-query',
    'transaction-service',
    'fraud-service',
    'admin-service',
    'admin-service-command',
    'admin-service-query'
)

$bundledMaven = Join-Path $repoRoot '.tools/apache-maven-3.9.9/bin/mvn.cmd'
$maven = if (Test-Path $bundledMaven) { $bundledMaven } else { 'mvn' }

if (-not $SkipBuild) {
    foreach ($service in $services) {
        Write-Host "Building $service"
        & $maven -B -q -f (Join-Path $repoRoot "$service/pom.xml") -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed for $service"
        }
    }
}

$bundledJqAssistant = Join-Path $repoRoot '.tools/jqassistant-2.9.1/jqassistant-commandline-neo4jv5-2.9.1/bin/jqassistant.cmd'
$jqassistantCommand = Get-Command jqassistant -ErrorAction SilentlyContinue
$jqassistant = if (Test-Path $bundledJqAssistant) {
    $bundledJqAssistant
} elseif ($jqassistantCommand) {
    $jqassistantCommand.Source
} else {
    $null
}
if (-not $jqassistant) {
    throw 'jQAssistant CLI is not on PATH. Install jQAssistant 2.9.x, then run this script again.'
}

Push-Location $repoRoot
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
try {
    # XO 2.6.x lower-cases JavaBean prefixes with the host locale. A Turkish
    # locale turns "IS" into "is" with a dotless i and hides boolean getters.
    $localeOptions = '-Duser.language=en -Duser.country=US'
    $env:JAVA_TOOL_OPTIONS = if ($previousJavaToolOptions) {
        "$previousJavaToolOptions $localeOptions"
    } else {
        $localeOptions
    }

    & $jqassistant scan analyze
    if ($LASTEXITCODE -ne 0) {
        throw 'jQAssistant scan/analyze failed.'
    }
    if ($Serve) {
        & $jqassistant server
    }
}
finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    Pop-Location
}
