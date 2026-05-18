@echo off
chcp 65001 >nul
setlocal

set "SCRIPT_DIR=%~dp0"
set "REPO_ROOT=%SCRIPT_DIR%.."
set "LOG_DIR=%SCRIPT_DIR%logs"
set "NAMESPACE=banking-microservices"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$logDir=$env:LOG_DIR;" ^
  "$namespace=$env:NAMESPACE;" ^
  "$index=1;" ^
  "while(Test-Path (Join-Path $logDir ('logs' + $index + '.log'))){$index++};" ^
  "$logFile=Join-Path $logDir ('logs' + $index + '.log');" ^
  "Write-Host ('Loglar yaziliyor: ' + $logFile) -ForegroundColor Cyan;" ^
  "Write-Host 'Izlenen servisler: gateway, admin-service, admin-service-command, admin-service-query, user-service, money-service, money-service-command, money-service-query, transaction-service, fraud-service' -ForegroundColor DarkCyan;" ^
  "kubectl logs -n $namespace -l 'app in (gateway,admin-service,admin-service-command,admin-service-query,user-service,money-service,money-service-command,money-service-query,transaction-service,fraud-service)' -f --tail=200 --max-log-requests=50 | Tee-Object -FilePath $logFile"

pause
