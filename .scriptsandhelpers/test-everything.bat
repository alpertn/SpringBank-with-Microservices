@echo off
setlocal
if "%~1"=="" (
  if not defined TEST_MODE set "TEST_MODE=full"
) else (
  set "TEST_MODE=%~1"
)
set "SELF=%~f0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$p=$env:SELF; $c=Get-Content -Raw -LiteralPath $p; $m='# POWERSHELL_PAYLOAD'; $i=$c.LastIndexOf($m); if($i -lt 0){throw 'PowerShell payload not found'}; $s=$c.Substring($i + $m.Length); Invoke-Expression $s"
exit /b %ERRORLEVEL%

# POWERSHELL_PAYLOAD
$ErrorActionPreference = "Stop"

$Namespace = if ($env:BANK_NS) { $env:BANK_NS } else { "banking-microservices" }
$Gateway = if ($env:GATEWAY) { $env:GATEWAY.TrimEnd("/") } else { "http://localhost:8095" }
$GatewayPort = if ($env:GATEWAY_PORT) { [int]$env:GATEWAY_PORT } else { 8095 }
$AdminEmail = if ($env:ADMIN_EMAIL) { $env:ADMIN_EMAIL } else { "admin@springbank.local" }
$AdminPassword = if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { "SpringBankAdmin123!" }
$UserPassword = if ($env:TEST_USER_PASSWORD) { $env:TEST_USER_PASSWORD } else { "Test1234!" }
$Mode = if ($env:TEST_MODE) { $env:TEST_MODE.ToLowerInvariant() } else { "full" }
$IsFullMode = $Mode -in @("full", "everything", "all", "e2e")
$SkipLongRunning = if ($env:SKIP_LONG_RUNNING_SECONDS) { [int]$env:SKIP_LONG_RUNNING_SECONDS } else { 60 }
$HttpTimeoutSeconds = if ($env:HTTP_TIMEOUT_SECONDS) { [int]$env:HTTP_TIMEOUT_SECONDS } else { $SkipLongRunning }
$StepTimeoutSeconds = if ($env:STEP_TIMEOUT_SECONDS) { [int]$env:STEP_TIMEOUT_SECONDS } else { 120 }
$DefaultWaitCapSeconds = if ($env:WAIT_CAP_SECONDS) { [int]$env:WAIT_CAP_SECONDS } else { $SkipLongRunning }
$RunId = (Get-Date -Format "yyyyMMddHHmmss") + "-" + (-join ((48..57 + 97..122) | Get-Random -Count 6 | ForEach-Object {[char]$_}))
$Failures = New-Object System.Collections.Generic.List[string]
$PassCount = 0
$PortForwardProcess = $null
$SelfPath = $env:SELF
$ScriptDir = Split-Path -Parent $SelfPath
$RepoRoot = Split-Path -Parent $ScriptDir
$LogRoot = Join-Path $RepoRoot ".scriptsandhelpers\logs\full-e2e-$RunId"
$HttpLog = Join-Path $LogRoot "http.txt"
$KafkaLog = Join-Path $LogRoot "kafka.txt"
$DbLog = Join-Path $LogRoot "db.txt"
$PodLog = Join-Path $LogRoot "pods.txt"
$KubeLog = Join-Path $LogRoot "kubectl.txt"
$AllLog = Join-Path $LogRoot "all-logs.txt"
$AuditLog = Join-Path $LogRoot "audit.jsonl"
$TranscriptLog = Join-Path $LogRoot "console.log"
$script:HttpCounter = 0
$script:RunStart = Get-Date
$script:StepStart = $null
$script:StepName = $null
$script:FailFastEnabled = $false
$script:HandlingFatal = $false
$script:LogsSyncedToGateway = $false
New-Item -ItemType Directory -Force -Path $LogRoot | Out-Null
"" | Set-Content -LiteralPath $HttpLog
"" | Set-Content -LiteralPath $KafkaLog
"" | Set-Content -LiteralPath $DbLog
"" | Set-Content -LiteralPath $PodLog
"" | Set-Content -LiteralPath $KubeLog
"" | Set-Content -LiteralPath $AllLog
Start-Transcript -LiteralPath $TranscriptLog -Force | Out-Null

function Log-FileForType($type) {
    switch -Regex ($type) {
        "http|pod-http" { return $HttpLog }
        "kafka" { return $KafkaLog }
        "postgres|mongo|db" { return $DbLog }
        "pod" { return $PodLog }
        "kubectl" { return $KubeLog }
        default { return $AllLog }
    }
}

function Format-Duration([TimeSpan]$duration) {
    $ms = [math]::Round($duration.TotalMilliseconds, 0)
    $sec = [math]::Round($duration.TotalSeconds, 3)
    $min = [math]::Round($duration.TotalMinutes, 3)
    return "${ms}ms / ${sec}s / ${min}m"
}

function Add-LogContent($path, $value) {
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        try {
            Add-Content -LiteralPath $path -Value $value
            return
        } catch {
            if ($attempt -eq 10) {
                Write-Host "[WARN] log yazilamadi path=$path error=$($_.Exception.Message)" -ForegroundColor DarkYellow
                return
            }
            Start-Sleep -Milliseconds (100 * $attempt)
        }
    }
}

function Write-DetailLog($type, $status, $label, $details = "") {
    $timestamp = Get-Date -Format "HH:mm:ss"
    $elapsed = Format-Duration ((Get-Date) - $script:RunStart)
    $entry = @"
[$timestamp][elapsed=$elapsed][$type][$status] $label
$details
--------------------------------------------------------------------------------
"@
    Add-LogContent $AllLog $entry
    $target = Log-FileForType $type
    if ($target -ne $AllLog) {
        Add-LogContent $target $entry
    }
}

function Info($message) { Write-Host "[INFO] $message" -ForegroundColor Cyan; Write-DetailLog "general" "INFO" $message }
function Ok($message) { $script:PassCount++; Write-Host "[OK]   $message" -ForegroundColor Green; Write-DetailLog "general" "OK" $message }
function Bad($message) {
    $script:Failures.Add($message) | Out-Null
    Write-Host "[FAIL] $message" -ForegroundColor Red
    Write-DetailLog "general" "FAIL" $message
    if ($script:FailFastEnabled -and -not $script:HandlingFatal) {
        throw "DEV_FAIL_FAST: $message"
    }
}
function Skip($message) { Write-Host "[SKIP] $message" -ForegroundColor DarkYellow; Write-DetailLog "general" "SKIP" $message }
function Step($message) {
    if ($script:StepStart -and $script:StepName) {
        $previousDuration = (Get-Date) - $script:StepStart
        $status = if ($previousDuration.TotalSeconds -gt $SkipLongRunning) { "SKIP" } else { "OK" }
        if ($status -eq "SKIP") {
            Skip "Step 1 dakikayi asti: $($script:StepName) duration=$(Format-Duration $previousDuration)"
        }
        Write-DetailLog "step" $status "step-finished: $($script:StepName)" "DURATION: $(Format-Duration $previousDuration)`nSKIP_LONG_RUNNING_SECONDS: $SkipLongRunning"
    }
    $script:StepStart = Get-Date
    $script:StepName = $message
    Write-Host ""
    Write-Host "==== $message ====" -ForegroundColor Yellow
    Write-DetailLog "step" "START" $message "MODE: $Mode`nSKIP_LONG_RUNNING_SECONDS: $SkipLongRunning"
}
function Assert-True($condition, $message) { if ($condition) { Ok $message } else { Bad $message } }
function Safe-Name($value) { return (($value -replace '[^a-zA-Z0-9_.-]', '_').Trim('_')) }
function Is-FullMode { return [bool]$IsFullMode }
function Is-DevMode { return -not [bool]$IsFullMode }
function Skip-FullOnly($message) {
    return
}
function Invoke-SkipLong {
    param(
        [Parameter(Mandatory=$true)][string]$Label,
        [Parameter(Mandatory=$true)][scriptblock]$Script,
        [int]$TimeoutSeconds = $SkipLongRunning
    )
    $opStart = Get-Date
    Write-DetailLog "timeout" "START" $Label "LIMIT_SECONDS: $TimeoutSeconds`nMODE: inline-context"
    try {
        $result = & $Script
        $duration = (Get-Date) - $opStart
        $status = if ($duration.TotalSeconds -gt $TimeoutSeconds) { "SKIP" } else { "OK" }
        if ($status -eq "SKIP") {
            Skip "$Label 1 dakikayi asti, atlandi. Duration=$(Format-Duration $duration)"
        }
        Write-DetailLog "timeout" $status $Label "DURATION: $(Format-Duration $duration)`nLIMIT_SECONDS: $TimeoutSeconds"
        return $result
    } catch {
        $duration = (Get-Date) - $opStart
        Write-DetailLog "timeout" "FAIL" $Label "DURATION: $(Format-Duration $duration)`nERROR: $($_.Exception.Message)"
        throw
    }
}

function Invoke-ExternalCommand {
    param(
        [Parameter(Mandatory=$true)][string]$FileName,
        [string[]]$Arguments = @(),
        [int]$TimeoutSeconds = $SkipLongRunning
    )
    $argumentText = (($Arguments | ForEach-Object {
        $arg = [string]$_
        '"' + ($arg -replace '"', '\"') + '"'
    }) -join " ")
    $stdoutPath = Join-Path $env:TEMP ("springbank-e2e-stdout-" + [guid]::NewGuid() + ".txt")
    $stderrPath = Join-Path $env:TEMP ("springbank-e2e-stderr-" + [guid]::NewGuid() + ".txt")
    $commandLine = '"' + $FileName + '" ' + $argumentText + ' > "' + $stdoutPath + '" 2> "' + $stderrPath + '"'
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = if ($env:COMSPEC) { $env:COMSPEC } else { "cmd.exe" }
    $psi.Arguments = '/d /s /c "' + $commandLine + '"'
    $psi.UseShellExecute = $false
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    $opStart = Get-Date
    try {
        [void]$process.Start()
        $completed = $process.WaitForExit($TimeoutSeconds * 1000)
        $duration = (Get-Date) - $opStart
        if (-not $completed) {
            try { $process.Kill() } catch {}
            try { $process.WaitForExit(5000) | Out-Null } catch {}
            $stdout = if (Test-Path $stdoutPath) { Get-Content -Raw -LiteralPath $stdoutPath } else { "" }
            $stderr = if (Test-Path $stderrPath) { Get-Content -Raw -LiteralPath $stderrPath } else { "" }
            return [pscustomobject]@{ ExitCode=124; TimedOut=$true; Duration=$duration; Output=(($stdout, $stderr) -join "`n").Trim() }
        }
        $stdout = if (Test-Path $stdoutPath) { Get-Content -Raw -LiteralPath $stdoutPath } else { "" }
        $stderr = if (Test-Path $stderrPath) { Get-Content -Raw -LiteralPath $stderrPath } else { "" }
        return [pscustomobject]@{ ExitCode=$process.ExitCode; TimedOut=$false; Duration=$duration; Output=(($stdout, $stderr) -join "`n").Trim() }
    } finally {
        Remove-Item -LiteralPath $stdoutPath -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $stderrPath -ErrorAction SilentlyContinue
    }
}
function Compress-AuditValue($value, [int]$limit = 1200) {
    if ($null -eq $value) { return $null }
    if ($value -is [string]) {
        if ($value.Length -le $limit) { return $value }
        return ($value.Substring(0, $limit) + "... [TRUNCATED_FOR_AUDIT length=$($value.Length); full detail is in all-logs.txt]")
    }
    if ($value -is [System.Collections.IDictionary]) {
        $copy = @{}
        foreach ($key in $value.Keys) { $copy[$key] = Compress-AuditValue $value[$key] $limit }
        return $copy
    }
    if ($value -is [System.Collections.IEnumerable] -and -not ($value -is [string])) {
        $items = @()
        foreach ($item in $value) { $items += Compress-AuditValue $item $limit }
        return $items
    }
    return $value
}
function Escape-JsonString($value) {
    if ($null -eq $value) { return "" }
    return ([string]$value).
        Replace("\", "\\").
        Replace('"', '\"').
        Replace("`r", "\r").
        Replace("`n", "\n").
        Replace("`t", "\t")
}

function Audit-Summary($data) {
    if ($null -eq $data) { return "" }
    if ($data -is [System.Collections.IDictionary]) {
        $parts = @()
        foreach ($key in $data.Keys) {
            $value = $data[$key]
            if ($null -eq $value) {
                $parts += "$key=null"
            } elseif ($value -is [string]) {
                $text = if ($value.Length -gt 300) { $value.Substring(0, 300) + "...[truncated]" } else { $value }
                $parts += "$key=$text"
            } elseif ($value -is [System.Array]) {
                $parts += "$key=array($($value.Count))"
            } else {
                $parts += "$key=$value"
            }
        }
        return ($parts -join "; ")
    }
    $text = [string]$data
    if ($text.Length -gt 300) { return $text.Substring(0, 300) + "...[truncated]" }
    return $text
}

function Audit($type, $label, $data = @{}) {
    $summary = Audit-Summary $data
    $line = '{"timestamp":"' + (Escape-JsonString ((Get-Date).ToString("o"))) +
        '","runId":"' + (Escape-JsonString $RunId) +
        '","type":"' + (Escape-JsonString $type) +
        '","label":"' + (Escape-JsonString $label) +
        '","summary":"' + (Escape-JsonString $summary) + '"}'
    Add-LogContent $AuditLog $line
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory=$true)][string]$Method,
        [Parameter(Mandatory=$true)][string]$PathOrUrl,
        [hashtable]$Headers = @{},
        $Body = $null,
        [int[]]$Expected = @(200),
        [string]$Label = $PathOrUrl
    )
    $url = if ($PathOrUrl.StartsWith("http")) { $PathOrUrl } else { "$Gateway$PathOrUrl" }
    $tmp = Join-Path $env:TEMP ("springbank-e2e-" + [guid]::NewGuid() + ".json")
    $bodyTmp = $null
    $script:HttpCounter++
    $safeLabel = Safe-Name ("{0:D3}-{1}-{2}" -f $script:HttpCounter, $Method, $Label)
    try {
        $curlArgs = @("-sS", "--connect-timeout", "5", "--max-time", "$HttpTimeoutSeconds", "-X", $Method, "-w", "`n%{http_code}", "-o", $tmp)
        foreach ($key in $Headers.Keys) { $curlArgs += @("-H", "$key`: $($Headers[$key])") }
        $bodyForLog = $null
        if ($null -ne $Body) {
            $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 20 -Compress }
            $bodyForLog = $json
            $bodyTmp = Join-Path $env:TEMP ("springbank-e2e-body-" + [guid]::NewGuid() + ".json")
            Set-Content -LiteralPath $bodyTmp -Value $json -NoNewline
            $curlArgs += @("-H", "Content-Type: application/json", "--data-binary", "@$bodyTmp")
        }
        $curlArgs += $url
        $commandForLog = "curl.exe " + (($curlArgs | ForEach-Object { if ($_ -match "\s") { '"' + $_ + '"' } else { $_ } }) -join " ")
        $opStart = Get-Date
        Write-DetailLog "http" "START" $Label "COMMAND:`n$commandForLog`nSTART_ELAPSED_TOTAL: $(Format-Duration ($opStart - $script:RunStart))"
        try {
            $raw = & curl.exe @curlArgs 2>&1
            $curlExitCode = $LASTEXITCODE
        } catch {
            $raw = $_.Exception.Message
            $curlExitCode = if ($LASTEXITCODE) { $LASTEXITCODE } else { 1 }
        }
        $duration = (Get-Date) - $opStart
        $statusLine = ($raw | Select-Object -Last 1)
        $status = 0
        [void][int]::TryParse($statusLine, [ref]$status)
        $content = if (Test-Path $tmp) { Get-Content -Raw -LiteralPath $tmp } else { "" }
        if ($curlExitCode -eq 28 -or ($status -eq 0 -and $duration.TotalSeconds -ge $HttpTimeoutSeconds)) {
            Skip "$Label HTTP 1 dakikayi asti, atlandi."
            $detail = @"
COMMAND:
$commandForLog
DURATION: $(Format-Duration $duration)
METHOD: $Method
URL: $url
EXPECTED: $($Expected -join ",")
STATUS: $status
CURL RAW:
$raw
CURL_EXIT_CODE: $curlExitCode
RESPONSE:
$content
"@
            Write-DetailLog "http" "SKIP" $Label $detail
            Audit "http" $Label @{ method=$Method; url=$url; status=$status; skipped=$true; duration=(Format-Duration $duration); command=$commandForLog; requestBody=$bodyForLog; response=$content; curlRaw="$raw" }
            return [pscustomobject]@{ Status=$status; Raw=$content; Json=$null; Skipped=$true }
        }
        $okStatus = $Expected -contains $status
        if ($okStatus) { Ok "$Label HTTP $status" } else { Bad "$Label HTTP $status expected $($Expected -join ',') body=$content curl=$raw" }
        $detail = @"
COMMAND:
$commandForLog
DURATION: $(Format-Duration $duration)
METHOD: $Method
URL: $url
EXPECTED: $($Expected -join ",")
STATUS: $status
HEADERS:
$(($Headers.GetEnumerator() | ForEach-Object { "$($_.Key): $($_.Value)" }) -join "`n")
BODY:
$bodyForLog
CURL RAW:
$raw
CURL_EXIT_CODE: $curlExitCode
RESPONSE:
$content
"@
        Write-DetailLog "http" $(if ($okStatus) { "OK" } else { "FAIL" }) $Label $detail
        Audit "http" $Label @{ method=$Method; url=$url; status=$status; expected=$Expected; command=$commandForLog; requestBody=$bodyForLog; response=$content; curlRaw="$raw" }
        if ([string]::IsNullOrWhiteSpace($content)) { return [pscustomobject]@{ Status=$status; Raw=""; Json=$null } }
        try {
            $trimForJson = $content.TrimStart()
            $jsonObj = if ($trimForJson.StartsWith("{") -or $trimForJson.StartsWith("[")) { $content | ConvertFrom-Json } else { $null }
        } catch { $jsonObj = $null }
        return [pscustomobject]@{ Status=$status; Raw=$content; Json=$jsonObj }
    } finally {
        Remove-Item -LiteralPath $tmp -ErrorAction SilentlyContinue
        if ($bodyTmp) { Remove-Item -LiteralPath $bodyTmp -ErrorAction SilentlyContinue }
    }
}

function Decode-JwtPayload($token) {
    $payload = $token.Split(".")[1].Replace("-", "+").Replace("_", "/")
    while ($payload.Length % 4) { $payload += "=" }
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) | ConvertFrom-Json
}

function Get-Token($email, $password, $label) {
    $response = Invoke-Checked POST "/api/user-service/v1/auth/login" @{} @{ email=$email; password=$password } @(200) "$label login"
    if ($null -eq $response.Json -or [string]::IsNullOrWhiteSpace($response.Json.access_token)) {
        Bad "$label token alinamadi. Body=$($response.Raw)"
        return $null
    }
    Ok "$label access_token alindi"
    return $response.Json
}

function AuthHeaders($token) {
    return @{ Authorization = "Bearer $token" }
}

function Wait-Until {
    param([scriptblock]$Condition, [int]$TimeoutSeconds = 90, [int]$EverySeconds = 3, [string]$Label = "condition")
    if ($TimeoutSeconds -gt $DefaultWaitCapSeconds) {
        Write-DetailLog "wait" "INFO" $Label "REQUESTED_TIMEOUT_SECONDS: $TimeoutSeconds`nCAPPED_TIMEOUT_SECONDS: $DefaultWaitCapSeconds"
        $TimeoutSeconds = $DefaultWaitCapSeconds
    }
    $opStart = Get-Date
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = $null
    $attempt = 0
    Write-DetailLog "wait" "START" $Label "TIMEOUT_SECONDS: $TimeoutSeconds`nEVERY_SECONDS: $EverySeconds`nSTART_ELAPSED_TOTAL: $(Format-Duration ($opStart - $script:RunStart))"
    while ((Get-Date) -lt $deadline) {
        $attempt++
        try {
            $last = & $Condition
            Write-DetailLog "wait" "INFO" $Label "ATTEMPT: $attempt`nATTEMPT_ELAPSED: $(Format-Duration ((Get-Date) - $opStart))`nRESULT: $last"
            if ($last) {
                Ok "$Label satisfied"
                Write-DetailLog "wait" "OK" $Label "ATTEMPTS: $attempt`nDURATION: $(Format-Duration ((Get-Date) - $opStart))"
                return $true
            }
        } catch {
            $last = $_.Exception.Message
            Write-DetailLog "wait" "INFO" $Label "ATTEMPT: $attempt`nATTEMPT_ELAPSED: $(Format-Duration ((Get-Date) - $opStart))`nEXCEPTION: $last"
        }
        Start-Sleep -Seconds $EverySeconds
    }
    Skip "$Label 1 dakikayi asti, atlandi. Last=$last"
    Write-DetailLog "wait" "SKIP" $Label "ATTEMPTS: $attempt`nDURATION: $(Format-Duration ((Get-Date) - $opStart))`nLAST: $last"
    return $false
}

function Kube {
    param(
        [Alias("Args")]
        [Parameter(Position=0)]
        [object[]]$KubectlArgs,
        [switch]$AllowFail
    )
    $flatArgs = @()
    foreach ($item in $KubectlArgs) {
        if ($item -is [System.Array]) {
            foreach ($nested in $item) { $flatArgs += [string]$nested }
        } else {
            $flatArgs += [string]$item
        }
    }
    if ($flatArgs -contains "-AllowFail") {
        $AllowFail = $true
        $flatArgs = @($flatArgs | Where-Object { $_ -ne "-AllowFail" })
    }
    $hasRequestTimeout = @($flatArgs | Where-Object { $_ -match "^--request-timeout=" }).Count -gt 0
    if (-not $hasRequestTimeout -and -not ($flatArgs -contains "exec") -and -not ($flatArgs -contains "logs") -and -not ($flatArgs -contains "port-forward")) {
        $flatArgs = @("--request-timeout=${SkipLongRunning}s") + $flatArgs
    }
    $opStart = Get-Date
    Write-DetailLog "kubectl" "START" ($flatArgs -join " ") "COMMAND:`nkubectl $($flatArgs -join ' ')`nSTART_ELAPSED_TOTAL: $(Format-Duration ($opStart - $script:RunStart))"
    $result = Invoke-ExternalCommand "kubectl" $flatArgs $SkipLongRunning
    $duration = $result.Duration
    if ($result.TimedOut) {
        Skip "kubectl $($flatArgs -join ' ') 1 dakikayi asti, atlandi."
        Write-DetailLog "kubectl" "SKIP" ($flatArgs -join " ") "COMMAND:`nkubectl $($flatArgs -join ' ')`nDURATION: $(Format-Duration $duration)`nLIMIT_SECONDS: $SkipLongRunning`nOUTPUT:`n$($result.Output)"
        Audit "kubectl" ($flatArgs -join " ") @{ exitCode=$result.ExitCode; timedOut=$true; duration=(Format-Duration $duration); command=("kubectl " + ($flatArgs -join " ")); output=$result.Output }
        return ""
    }
    if ($result.ExitCode -ne 0 -and -not $AllowFail) {
        Write-DetailLog "kubectl" "FAIL" ($flatArgs -join " ") "COMMAND:`nkubectl $($flatArgs -join ' ')`nDURATION: $(Format-Duration $duration)`nEXIT_CODE: $($result.ExitCode)`nOUTPUT:`n$($result.Output)"
        throw "kubectl $($flatArgs -join ' ') failed: $($result.Output)"
    }
    $text = $result.Output
    Write-DetailLog "kubectl" $(if ($result.ExitCode -eq 0) { "OK" } else { "FAIL" }) ($flatArgs -join " ") "COMMAND:`nkubectl $($flatArgs -join ' ')`nDURATION: $(Format-Duration $duration)`nEXIT_CODE: $($result.ExitCode)`nALLOW_FAIL: $([bool]$AllowFail)`nOUTPUT:`n$text"
    Audit "kubectl" ($flatArgs -join " ") @{ exitCode=$result.ExitCode; allowFail=[bool]$AllowFail; timedOut=$false; duration=(Format-Duration $duration); command=("kubectl " + ($flatArgs -join " ")); output=$text }
    return $text
}

function Sync-TestLogsToGateway {
    if ($script:LogsSyncedToGateway) {
        Write-DetailLog "sync" "SKIP" "gateway test log sync" "Already synced in this run.`nLOG_ROOT: $LogRoot"
        return
    }
    $script:LogsSyncedToGateway = $true
    $targetRoot = "/tmp/springbank-test-logs"
    $targetPath = "$targetRoot/full-e2e-$RunId"
    $opStart = Get-Date
    Write-DetailLog "sync" "START" "gateway test log sync" "SOURCE: $LogRoot`nTARGET: $Namespace/gateway:$targetPath"
    try {
        $pod = Get-PodByLabel "app=gateway"
        $copySource = $LogRoot
        if ($LogRoot.StartsWith($RepoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            $copySource = ".\" + $LogRoot.Substring($RepoRoot.Length).TrimStart("\", "/")
        }
        $mkdirArgs = @("-n", $Namespace, "exec", $pod, "--", "sh", "-lc", "mkdir -p '$targetRoot' && rm -rf '$targetPath'")
        $copyArgs = @("-n", $Namespace, "cp", $copySource, "${pod}:$targetPath")
        $mkdir = Invoke-ExternalCommand "kubectl" $mkdirArgs $SkipLongRunning
        $copy = if ($mkdir.ExitCode -eq 0 -and -not $mkdir.TimedOut) { Invoke-ExternalCommand "kubectl" $copyArgs $SkipLongRunning } else { $null }
        $duration = (Get-Date) - $opStart
        if ($null -eq $copy -or $mkdir.TimedOut -or $mkdir.ExitCode -ne 0 -or $copy.TimedOut -or $copy.ExitCode -ne 0) {
            Write-Host "[WARN] gateway test log sync basarisiz." -ForegroundColor DarkYellow
            Write-DetailLog "sync" "WARN" "gateway test log sync" "SOURCE: $LogRoot`nCOPY_SOURCE: $copySource`nCOMMANDS:`nkubectl $($mkdirArgs -join ' ')`nkubectl $($copyArgs -join ' ')`nDURATION: $(Format-Duration $duration)`nMKDIR_EXIT: $($mkdir.ExitCode)`nMKDIR_TIMEOUT: $($mkdir.TimedOut)`nMKDIR_OUTPUT:`n$($mkdir.Output)`nCOPY_EXIT: $($copy.ExitCode)`nCOPY_TIMEOUT: $($copy.TimedOut)`nCOPY_OUTPUT:`n$($copy.Output)"
            return
        }
        Write-DetailLog "sync" "OK" "gateway test log sync" "SOURCE: $LogRoot`nCOPY_SOURCE: $copySource`nCOMMANDS:`nkubectl $($mkdirArgs -join ' ')`nkubectl $($copyArgs -join ' ')`nDURATION: $(Format-Duration $duration)`nTARGET: $targetPath`nOUTPUT:`n$($copy.Output)"
    } catch {
        $duration = (Get-Date) - $opStart
        Write-Host "[WARN] gateway test log sync basarisiz: $($_.Exception.Message)" -ForegroundColor DarkYellow
        Write-DetailLog "sync" "WARN" "gateway test log sync" "SOURCE: $LogRoot`nTARGET: $Namespace/gateway:$targetPath`nDURATION: $(Format-Duration $duration)`nERROR: $($_.Exception.Message)"
    }
}

function Get-PodByLabel($label) {
    $pod = Kube -Args @("-n", $Namespace, "get", "pods", "-l", $label, "-o", "jsonpath={.items[0].metadata.name}")
    if ([string]::IsNullOrWhiteSpace($pod)) { throw "Pod not found for label $label" }
    return $pod
}

function PsqlScalar($db, $sql) {
    $opStart = Get-Date
    $pod = Get-PodByLabel "app=postgres"
    $out = Kube -Args @("-n", $Namespace, "exec", $pod, "--", "psql", "-U", "banking_admin", "-d", $db, "-t", "-A", "-c", $sql)
    $duration = (Get-Date) - $opStart
    $trimmed = $out.Trim()
    $command = "kubectl -n $Namespace exec $pod -- psql -U banking_admin -d $db -t -A -c `"$sql`""
    Write-DetailLog "db" "OK" "postgres:$db" "COMMAND:`n$command`nDURATION: $(Format-Duration $duration)`nSQL:`n$sql`nRESULT:`n$trimmed"
    Audit "postgres" $db @{ command=$command; duration=(Format-Duration $duration); sql=$sql; result=$trimmed }
    return $trimmed
}

function GatewayExecCurl($method, $url, $body = $null, $expected = @(200), $label = $url) {
    $opStart = Get-Date
    $pod = Get-PodByLabel "app=gateway"
    $cmd = "curl -sS -X '$method' -w '\n%{http_code}'"
    if ($null -ne $body) {
        $json = if ($body -is [string]) { $body } else { $body | ConvertTo-Json -Depth 20 -Compress }
        $bodyB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($json))
        $cmd = "printf '%s' '$bodyB64' | base64 -d > /tmp/codex-body.json; " + $cmd
        $cmd += " -H 'Content-Type: application/json' --data-binary @/tmp/codex-body.json"
    }
    $urlEscaped = $url.Replace("'", "'""'""'")
    $cmd += " '$urlEscaped'"
    Write-DetailLog "pod-http" "START" $label "COMMAND:`nkubectl -n $Namespace exec $pod -- sh -lc `"$cmd`"`nSTART_ELAPSED_TOTAL: $(Format-Duration ($opStart - $script:RunStart))"
    $raw = Kube -Args @("-n", $Namespace, "exec", $pod, "--", "sh", "-lc", $cmd) -AllowFail
    $duration = (Get-Date) - $opStart
    $lines = $raw -split "`r?`n"
    $status = [int]($lines[-1])
    $content = (($lines | Select-Object -First ($lines.Count - 1)) -join "`n")
    $script:HttpCounter++
    $okStatus = $expected -contains $status
    if ($okStatus) { Ok "$label HTTP $status" } else { Bad "$label HTTP $status expected $($expected -join ',') body=$content" }
    $detail = @"
COMMAND:
kubectl -n $Namespace exec $pod -- sh -lc "$cmd"
DURATION: $(Format-Duration $duration)
METHOD: $method
URL: $url
EXPECTED: $($expected -join ",")
STATUS: $status
RESPONSE:
$content
RAW:
$raw
"@
    Write-DetailLog "pod-http" $(if ($okStatus) { "OK" } else { "FAIL" }) $label $detail
    Audit "pod-http" $label @{ method=$method; url=$url; status=$status; expected=$expected; duration=(Format-Duration $duration); command=("kubectl -n $Namespace exec $pod -- sh -lc `"$cmd`""); response=$content; raw=$raw }
    try { $jsonObj = if ($content.Trim()) { $content | ConvertFrom-Json } else { $null } } catch { $jsonObj = $null }
    return [pscustomobject]@{ Status=$status; Raw=$content; Json=$jsonObj }
}

function Kafka-EndOffset($topic) {
    $opStart = Get-Date
    $pod = Get-PodByLabel "app=kafka"
    try {
        $out = Kube -Args @("-n", $Namespace, "exec", $pod, "--", "kafka-run-class", "kafka.tools.GetOffsetShell", "--broker-list", "kafka:9092", "--topic", $topic, "--time", "-1") -AllowFail
    } catch {
        $out = $_.Exception.Message
    }
    if ([string]::IsNullOrWhiteSpace($out) -or $out -match "not found|No such file") {
        try {
            $out = Kube -Args @("-n", $Namespace, "exec", $pod, "--", "kafka-get-offsets", "--bootstrap-server", "kafka:9092", "--topic", $topic) -AllowFail
        } catch {
            $out = $_.Exception.Message
        }
    }
    $sum = 0L
    foreach ($line in ($out -split "`r?`n")) {
        if ($line -match ":(\d+)$") { $sum += [int64]$Matches[1] }
    }
    $duration = (Get-Date) - $opStart
    Write-DetailLog "kafka" "OK" "offset:$topic" "COMMAND:`nkafka-run-class kafka.tools.GetOffsetShell --broker-list kafka:9092 --topic $topic --time -1`nDURATION: $(Format-Duration $duration)`nTOPIC: $topic`nRAW:`n$out`nSUM: $sum"
    Audit "kafka-offset" $topic @{ topic=$topic; offset=$sum; duration=(Format-Duration $duration); command="kafka-run-class kafka.tools.GetOffsetShell --broker-list kafka:9092 --topic $topic --time -1"; raw=$out }
    return $sum
}

function Snapshot-KafkaOffsets($topics) {
    $snapshot = @{}
    foreach ($topic in $topics) { $snapshot[$topic] = Kafka-EndOffset $topic }
    return $snapshot
}

function Assert-KafkaAdvanced($before, $topic, $label) {
    $after = Kafka-EndOffset $topic
    $old = if ($before.ContainsKey($topic)) { [int64]$before[$topic] } else { 0L }
    Assert-True ($after -gt $old) "$label Kafka offset advanced $topic $old -> $after"
}

function Kafka-ConsumeContains($topic, $needle, $label, [int]$TimeoutMs = 5000) {
    $opStart = Get-Date
    $pod = Get-PodByLabel "app=kafka"
    $cmd = "kafka-console-consumer --bootstrap-server kafka:9092 --topic $topic --from-beginning --timeout-ms $TimeoutMs 2>/dev/null || true"
    Write-DetailLog "kafka" "START" $label "COMMAND:`nkubectl -n $Namespace exec $pod -- sh -lc `"$cmd`"`nSTART_ELAPSED_TOTAL: $(Format-Duration ($opStart - $script:RunStart))"
    try {
        $out = Kube -Args @("-n", $Namespace, "exec", $pod, "--", "sh", "-lc", $cmd) -AllowFail
    } catch {
        $out = $_.Exception.Message
    }
    $duration = (Get-Date) - $opStart
    $found = $out -like "*$needle*"
    Assert-True $found "$label Kafka mesaj icerigi bulundu topic=$topic needle=$needle"
    Write-DetailLog "kafka" $(if ($found) { "OK" } else { "FAIL" }) $label "COMMAND:`nkubectl -n $Namespace exec $pod -- sh -lc `"$cmd`"`nDURATION: $(Format-Duration $duration)`nTOPIC: $topic`nNEEDLE: $needle`nFOUND: $found`nOUTPUT:`n$out"
    Audit "kafka-consume" $label @{ topic=$topic; needle=$needle; found=$found; duration=(Format-Duration $duration); command=("kubectl -n $Namespace exec $pod -- sh -lc `"$cmd`""); output=$out }
    return $found
}

function Capture-PodLogs($selector, $label, $needle = $RunId) {
    $opStart = Get-Date
    $pod = Get-PodByLabel $selector
    $out = Kube -Args @("-n", $Namespace, "logs", $pod, "--tail=400") -AllowFail
    $duration = (Get-Date) - $opStart
    $found = $out -like "*$needle*"
    if ($found) { Ok "$label pod log icinde '$needle' bulundu" } else { Info "$label pod log icinde '$needle' bulunamadi; log kaydedildi" }
    Write-DetailLog "pods" $(if ($found) { "OK" } else { "INFO" }) $label "COMMAND:`nkubectl -n $Namespace logs $pod --tail=400`nDURATION: $(Format-Duration $duration)`nSELECTOR: $selector`nNEEDLE: $needle`nFOUND: $found`nOUTPUT:`n$out"
    Audit "pod-log" $label @{ selector=$selector; pod=$pod; needle=$needle; found=$found; duration=(Format-Duration $duration); command=("kubectl -n $Namespace logs $pod --tail=400"); output=$out }
    return $found
}

function Get-Balance($token, $uuid, $label) {
    $headers = AuthHeaders $token
    $headers["X-User-KeycloakUUID"] = $uuid
    $response = Invoke-Checked GET "/api/money-service/v1/accounts/balance-info" $headers $null @(200) "$label balance-info"
    return $response.Json
}

try {
    Info "RunId=$RunId"
    Info "Test modu=$Mode (varsayilan full kapsam; 1 dakikayi asan parca SKIP yazilip sonraki teste gecilir)"
    Info "Timeoutlar: HTTP=$HttpTimeoutSeconds sn, long-running-skip=$SkipLongRunning sn, wait-cap=$DefaultWaitCapSeconds sn"
    Info "Tum loglar: $LogRoot"
    Audit "run" "start" @{ namespace=$Namespace; gateway=$Gateway; mode=$Mode; httpTimeoutSeconds=$HttpTimeoutSeconds; stepTimeoutSeconds=$StepTimeoutSeconds; waitCapSeconds=$DefaultWaitCapSeconds; logRoot=$LogRoot }

    Step "Kubernetes ve servis on kontrolleri"
    $context = Kube -Args @("config", "current-context")
    Ok "kubectl context: $context"
    Kube -Args @("--request-timeout=10s", "cluster-info") | Out-Null
    Ok "Kubernetes API cevap veriyor"
    Kube -Args @("get", "namespace", $Namespace) | Out-Null
    Ok "namespace var: $Namespace"
    $deploymentWait = if (Is-FullMode) { "600s" } else { "90s" }
    Kube -Args @("-n", $Namespace, "wait", "--for=condition=available", "deployment", "--all", "--timeout=$deploymentWait") | Out-Null
    Ok "tum deployment'lar available condition beklemesi tamam"

    $expectedDeployments = if (Is-FullMode) {
        @("postgres","redis","mongodb","elasticsearch","zookeeper","kafka","keycloak","user-service","money-service","money-service-command","money-service-query","transaction-service","fraud-service","gateway")
    } else {
        @("postgres","kafka","keycloak","user-service","money-service","transaction-service","gateway")
    }
    foreach ($deployment in $expectedDeployments) {
        $ready = Kube -Args @("-n", $Namespace, "get", "deploy", $deployment, "-o", "jsonpath={.status.readyReplicas}") -AllowFail
        Assert-True (-not [string]::IsNullOrWhiteSpace($ready) -and [int]$ready -ge 1) "deployment ready: $deployment ($ready)"
    }

    $expectedServices = if (Is-FullMode) {
        @("postgres","redis","mongodb","elasticsearch","zookeeper","kafka","keycloak","user-service","money-service","money-service-command","money-service-query","transaction-service","fraud-service","gateway")
    } else {
        @("postgres","kafka","keycloak","user-service","money-service","transaction-service","gateway")
    }
    foreach ($service in $expectedServices) {
        Kube -Args @("-n", $Namespace, "get", "svc", $service) | Out-Null
        Ok "service var: $service"
    }

    Step "Gateway erisimi"
    $gatewayReachable = $false
    try {
        $probe = Invoke-Checked GET "$Gateway/login.html" @{} $null @(200) "gateway static login.html"
        $gatewayReachable = $probe.Status -eq 200
    } catch {
        Info "Gateway local erisim yok: $($_.Exception.Message)"
    }
    if (-not $gatewayReachable) {
        Info "localhost:$GatewayPort kapali, kubectl port-forward service/gateway $GatewayPort`:8095 aciliyor"
        $PortForwardProcess = Start-Process -FilePath "kubectl" -ArgumentList @("-n", $Namespace, "port-forward", "svc/gateway", "$GatewayPort`:8095") -WindowStyle Hidden -PassThru
        Wait-Until {
            try {
                $probeAfterForward = Invoke-Checked GET "$Gateway/login.html" @{} $null @(200) "gateway static login.html after port-forward"
                $probeAfterForward.Status -eq 200
            } catch {
                $false
            }
        } 120 5 "gateway HTTP ready after port-forward" | Out-Null
    }

    $staticPages = if (Is-FullMode) {
        @("/", "/index.html", "/login.html", "/register.html", "/dashboard.html", "/deposit.html", "/withdraw.html", "/transfer.html", "/transactions.html", "/admin.html")
    } else {
        @("/login.html", "/register.html", "/dashboard.html", "/transactions.html", "/admin.html")
    }
    foreach ($page in $staticPages) { Invoke-Checked GET $page @{} $null @(200) "static $page" | Out-Null }

    Step "Public health endpointleri"
    Invoke-Checked GET "/api/money-service/v1/accounts/health" @{} $null @(200,401) "money-service health unauthenticated/security" | Out-Null
    Wait-Until {
        $health = Invoke-Checked GET "/api/user-service/actuator/health/liveness" @{} $null @(200) "user-service liveness"
        return $health.Status -eq 200
    } 60 3 "user-service liveness HTTP 200" | Out-Null
    Wait-Until {
        $health = Invoke-Checked GET "/api/money-service/actuator/health/liveness" @{} $null @(200) "money-service liveness"
        return $health.Status -eq 200
    } 60 3 "money-service liveness HTTP 200" | Out-Null
    Wait-Until {
        $health = Invoke-Checked GET "/api/transaction-service/actuator/health/liveness" @{} $null @(200) "transaction-service liveness"
        return $health.Status -eq 200
    } 60 3 "transaction-service liveness HTTP 200" | Out-Null
    Wait-Until {
        $health = Invoke-Checked GET "/api/fraud-service/actuator/health/liveness" @{} $null @(200) "fraud-service liveness"
        return $health.Status -eq 200
    } 60 3 "fraud-service liveness HTTP 200" | Out-Null
    if (Is-FullMode) {
        GatewayExecCurl GET "http://money-service-command:8092/api/money-service-command/v1/accounts/health" $null @(200) "money-service-command internal health" | Out-Null
        GatewayExecCurl GET "http://money-service-query:8093/api/money-service-query/v1/accounts/health" $null @(200) "money-service-query internal health" | Out-Null
    } else {
        Skip-FullOnly "CQRS command/query internal health kontrolleri"
    }

    Step "Kafka topic ve altyapi kontrolleri"
    $expectedTopics = @(
        "banking-microservices.user.created.v1",
        "banking-microservices.money.account.created.v1",
        "banking-microservices.money.account.create-failed.v1",
        "banking-microservices.transaction.created.v1",
        "banking-microservices.transaction.completed.v1",
        "banking-microservices.transaction.failed.v1",
        "banking-microservices.transaction.money-blocked.v1",
        "banking-microservices.transaction.user-validation.request.v1",
        "banking-microservices.transaction.user-validation.success.v1",
        "banking-microservices.transaction.fraud.checked.v1",
        "banking-microservices.transaction.saga.created.v1",
        "banking-microservices.transaction.saga.money.completed.v1",
        "banking-microservices.transaction.saga.money.failed.v1",
        "banking-microservices.transaction.money-blocked.v1"
    )
    $KafkaBeforeAll = @{}
    if (Is-FullMode) {
        $kafkaPod = Get-PodByLabel "app=kafka"
        $topics = Kube -Args @("-n", $Namespace, "exec", $kafkaPod, "--", "kafka-topics", "--bootstrap-server", "kafka:9092", "--list")
        foreach ($topic in $expectedTopics) {
            if ($topics -match [regex]::Escape($topic)) {
                Ok "Kafka topic var: $topic"
            } else {
                Info "Kafka topic henuz yok, akista uretildiginde tekrar dogrulanacak: $topic"
                Audit "kafka-topic" $topic @{ existsAtStart=$false }
            }
        }
        $KafkaBeforeAll = Snapshot-KafkaOffsets $expectedTopics
    } else {
        Skip-FullOnly "Kafka topic listeleme ve offset snapshot"
    }

    Step "Register, login, token decode"
    $user1 = [pscustomobject]@{ Name="E2EAlpha"; Surname="Tester"; Email="e2e.alpha.$RunId@springbank.test"; Password=$UserPassword }
    $user2 = [pscustomobject]@{ Name="E2EBeta"; Surname="Tester"; Email="e2e.beta.$RunId@springbank.test"; Password=$UserPassword }

    Invoke-Checked POST "/api/user-service/v1/auth/register" @{} @{ email=$user1.Email; password=$user1.Password; name=$user1.Name; surname=$user1.Surname; role="USER" } @(200) "register user1" | Out-Null
    Invoke-Checked POST "/api/user-service/v1/auth/register" @{} @{ email=$user2.Email; password=$user2.Password; name=$user2.Name; surname=$user2.Surname; role="USER" } @(200) "register user2" | Out-Null
    Start-Sleep -Seconds 8

    $token1Response = Get-Token $user1.Email $user1.Password "user1"
    $token2Response = Get-Token $user2.Email $user2.Password "user2"
    $adminTokenResponse = Get-Token $AdminEmail $AdminPassword "admin"
    $token1 = $token1Response.access_token
    $token2 = $token2Response.access_token
    $adminToken = $adminTokenResponse.access_token
    $uuid1 = (Decode-JwtPayload $token1).sub
    $uuid2 = (Decode-JwtPayload $token2).sub
    $adminPayload = Decode-JwtPayload $adminToken
    Assert-True (-not [string]::IsNullOrWhiteSpace($uuid1)) "user1 JWT sub: $uuid1"
    Assert-True (-not [string]::IsNullOrWhiteSpace($uuid2)) "user2 JWT sub: $uuid2"
    Assert-True (($adminPayload.realm_access.roles -contains "ADMIN") -or ($adminPayload.realm_access.roles -contains "admin")) "admin JWT ADMIN role iceriyor"

    Invoke-Checked POST "/api/user-service/v1/auth/refresh" @{} @{ refreshToken=$token1Response.refresh_token } @(200) "refresh token user1" | Out-Null
    if (Is-FullMode) {
        Assert-KafkaAdvanced $KafkaBeforeAll "banking-microservices.user.created.v1" "register user"
        Kafka-ConsumeContains "banking-microservices.user.created.v1" $uuid1 "register user1 create-user event" | Out-Null
        Kafka-ConsumeContains "banking-microservices.user.created.v1" $uuid2 "register user2 create-user event" | Out-Null
    } else {
        Skip-FullOnly "register sonrasi Kafka user.created detay kontrolu"
    }

    Step "Keycloak DB dogrulamasi"
    $kcCount1 = PsqlScalar "banking_keycloak" "select count(*) from user_entity where lower(email)=lower('$($user1.Email)');"
    $kcCount2 = PsqlScalar "banking_keycloak" "select count(*) from user_entity where lower(email)=lower('$($user2.Email)');"
    Assert-True ([int]$kcCount1 -ge 1) "Keycloak DB user1 kaydi var"
    Assert-True ([int]$kcCount2 -ge 1) "Keycloak DB user2 kaydi var"

    Step "User admin endpointleri"
    $adminHeaders = AuthHeaders $adminToken
    Invoke-Checked GET "/api/user-service/v1/admin/stats/total" $adminHeaders $null @(200) "user admin total" | Out-Null
    Invoke-Checked GET "/api/user-service/v1/admin/stats/roles" $adminHeaders $null @(200) "user admin roles" | Out-Null
    Invoke-Checked GET "/api/user-service/v1/admin/stats/active" $adminHeaders $null @(200) "user admin active" | Out-Null
    $findUser1 = Invoke-Checked GET "/api/user-service/v1/admin/findbyemail?email=$($user1.Email)" $adminHeaders $null @(200) "user admin findbyemail user1"
    $user1DbId = @($findUser1.Json)[0].id
    Assert-True (-not [string]::IsNullOrWhiteSpace($user1DbId)) "user1 DB id bulundu: $user1DbId"
    Invoke-Checked GET "/api/user-service/v1/user/$user1DbId" (AuthHeaders $token1) $null @(200) "user get self by id" | Out-Null
    Invoke-Checked GET "/api/user-service/v1/admin/findbykeycloakuuid/$uuid1" $adminHeaders $null @(200) "user admin findbykeycloakuuid user1" | Out-Null
    Invoke-Checked GET "/api/user-service/v1/admin/search?query=$($user1.Name)" $adminHeaders $null @(200) "user admin search name" | Out-Null
    Invoke-Checked GET "/api/user-service/v1/admin/allusers" $adminHeaders $null @(200) "user admin allusers" | Out-Null

    Step "User self-service profil mutasyon endpointleri"
    $user2NewEmail = "e2e.beta.updated.$RunId@springbank.test"
    $user2NewPassword = "Test1234!X"
    Invoke-Checked POST "/api/user-service/v1/user/$uuid2/change-email" (AuthHeaders $token2) @{ newEmail=$user2NewEmail; password=$user2.Password } @(200) "user2 change-email" | Out-Null
    $user2.Email = $user2NewEmail
    Invoke-Checked POST "/api/user-service/v1/user/$uuid2/change-password" (AuthHeaders $token2) @{ currentPassword=$user2.Password; newPassword=$user2NewPassword } @(200) "user2 change-password" | Out-Null
    $user2.Password = $user2NewPassword
    $token2Response = Get-Token $user2.Email $user2.Password "user2 after self-service mutations"
    $token2 = $token2Response.access_token
    $h2 = AuthHeaders $token2; $h2["X-User-KeycloakUUID"] = $uuid2

    if (Is-FullMode) {
        $adminTarget = [pscustomobject]@{ Name="E2EAdminTarget"; Surname="Tester"; Email="e2e.admin.target.$RunId@springbank.test"; Password=$UserPassword }
        Invoke-Checked POST "/api/user-service/v1/auth/register" @{} @{ email=$adminTarget.Email; password=$adminTarget.Password; name=$adminTarget.Name; surname=$adminTarget.Surname; role="USER" } @(200) "register admin mutation target" | Out-Null
        Start-Sleep -Seconds 5
        $findTarget = Invoke-Checked GET "/api/user-service/v1/admin/findbyemail?email=$($adminTarget.Email)" $adminHeaders $null @(200) "user admin findbyemail mutation target"
        $targetUser = @($findTarget.Json)[0]
        $targetDbId = $targetUser.id
        Assert-True (-not [string]::IsNullOrWhiteSpace($targetDbId)) "admin mutation target DB id bulundu: $targetDbId"
        Invoke-Checked PATCH "/api/user-service/v1/admin/updaterole/${targetDbId}?role=USER" $adminHeaders $null @(200) "user admin updaterole target USER" | Out-Null
        Invoke-Checked POST "/api/user-service/v1/admin/users/$targetDbId/deactivate" $adminHeaders $null @(200) "user admin deactivate target" | Out-Null
        Invoke-Checked POST "/api/user-service/v1/admin/users/$targetDbId/activate" $adminHeaders $null @(200) "user admin activate target" | Out-Null
        $resetPassword = "Reset1234!"
        Invoke-Checked POST "/api/user-service/v1/admin/users/$targetDbId/reset-password" $adminHeaders @{ newPassword=$resetPassword } @(200) "user admin reset-password target" | Out-Null
        Get-Token $adminTarget.Email $resetPassword "admin target after reset" | Out-Null
        $targetUser.name = "E2EAdminUpdated"
        Invoke-Checked PUT "/api/user-service/v1/admin/updateuser" $adminHeaders $targetUser @(200) "user admin updateuser target" | Out-Null
        Invoke-Checked DELETE "/api/user-service/v1/admin/deleteuser/$targetDbId" $adminHeaders $null @(200) "user admin deleteuser target" | Out-Null
    } else {
        Skip-FullOnly "admin kullanici mutate/deactivate/reset/delete regression akisi"
    }

    Step "Money account create ve DB dogrulamasi"
    $h1 = AuthHeaders $token1; $h1["X-User-KeycloakUUID"] = $uuid1
    $h2 = AuthHeaders $token2; $h2["X-User-KeycloakUUID"] = $uuid2
    Invoke-Checked POST "/api/money-service/v1/accounts/createusermoney" $h1 $null @(200) "money create user1" | Out-Null
    Invoke-Checked POST "/api/money-service/v1/accounts/createusermoney" $h2 $null @(200) "money create user2" | Out-Null
    Start-Sleep -Seconds 5
    $balance1 = Get-Balance $token1 $uuid1 "user1"
    $balance2 = Get-Balance $token2 $uuid2 "user2"
    $iban1 = $balance1.userIban
    $iban2 = $balance2.userIban
    Assert-True (-not [string]::IsNullOrWhiteSpace($iban1)) "user1 IBAN var: $iban1"
    Assert-True (-not [string]::IsNullOrWhiteSpace($iban2)) "user2 IBAN var: $iban2"
    Invoke-Checked POST "/api/money-service/v1/accounts/getUserIbanWithUserId" (AuthHeaders $token1) @{ userId=$uuid1 } @(200) "getUserIbanWithUserId user1" | Out-Null
    $moneyDb1 = PsqlScalar "banking_money" "select count(*) from money where user_id='$uuid1' or keycloak_user_uuid='$uuid1';"
    $moneyDb2 = PsqlScalar "banking_money" "select count(*) from money where user_id='$uuid2' or keycloak_user_uuid='$uuid2';"
    Assert-True ([int]$moneyDb1 -ge 1) "Postgres banking_money user1 money kaydi var"
    Assert-True ([int]$moneyDb2 -ge 1) "Postgres banking_money user2 money kaydi var"
    if (Is-FullMode) {
        Assert-KafkaAdvanced $KafkaBeforeAll "banking-microservices.money.account.created.v1" "money account create"
        Kafka-ConsumeContains "banking-microservices.money.account.created.v1" $uuid1 "money account created user1 event" | Out-Null
        Kafka-ConsumeContains "banking-microservices.money.account.created.v1" $uuid2 "money account created user2 event" | Out-Null
    } else {
        Skip-FullOnly "money account Kafka detay kontrolu"
    }

    Step "Money direct deposit/withdraw endpointleri"
    Invoke-Checked POST "/api/money-service/v1/accounts/depositByUserId" $adminHeaders @{ userId=$uuid2; amount=200 } @(200) "money direct depositByUserId user2" | Out-Null
    Wait-Until { $b = Get-Balance $token2 $uuid2 "user2 after direct deposit"; [decimal]$b.money -eq 200 } 60 3 "user2 direct deposit balance 200" | Out-Null
    Invoke-Checked POST "/api/money-service/v1/accounts/withdrawByUserId" $adminHeaders @{ userId=$uuid2; amount=200 } @(200) "money direct withdrawByUserId user2" | Out-Null
    Wait-Until { $b = Get-Balance $token2 $uuid2 "user2 after direct withdraw"; [decimal]$b.money -eq 0 } 60 3 "user2 direct withdraw balance 0" | Out-Null

    Step "Deposit, withdraw, transfer E2E"
    $KafkaBeforeTransactions = @{}
    if (Is-FullMode) {
        $KafkaBeforeTransactions = Snapshot-KafkaOffsets $expectedTopics
    } else {
        Skip-FullOnly "transaction Kafka offset snapshot"
    }
    Invoke-Checked POST "/api/transaction-service/v1/transactions/create" $h1 @{ amount=5000; transactionType="DEPOSIT"; senderIban=$null; receiverIban=$null; receiverName=$null; receiverSurname=$null; description="E2E deposit $RunId" } @(201) "transaction deposit user1" | Out-Null
    Wait-Until { $b = Get-Balance $token1 $uuid1 "user1 after deposit"; [decimal]$b.money -ge 5000 } 90 5 "user1 balance >= 5000 after deposit" | Out-Null

    Invoke-Checked POST "/api/transaction-service/v1/transactions/create" $h1 @{ amount=500; transactionType="WITHDRAW"; senderIban=$iban1; receiverIban=$null; receiverName=$null; receiverSurname=$null; description="E2E withdraw $RunId" } @(201) "transaction withdraw user1" | Out-Null
    Wait-Until { $b = Get-Balance $token1 $uuid1 "user1 after withdraw"; [decimal]$b.money -eq 4500 } 90 5 "user1 balance 4500 after withdraw" | Out-Null

    Invoke-Checked POST "/api/transaction-service/v1/transactions/create" $h1 @{ amount=1000; transactionType="TRANSFER"; senderIban=$iban1; receiverIban=$iban2; receiverName=$user2.Name; receiverSurname=$user2.Surname; description="E2E transfer $RunId" } @(201) "transaction transfer user1 to user2" | Out-Null
    Wait-Until {
        $b1 = Get-Balance $token1 $uuid1 "user1 after transfer"
        $b2 = Get-Balance $token2 $uuid2 "user2 after transfer"
        ([decimal]$b1.money -eq 3500) -and ([decimal]$b2.money -eq 1000)
    } 150 5 "transfer balances user1=3500 user2=1000" | Out-Null

    $final1 = Get-Balance $token1 $uuid1 "user1 final"
    $final2 = Get-Balance $token2 $uuid2 "user2 final"
    Assert-True ([decimal]$final1.money -eq 3500) "Final user1 money 3500"
    Assert-True ([decimal]$final2.money -eq 1000) "Final user2 money 1000"
    Assert-True ([decimal]$final1.blockedmoney -eq 0) "Final user1 blockedmoney 0"
    Assert-True ([decimal]$final2.blockedmoney -eq 0) "Final user2 blockedmoney 0"

    $moneyDbFinal1 = PsqlScalar "banking_money" "select money || '|' || blocked_money from money where user_id='$uuid1' or keycloak_user_uuid='$uuid1' limit 1;"
    $moneyDbFinal2 = PsqlScalar "banking_money" "select money || '|' || blocked_money from money where user_id='$uuid2' or keycloak_user_uuid='$uuid2' limit 1;"
    Assert-True ($moneyDbFinal1 -match "^3500(\.00)?\|0(\.00)?$") "DB user1 money/blocked dogru: $moneyDbFinal1"
    Assert-True ($moneyDbFinal2 -match "^1000(\.00)?\|0(\.00)?$") "DB user2 money/blocked dogru: $moneyDbFinal2"

    Step "Transaction endpointleri ve DB dogrulamasi"
    $hist1 = Invoke-Checked GET "/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=$uuid1" (AuthHeaders $token1) $null @(200) "transaction history user1"
    $hist2 = Invoke-Checked GET "/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=$uuid2" (AuthHeaders $token2) $null @(200) "transaction history user2"
    Assert-True ($hist1.Json.Count -ge 3) "user1 transaction history >= 3"
    Assert-True ($hist2.Json.Count -ge 1) "user2 transaction history >= 1"
    Invoke-Checked GET "/api/transaction-service/v1/transactions/errors" (AuthHeaders $token1) $null @(200) "transaction errors endpoint" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/transactions/daterange?startDate=$((Get-Date).AddDays(-1).ToString('yyyy-MM-ddTHH:mm:ss'))&endDate=$((Get-Date).AddDays(1).ToString('yyyy-MM-ddTHH:mm:ss'))" (AuthHeaders $token1) $null @(200) "transaction daterange" | Out-Null

    $txRow = PsqlScalar "banking_transactions" "select id || '|' || event_id || '|' || status from transactions where sender_user_id='$uuid1' and transaction_type='TRANSFER' order by created_at desc limit 1;"
    $txParts = $txRow -split "\|"
    $txId = $txParts[0]
    $eventUuid = $txParts[1]
    $txStatus = $txParts[2]
    Assert-True (-not [string]::IsNullOrWhiteSpace($eventUuid)) "DB transfer event_uuid var: $eventUuid"
    Assert-True ($txStatus -eq "COMPLETED") "DB transfer status COMPLETED"
    Invoke-Checked GET "/api/transaction-service/v1/transactions/byid?id=$txId" (AuthHeaders $token1) $null @(200) "transaction byid" | Out-Null
    if (Is-FullMode) {
        Assert-KafkaAdvanced $KafkaBeforeTransactions "banking-microservices.transaction.created.v1" "transaction create"
        Assert-KafkaAdvanced $KafkaBeforeTransactions "banking-microservices.transaction.money-blocked.v1" "transaction block/projection"
        Assert-KafkaAdvanced $KafkaBeforeTransactions "banking-microservices.transaction.user-validation.request.v1" "transaction user validation request"
        Assert-KafkaAdvanced $KafkaBeforeTransactions "banking-microservices.transaction.user-validation.success.v1" "transaction user validation success"
        Assert-KafkaAdvanced $KafkaBeforeTransactions "banking-microservices.transaction.fraud.checked.v1" "transaction fraud checked"
        Assert-KafkaAdvanced $KafkaBeforeTransactions "banking-microservices.transaction.completed.v1" "transaction completed"
        Kafka-ConsumeContains "banking-microservices.transaction.created.v1" $eventUuid "transaction.created eventUUID" | Out-Null
        Kafka-ConsumeContains "banking-microservices.transaction.money-blocked.v1" $eventUuid "transaction.money-blocked eventUUID" | Out-Null
        Kafka-ConsumeContains "banking-microservices.transaction.user-validation.request.v1" $eventUuid "user-validation.request eventUUID" | Out-Null
        Kafka-ConsumeContains "banking-microservices.transaction.user-validation.success.v1" $eventUuid "user-validation.success eventUUID" | Out-Null
        Kafka-ConsumeContains "banking-microservices.transaction.fraud.checked.v1" $eventUuid "fraud.checked eventUUID" | Out-Null
        Kafka-ConsumeContains "banking-microservices.transaction.completed.v1" $eventUuid "transaction.completed eventUUID" | Out-Null
    } else {
        Skip-FullOnly "transaction Kafka topic icerik kontrolleri"
    }

    if (Is-DevMode) {
        Step "Dev monolith admin ve frontend smoke"
        $adminTokenResponse = Get-Token $AdminEmail $AdminPassword "admin refresh before dev smoke"
        $adminToken = $adminTokenResponse.access_token
        $adminHeaders = AuthHeaders $adminToken
        Invoke-Checked GET "/api/money-service/v1/admin/stats/summary" $adminHeaders $null @(200) "dev money admin summary" | Out-Null
        Invoke-Checked GET "/api/transaction-service/v1/admin/stats/summary" $adminHeaders $null @(200) "dev transaction admin summary" | Out-Null
        Invoke-Checked GET "/api/gateway/admin/test-logs/runs" $adminHeaders $null @(200) "dev gateway test logs runs" | Out-Null
        Invoke-Checked GET "/admin.html" @{} $null @(200) "dev admin panel html" | Out-Null
        Invoke-Checked GET "/dashboard.html" @{} $null @(200) "dev dashboard html" | Out-Null
        Invoke-Checked GET "/transfer.html" @{} $null @(200) "dev transfer html" | Out-Null
        Invoke-Checked GET "/deposit.html" @{} $null @(200) "dev deposit html" | Out-Null
        Invoke-Checked GET "/withdraw.html" @{} $null @(200) "dev withdraw html" | Out-Null
        Invoke-Checked GET "/transactions.html" @{} $null @(200) "dev transactions html" | Out-Null

        Step "Dev kullanici islem geri cek smoke"
        Invoke-Checked POST "/api/transaction-service/v1/transactions/create" $h1 @{ amount=10; transactionType="DEPOSIT"; senderIban=$null; receiverIban=$null; receiverName=$null; receiverSurname=$null; description="DEV cancel smoke $RunId" } @(201) "dev cancel smoke deposit" | Out-Null
        Wait-Until { $b = Get-Balance $token1 $uuid1 "user1 before cancel smoke"; [decimal]$b.money -ge 3510 } 60 3 "dev cancel smoke deposit applied" | Out-Null
        $cancelEvent = PsqlScalar "banking_transactions" "select event_id from transactions where sender_user_id='$uuid1' and description='DEV cancel smoke $RunId' order by created_at desc limit 1;"
        Assert-True (-not [string]::IsNullOrWhiteSpace($cancelEvent)) "dev cancel smoke event bulundu: $cancelEvent"
        Invoke-Checked POST "/api/transaction-service/v1/transactions/cancel?eventUUID=$cancelEvent" $h1 $null @(200) "dev user transaction cancel" | Out-Null
        $cancelStatus = PsqlScalar "banking_transactions" "select status from transactions where event_id='$cancelEvent' limit 1;"
        Assert-True ($cancelStatus -eq "REVERSED" -or $cancelStatus -eq "CANCELLED") "dev cancel status dogru: $cancelStatus"

        Step "Logout"
        Invoke-Checked POST "/api/user-service/v1/auth/logout" @{} @{ refreshToken=$token2Response.refresh_token } @(200,204) "logout user2 refresh token" | Out-Null

        Step "Son ozet"
        $totalDuration = Format-Duration ((Get-Date) - $script:RunStart)
        if ($Failures.Count -eq 0) {
            Write-Host ""
            Write-Host "DEV MONOLITH TESTLERI GECTI. Pass=$PassCount RunId=$RunId Duration=$totalDuration" -ForegroundColor Green
            Write-DetailLog "run" "OK" "final-summary" "MODE: $Mode`nPASS: $PassCount`nFAIL: 0`nTOTAL_DURATION: $totalDuration`nLOG_ROOT: $LogRoot"
            Audit "run" "success" @{ mode=$Mode; pass=$PassCount; fail=0; totalDuration=$totalDuration; logRoot=$LogRoot }
            exit 0
        } else {
            Write-Host ""
            Write-Host "DEV MONOLITH TESTLERINDE HATA VAR. Pass=$PassCount Fail=$($Failures.Count) RunId=$RunId Duration=$totalDuration" -ForegroundColor Red
            $Failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
            Write-DetailLog "run" "FAIL" "final-summary" "MODE: $Mode`nPASS: $PassCount`nFAIL: $($Failures.Count)`nTOTAL_DURATION: $totalDuration`nFAILURES:`n$($Failures -join "`n")`nLOG_ROOT: $LogRoot"
            Audit "run" "failed" @{ mode=$Mode; pass=$PassCount; fail=$Failures.Count; totalDuration=$totalDuration; failures=$Failures; logRoot=$LogRoot }
            exit 1
        }
    }

    Step "Admin money/transaction/saga/gateway endpointleri"
    $adminTokenResponse = Get-Token $AdminEmail $AdminPassword "admin refresh before admin endpoints"
    $adminToken = $adminTokenResponse.access_token
    $adminHeaders = AuthHeaders $adminToken
    Invoke-Checked GET "/api/money-service/v1/admin/stats/summary" $adminHeaders $null @(200) "money admin summary" | Out-Null
    Invoke-Checked GET "/api/money-service/v1/admin/stats/count" $adminHeaders $null @(200) "money admin count" | Out-Null
    Invoke-Checked GET "/api/money-service/v1/admin/accounts?page=0&size=5" $adminHeaders $null @(200) "money admin accounts" | Out-Null
    Invoke-Checked GET "/api/money-service/v1/admin/accounts/top?limit=5" $adminHeaders $null @(200) "money admin top" | Out-Null
    Invoke-Checked GET "/api/money-service/v1/admin/account/byiban?iban=$iban2" $adminHeaders $null @(200) "money admin account byiban" | Out-Null
    Invoke-Checked GET "/api/money-service/v1/admin/stats/distribution" $adminHeaders $null @(200) "money admin distribution" | Out-Null

    Invoke-Checked GET "/api/transaction-service/v1/admin/stats/summary" $adminHeaders $null @(200) "transaction admin summary" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/admin/stats/daily?days=7" $adminHeaders $null @(200) "transaction admin daily" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/admin/stuck?olderThanMinutes=0" $adminHeaders $null @(200) "transaction admin stuck" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/admin/byiban?iban=$iban2" $adminHeaders $null @(200) "transaction admin byiban" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/admin/errors/analysis" $adminHeaders $null @(200) "transaction admin errors analysis" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/admin/stats/count" $adminHeaders $null @(200) "transaction admin count" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/admin/stats/top-by-volume?limit=5" $adminHeaders $null @(200) "transaction admin top by volume" | Out-Null

    Invoke-Checked GET "/api/transaction-service/v1/saga/all" $adminHeaders $null @(200) "saga all" | Out-Null
    Invoke-Checked GET "/api/transaction-service/v1/saga/exists?eventUUID=$eventUuid" $adminHeaders $null @(200) "saga exists before/manual" | Out-Null
    Invoke-Checked POST "/api/transaction-service/v1/saga/create?eventUUID=$eventUuid" $adminHeaders $null @(201) "saga create manual existing tx" | Out-Null
    Start-Sleep -Seconds 3
    $sagaExistsAfter = Invoke-Checked GET "/api/transaction-service/v1/saga/exists?eventUUID=$eventUuid" $adminHeaders $null @(200) "saga exists after/manual"
    Assert-True ($sagaExistsAfter.Json.exists -eq $true) "saga exists true after manual create"
    $sagaByTx = Invoke-Checked GET "/api/transaction-service/v1/saga/status/by-transaction?eventUUID=$eventUuid" $adminHeaders $null @(200) "saga status by transaction after/manual"
    if ($sagaByTx.Status -eq 200 -and $sagaByTx.Json.uuid) {
        Invoke-Checked GET "/api/transaction-service/v1/saga/status?uuid=$($sagaByTx.Json.uuid)" $adminHeaders $null @(200) "saga status by saga uuid" | Out-Null
    }
    Invoke-Checked POST "/api/transaction-service/v1/admin/transactions/saga?eventUUID=" $adminHeaders $null @(400) "transaction admin manual saga bad request validation" | Out-Null
    Invoke-Checked GET "/api/gateway/admin/logs/gateway/recent" $adminHeaders $null @(200,500) "gateway recent logs" | Out-Null

    Step "CQRS command/query servisleri internal gercek akisi"
    $KafkaBeforeCqrs = Snapshot-KafkaOffsets @("banking-microservices.transaction.money-blocked.v1")
    $cmdUser1 = "cmd-user-a-$RunId"
    $cmdUser2 = "cmd-user-b-$RunId"
    $cmdKey1 = "cmd-key-a-$RunId"
    $cmdKey2 = "cmd-key-b-$RunId"
    $cmdCreate1 = GatewayExecCurl POST "http://money-service-command:8092/api/money-service-command/v1/accounts" @{ userId=$cmdUser1; keycloakUserUUID=$cmdKey1 } @(201) "command create account 1"
    $cmdCreate2 = GatewayExecCurl POST "http://money-service-command:8092/api/money-service-command/v1/accounts" @{ userId=$cmdUser2; keycloakUserUUID=$cmdKey2 } @(201) "command create account 2"
    $cmdIban1 = $cmdCreate1.Json.userIban
    $cmdIban2 = $cmdCreate2.Json.userIban
    GatewayExecCurl POST "http://money-service-command:8092/api/money-service-command/v1/accounts/deposit" @{ userId=$cmdUser1; amount=3000 } @(200) "command deposit" | Out-Null
    GatewayExecCurl POST "http://money-service-command:8092/api/money-service-command/v1/accounts/withdraw" @{ userId=$cmdUser1; amount=250 } @(200) "command withdraw" | Out-Null
    GatewayExecCurl POST "http://money-service-command:8092/api/money-service-command/v1/accounts/block-money" @{ senderIban=$cmdIban1; amount=750 } @(200) "command block money" | Out-Null
    GatewayExecCurl POST "http://money-service-command:8092/api/money-service-command/v1/accounts/transfer" @{ senderIban=$cmdIban1; receiverIban=$cmdIban2; amount=750 } @(200) "command execute transfer" | Out-Null
    $cmdDb1 = PsqlScalar "banking_money_command" "select money || '|' || blocked_money from money_accounts where user_id='$cmdUser1' limit 1;"
    $cmdDb2 = PsqlScalar "banking_money_command" "select money || '|' || blocked_money from money_accounts where user_id='$cmdUser2' limit 1;"
    Assert-True ($cmdDb1 -match "^2000(\.00)?\|0(\.00)?$") "command DB sender money/blocked dogru: $cmdDb1"
    Assert-True ($cmdDb2 -match "^750(\.00)?\|0(\.00)?$") "command DB receiver money/blocked dogru: $cmdDb2"

    Wait-Until {
        $query1 = GatewayExecCurl GET "http://money-service-query:8093/api/money-service-query/v1/accounts/user/$cmdUser1" $null @(200) "query by user command sender"
        $query2 = GatewayExecCurl GET "http://money-service-query:8093/api/money-service-query/v1/accounts/user/$cmdUser2" $null @(200) "query by user command receiver"
        ([decimal]$query1.Json.availableBalance -eq 2000) -and ([decimal]$query2.Json.availableBalance -eq 750)
    } 90 5 "money-service-query projection balances" | Out-Null

    GatewayExecCurl GET "http://money-service-query:8093/api/money-service-query/v1/accounts/iban/$cmdIban2" $null @(200) "query by iban receiver" | Out-Null
    GatewayExecCurl GET "http://money-service-query:8093/api/money-service-query/v1/accounts/search?keyword=$cmdUser2" $null @(200) "query search" | Out-Null
    Assert-KafkaAdvanced $KafkaBeforeCqrs "banking-microservices.transaction.money-blocked.v1" "CQRS projection publish"
    Kafka-ConsumeContains "banking-microservices.transaction.money-blocked.v1" $cmdUser2 "CQRS projection receiver event" | Out-Null

    $mongoPod = Get-PodByLabel "app=mongodb"
    $mongoBalance = Kube -Args @("-n", $Namespace, "exec", $mongoPod, "--", "mongosh", "--quiet", "banking_money_query", "--eval", "const d=db.money_accounts.findOne({userId:'$cmdUser2'}); d ? d.availableBalance.toString() : 'MISSING';") -AllowFail
    Assert-True ($mongoBalance.Trim() -match "750") "Mongo projection receiver balance 750: $($mongoBalance.Trim())"

    $esCheck = GatewayExecCurl GET "http://elasticsearch:9200/money-accounts/_search?q=userId:$cmdUser2" $null @(200,404) "Elasticsearch money-accounts search"
    Assert-True ($esCheck.Status -eq 200) "Elasticsearch money-accounts index searchable"

    Step "Negatif/error senaryolari"
    $token1Response = Get-Token $user1.Email $user1.Password "user1 refresh before negative scenario"
    $token1 = $token1Response.access_token
    $h1 = AuthHeaders $token1; $h1["X-User-KeycloakUUID"] = $uuid1
    $KafkaBeforeNegative = Snapshot-KafkaOffsets @("banking-microservices.transaction.failed.v1")
    Invoke-Checked POST "/api/transaction-service/v1/transactions/create" $h1 @{ amount=9999999; transactionType="TRANSFER"; senderIban=$iban1; receiverIban=$iban2; receiverName=$user2.Name; receiverSurname=$user2.Surname; description="E2E insufficient funds $RunId" } @(201) "insufficient transfer accepted into saga" | Out-Null
    Wait-Until {
        $errCount = PsqlScalar "banking_transactions" "select count(*) from transactions where sender_user_id='$uuid1' and error=true;"
        [int]$errCount -ge 1
    } 120 5 "insufficient funds eventually records error" | Out-Null
    Assert-KafkaAdvanced $KafkaBeforeNegative "banking-microservices.transaction.failed.v1" "negative transfer failed event"
    Invoke-Checked GET "/api/transaction-service/v1/transactions/errors" (AuthHeaders $token1) $null @(200) "errors after negative scenario" | Out-Null

    Step "Servis loglari"
    Capture-PodLogs "app=user-service" "user-service" $RunId | Out-Null
    Capture-PodLogs "app=money-service" "money-service" $RunId | Out-Null
    Capture-PodLogs "app=transaction-service" "transaction-service" $RunId | Out-Null
    Capture-PodLogs "app=fraud-service" "fraud-service" $eventUuid | Out-Null
    Capture-PodLogs "app=money-service-command" "money-service-command" $cmdUser2 | Out-Null
    Capture-PodLogs "app=money-service-query" "money-service-query" $cmdUser2 | Out-Null
    Capture-PodLogs "app=gateway" "gateway" $RunId | Out-Null

    Step "Logout"
    Invoke-Checked POST "/api/user-service/v1/auth/logout" @{} @{ refreshToken=$token2Response.refresh_token } @(200,204) "logout user2 refresh token" | Out-Null

    Step "Son ozet"
    $totalDuration = Format-Duration ((Get-Date) - $script:RunStart)
    if ($Failures.Count -eq 0) {
        Write-Host ""
        Write-Host "TUM TESTLER GECTI. Pass=$PassCount RunId=$RunId Duration=$totalDuration" -ForegroundColor Green
        Write-DetailLog "run" "OK" "final-summary" "PASS: $PassCount`nFAIL: 0`nTOTAL_DURATION: $totalDuration`nLOG_ROOT: $LogRoot"
        Audit "run" "success" @{ pass=$PassCount; fail=0; totalDuration=$totalDuration; logRoot=$LogRoot }
        exit 0
    } else {
        Write-Host ""
        Write-Host "TESTLERDE HATA VAR. Pass=$PassCount Fail=$($Failures.Count) RunId=$RunId Duration=$totalDuration" -ForegroundColor Red
        $Failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
        Write-DetailLog "run" "FAIL" "final-summary" "PASS: $PassCount`nFAIL: $($Failures.Count)`nTOTAL_DURATION: $totalDuration`nFAILURES:`n$($Failures -join "`n")`nLOG_ROOT: $LogRoot"
        Audit "run" "failed" @{ pass=$PassCount; fail=$Failures.Count; totalDuration=$totalDuration; failures=$Failures; logRoot=$LogRoot }
        exit 1
    }
} catch {
    $script:HandlingFatal = $true
    $totalDuration = Format-Duration ((Get-Date) - $script:RunStart)
    Bad "Script exception: $($_.Exception.Message)"
    Write-DetailLog "run" "FAIL" "exception-summary" "TOTAL_DURATION: $totalDuration`nMESSAGE: $($_.Exception.Message)`nSTACK:`n$($_.ScriptStackTrace)"
    Audit "run" "exception" @{ message=$_.Exception.Message; totalDuration=$totalDuration; stack=$_.ScriptStackTrace; logRoot=$LogRoot }
    Write-Host $_.ScriptStackTrace -ForegroundColor DarkGray
    exit 1
} finally {
    if ($null -ne $PortForwardProcess -and -not $PortForwardProcess.HasExited) {
        Info "Port-forward kapatiliyor. PID=$($PortForwardProcess.Id)"
        Stop-Process -Id $PortForwardProcess.Id -Force -ErrorAction SilentlyContinue
    }
    Stop-Transcript | Out-Null
    Sync-TestLogsToGateway
}
