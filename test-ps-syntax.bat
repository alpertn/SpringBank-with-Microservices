@echo off
setlocal enabledelayedexpansion

echo === Phase 1: Random Name Generation ===

for /f "delims=" %%R in ('powershell.exe -NoProfile -Command "(-join((48..57+97..122)|Get-Random -Count 8|ForEach-Object{[char]$_}))"') do set RAND1=%%R
for /f "delims=" %%R in ('powershell.exe -NoProfile -Command "(-join((48..57+97..122)|Get-Random -Count 8|ForEach-Object{[char]$_}))"') do set RAND2=%%R

echo RAND1=!RAND1!
echo RAND2=!RAND2!

for /f "delims=" %%N in ('powershell.exe -NoProfile -Command "('Ahmet Mehmet Ali Ayse Fatma' -split ' ' | Get-Random)"') do set USER1_NAME=%%N
for /f "delims=" %%N in ('powershell.exe -NoProfile -Command "('Ahmet Mehmet Ali Ayse Fatma' -split ' ' | Get-Random)"') do set USER2_NAME=%%N

for /f "delims=" %%S in ('powershell.exe -NoProfile -Command "('Yilmaz Kaya Demir Sahin' -split ' ' | Get-Random)"') do set USER1_SURNAME=%%S
for /f "delims=" %%S in ('powershell.exe -NoProfile -Command "('Yilmaz Kaya Demir Sahin' -split ' ' | Get-Random)"') do set USER2_SURNAME=%%S

for /f "delims=" %%E in ('powershell.exe -NoProfile -Command "('!USER1_NAME!.!USER1_SURNAME!_!RAND1!@springbank.test').ToLower()"') do set USER1_EMAIL=%%E
for /f "delims=" %%E in ('powershell.exe -NoProfile -Command "('!USER2_NAME!.!USER2_SURNAME!_!RAND2!@springbank.test').ToLower()"') do set USER2_EMAIL=%%E

echo USER1: !USER1_NAME! !USER1_SURNAME! - !USER1_EMAIL!
echo USER2: !USER2_NAME! !USER2_SURNAME! - !USER2_EMAIL!

echo.
echo === Phase 2: Register User 1 ===
set GATEWAY=http://localhost:8095
set USER1_PASS=Test1234!

curl -s -X POST "%GATEWAY%/api/auth-service/v1/auth/register" -H "Content-Type: application/json" -d "{\"email\":\"!USER1_EMAIL!\",\"password\":\"!USER1_PASS!\",\"name\":\"!USER1_NAME!\",\"surname\":\"!USER1_SURNAME!\",\"role\":\"USER\"}"
echo.
echo Register done, waiting 15s...
timeout /t 15 /nobreak >nul

echo.
echo === Phase 3: Login User 1 ===
set TMPFILE1=%TEMP%\bank_user1_token.json
curl -s -X POST "%GATEWAY%/api/auth-service/v1/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"!USER1_EMAIL!\",\"password\":\"!USER1_PASS!\"}" -o "%TMPFILE1%"
echo.
echo Login response:
type "%TMPFILE1%"
echo.

for /f "delims=" %%T in ('powershell.exe -NoProfile -Command "(Get-Content '%TMPFILE1%' | ConvertFrom-Json).accessToken"') do set TOKEN1=%%T

if "!TOKEN1!"=="" (
    echo [HATA] Token alinamadi!
    goto :FAIL
)
echo TOKEN1 alindi: !TOKEN1:~0,50!...

echo.
echo === Phase 4: Parse UUID ===
for /f "delims=" %%U in ('powershell.exe -NoProfile -Command "$t=(Get-Content '%TMPFILE1%'|ConvertFrom-Json).accessToken;$p=$t.Split('.')[1];switch($p.Length%%4){2{$p+='=='}3{$p+='='}};$b=[System.Convert]::FromBase64String($p);([System.Text.Encoding]::UTF8.GetString($b)|ConvertFrom-Json).sub"') do set UUID1=%%U
echo UUID1=!UUID1!

echo.
echo === TEST OK - Phases 1-4 complete ===
pause
goto :EOF

:FAIL
echo [HATA] Test basarisiz!
pause
