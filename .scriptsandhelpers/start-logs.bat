@echo off
set SCRIPT=%TEMP%\kube_logs_temp.ps1

echo $LOG_DIR = 'logs' > %SCRIPT%
echo if (-not (Test-Path $LOG_DIR)) { New-Item -ItemType Directory -Path $LOG_DIR ^| Out-Null } >> %SCRIPT%
echo $INDEX = 1 >> %SCRIPT%
echo while (Test-Path "$LOG_DIR\logs$INDEX.log") { $INDEX++ } >> %SCRIPT%
echo $LOG_FILE = "$LOG_DIR\logs$INDEX.log" >> %SCRIPT%
echo Write-Host 'Loglar yaziliyor:' $LOG_FILE >> %SCRIPT%
echo kubectl logs -n banking-microservices -l 'app in (gateway, auth-service, user-service, transaction-service, money-service, fraud-service)' -f --max-log-requests=50 ^| Tee-Object -FilePath $LOG_FILE >> %SCRIPT%

powershell -ExecutionPolicy Bypass -File %SCRIPT%
pause