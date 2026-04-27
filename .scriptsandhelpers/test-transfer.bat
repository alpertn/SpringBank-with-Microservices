@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
::  SpringBank - End-to-End Transfer Test
::  Her calistirmada RASTGELE isim ve email olusturulur
:: ============================================================

set GATEWAY=http://localhost:8095

:: -- Rastgele 8 karakterlik suffix (harf+rakam) --
for /f "delims=" %%R in ('powershell.exe -NoProfile -Command "(-join((48..57+97..122)|Get-Random -Count 8|ForEach-Object{[char]$_}))"') do set RAND1=%%R
for /f "delims=" %%R in ('powershell.exe -NoProfile -Command "(-join((48..57+97..122)|Get-Random -Count 8|ForEach-Object{[char]$_}))"') do set RAND2=%%R

:: -- Rastgele isimler --
for /f "delims=" %%N in ('powershell.exe -NoProfile -Command "('Ahmet Mehmet Ali Ayse Fatma Omer Zeynep Can Berk Selin Emre Deniz Burak Ceren Kemal Hasan Murat Yusuf Elif Nisa' -split ' ' | Get-Random)"') do set USER1_NAME=%%N
for /f "delims=" %%N in ('powershell.exe -NoProfile -Command "('Ahmet Mehmet Ali Ayse Fatma Omer Zeynep Can Berk Selin Emre Deniz Burak Ceren Kemal Hasan Murat Yusuf Elif Nisa' -split ' ' | Get-Random)"') do set USER2_NAME=%%N

for /f "delims=" %%S in ('powershell.exe -NoProfile -Command "('Yilmaz Kaya Demir Sahin Celik Arslan Dogan Kilic Aslan Ozturk Aydin Ozdemir Sari Yildiz Kurt Ozkan Simsek Polat Cakir Koc' -split ' ' | Get-Random)"') do set USER1_SURNAME=%%S
for /f "delims=" %%S in ('powershell.exe -NoProfile -Command "('Yilmaz Kaya Demir Sahin Celik Arslan Dogan Kilic Aslan Ozturk Aydin Ozdemir Sari Yildiz Kurt Ozkan Simsek Polat Cakir Koc' -split ' ' | Get-Random)"') do set USER2_SURNAME=%%S

:: -- Email olustur (kucuk harf) --
for /f "delims=" %%E in ('powershell.exe -NoProfile -Command "('%USER1_NAME%.%USER1_SURNAME%_%RAND1%@springbank.test').ToLower()"') do set USER1_EMAIL=%%E
for /f "delims=" %%E in ('powershell.exe -NoProfile -Command "('%USER2_NAME%.%USER2_SURNAME%_%RAND2%@springbank.test').ToLower()"') do set USER2_EMAIL=%%E

set USER1_PASS=Test1234!
set USER2_PASS=Test1234!

echo.
echo ==============================================================
echo    SpringBank End-to-End Transfer Testi
echo ==============================================================
echo.
echo  Kullanici 1 : !USER1_NAME! !USER1_SURNAME!
echo               !USER1_EMAIL!
echo  Kullanici 2 : !USER2_NAME! !USER2_SURNAME!
echo               !USER2_EMAIL!
echo.

:: ADIM 1: Kullanici 1 Kayit
echo [ADIM 1/10] Kullanici 1 kayit ediliyor...
curl.exe -s -X POST "%GATEWAY%/api/auth-service/v1/auth/register" -H "Content-Type: application/json" -d "{\"email\":\"!USER1_EMAIL!\",\"password\":\"%USER1_PASS%\",\"name\":\"!USER1_NAME!\",\"surname\":\"!USER1_SURNAME!\",\"role\":\"USER\"}"
echo.
echo  [7sn bekleniyor - Kafka user-create akisi...]
powershell.exe -NoProfile -Command "Start-Sleep -Seconds 7"

:: ADIM 2: Kullanici 2 Kayit
echo.
echo [ADIM 2/10] Kullanici 2 kayit ediliyor...
curl.exe -s -X POST "%GATEWAY%/api/auth-service/v1/auth/register" -H "Content-Type: application/json" -d "{\"email\":\"!USER2_EMAIL!\",\"password\":\"%USER2_PASS%\",\"name\":\"!USER2_NAME!\",\"surname\":\"!USER2_SURNAME!\",\"role\":\"USER\"}"
echo.
echo  [7sn bekleniyor - Kafka user-create akisi...]
powershell.exe -NoProfile -Command "Start-Sleep -Seconds 7"

:: ADIM 3: Kullanici 1 Login
echo.
echo [ADIM 3/10] Kullanici 1 token aliniyor...
set TMPFILE1=%TEMP%\bank_user1_token.json
curl.exe -s -X POST "%GATEWAY%/api/auth-service/v1/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"!USER1_EMAIL!\",\"password\":\"%USER1_PASS%\"}" -o "%TMPFILE1%"

for /f "delims=" %%T in ('powershell.exe -NoProfile -Command "(Get-Content '%TMPFILE1%' | ConvertFrom-Json).access_token"') do set TOKEN1=%%T
for /f "delims=" %%U in ('powershell.exe -NoProfile -Command "$t=(Get-Content '%TMPFILE1%'|ConvertFrom-Json).access_token;$p=$t.Split('.')[1];switch($p.Length%%4){2{$p+='=='}3{$p+='='}};$b=[System.Convert]::FromBase64String($p);([System.Text.Encoding]::UTF8.GetString($b)|ConvertFrom-Json).sub"') do set UUID1=%%U

if "!TOKEN1!"=="" (
    echo  [HATA] Kullanici 1 token alinamadi! Login yaniti:
    type "%TMPFILE1%"
    echo.
    goto :FAIL
)
echo  Token alindi.
echo  UUID : !UUID1!
echo  Token: !TOKEN1:~0,50!...

:: ADIM 4: Kullanici 2 Login
echo.
echo [ADIM 4/10] Kullanici 2 token aliniyor...
set TMPFILE2=%TEMP%\bank_user2_token.json
curl.exe -s -X POST "%GATEWAY%/api/auth-service/v1/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"!USER2_EMAIL!\",\"password\":\"%USER2_PASS%\"}" -o "%TMPFILE2%"

for /f "delims=" %%T in ('powershell.exe -NoProfile -Command "(Get-Content '%TMPFILE2%' | ConvertFrom-Json).access_token"') do set TOKEN2=%%T
for /f "delims=" %%U in ('powershell.exe -NoProfile -Command "$t=(Get-Content '%TMPFILE2%'|ConvertFrom-Json).access_token;$p=$t.Split('.')[1];switch($p.Length%%4){2{$p+='=='}3{$p+='='}};$b=[System.Convert]::FromBase64String($p);([System.Text.Encoding]::UTF8.GetString($b)|ConvertFrom-Json).sub"') do set UUID2=%%U

if "!TOKEN2!"=="" (
    echo  [HATA] Kullanici 2 token alinamadi! Login yaniti:
    type "%TMPFILE2%"
    echo.
    goto :FAIL
)
echo  Token alindi.
echo  UUID : !UUID2!
echo  Token: !TOKEN2:~0,50!...

:: ADIM 5: Money hesaplari olustur
echo.
echo [ADIM 5/10] Money hesaplari olusturuluyor...
echo  Kullanici 1 money hesabi...
curl.exe -s -X POST "%GATEWAY%/api/money-service/v1/accounts/createusermoney" -H "Authorization: Bearer !TOKEN1!" -H "X-User-KeycloakUUID: !UUID1!"
echo.
echo  Kullanici 2 money hesabi...
curl.exe -s -X POST "%GATEWAY%/api/money-service/v1/accounts/createusermoney" -H "Authorization: Bearer !TOKEN2!" -H "X-User-KeycloakUUID: !UUID2!"
echo.
echo  [5sn bekleniyor...]
powershell.exe -NoProfile -Command "Start-Sleep -Seconds 5"

:: ADIM 6: Kullanici 2 IBAN al
echo.
echo [ADIM 6/10] Kullanici 2 IBAN bilgisi aliniyor...
set TMPFILE_IBAN=%TEMP%\bank_user2_iban.json
curl.exe -s -X GET "%GATEWAY%/api/money-service/v1/accounts/balance-info" -H "Authorization: Bearer !TOKEN2!" -H "X-User-KeycloakUUID: !UUID2!" -o "%TMPFILE_IBAN%"

for /f "delims=" %%I in ('powershell.exe -NoProfile -Command "(Get-Content '%TMPFILE_IBAN%' | ConvertFrom-Json).userIban"') do set USER2_IBAN=%%I

if "!USER2_IBAN!"=="" (
    echo  [HATA] Kullanici 2 IBAN alinamadi! Yanit:
    type "%TMPFILE_IBAN%"
    echo.
    goto :FAIL
)
echo  Kullanici 2 IBAN: !USER2_IBAN!

:: ADIM 7: Deposit oncesi bakiye
echo.
echo [ADIM 7/10] Deposit oncesi bakiyeler:
echo  -- Kullanici 1 --
curl.exe -s -X GET "%GATEWAY%/api/money-service/v1/accounts/balance-info" -H "Authorization: Bearer !TOKEN1!" -H "X-User-KeycloakUUID: !UUID1!"
echo.
echo  -- Kullanici 2 --
curl.exe -s -X GET "%GATEWAY%/api/money-service/v1/accounts/balance-info" -H "Authorization: Bearer !TOKEN2!" -H "X-User-KeycloakUUID: !UUID2!"
echo.

:: ADIM 8: DEPOSIT 5000 TL
echo.
echo [ADIM 8/10] Kullanici 1'e DEPOSIT (5000 TL)...
curl.exe -s -X POST "%GATEWAY%/api/transaction-service/v1/transactions/create" -H "Authorization: Bearer !TOKEN1!" -H "Content-Type: application/json" -H "X-User-KeycloakUUID: !UUID1!" -H "X-User-Email: !USER1_EMAIL!" -H "X-User-Name: !USER1_NAME!" -H "X-User-Surname: !USER1_SURNAME!" -d "{\"amount\":5000,\"transactionType\":\"DEPOSIT\",\"senderIban\":null,\"receiverIban\":null,\"receiverName\":null,\"receiverSurname\":null,\"description\":\"Test deposit\"}"
echo.
echo  [7sn bekleniyor - Kafka DEPOSIT akisi...]
powershell.exe -NoProfile -Command "Start-Sleep -Seconds 7"
echo  -- Deposit sonrasi Kullanici 1 --
curl.exe -s -X GET "%GATEWAY%/api/money-service/v1/accounts/balance-info" -H "Authorization: Bearer !TOKEN1!" -H "X-User-KeycloakUUID: !UUID1!"
echo.

:: ADIM 9: TRANSFER 1000 TL
echo.
echo [ADIM 9/10] TRANSFER: !USER1_NAME! ^> !USER2_NAME! (1000 TL)
echo  Alici IBAN     : !USER2_IBAN!
echo  Alici Ad/Soyad : !USER2_NAME! !USER2_SURNAME!
echo.
curl.exe -s -X POST "%GATEWAY%/api/transaction-service/v1/transactions/create" -H "Authorization: Bearer !TOKEN1!" -H "Content-Type: application/json" -H "X-User-KeycloakUUID: !UUID1!" -H "X-User-Email: !USER1_EMAIL!" -H "X-User-Name: !USER1_NAME!" -H "X-User-Surname: !USER1_SURNAME!" -d "{\"amount\":1000,\"transactionType\":\"TRANSFER\",\"senderIban\":null,\"receiverIban\":\"!USER2_IBAN!\",\"receiverName\":\"!USER2_NAME!\",\"receiverSurname\":\"!USER2_SURNAME!\",\"description\":\"Test transfer\"}"
echo.
echo.
echo  [30sn bekleniyor - Kafka: block > user-validate > fraud > execute...]
powershell.exe -NoProfile -Command "Start-Sleep -Seconds 30"

:: ADIM 10: Son bakiyeler
echo.
echo [ADIM 10/10] SONUC - Transfer sonrasi:
echo.
echo  === !USER1_NAME! !USER1_SURNAME! (Gonderen) - 4000 bekleniyor ===
curl.exe -s -X GET "%GATEWAY%/api/money-service/v1/accounts/balance-info" -H "Authorization: Bearer !TOKEN1!" -H "X-User-KeycloakUUID: !UUID1!"
echo.
echo  === !USER2_NAME! !USER2_SURNAME! (Alan) - 1000 bekleniyor ===
curl.exe -s -X GET "%GATEWAY%/api/money-service/v1/accounts/balance-info" -H "Authorization: Bearer !TOKEN2!" -H "X-User-KeycloakUUID: !UUID2!"
echo.
echo  === !USER1_NAME! Islem Gecmisi ===
curl.exe -s -X GET "%GATEWAY%/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=!UUID1!" -H "Authorization: Bearer !TOKEN1!"
echo.
echo  === !USER2_NAME! Islem Gecmisi ===
curl.exe -s -X GET "%GATEWAY%/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=!UUID2!" -H "Authorization: Bearer !TOKEN2!"
echo.
echo ==============================================================
echo    Test TAMAMLANDI!
echo    Kullanici 1: !USER1_NAME! !USER1_SURNAME! (!USER1_EMAIL!)
echo    Kullanici 2: !USER2_NAME! !USER2_SURNAME! (!USER2_EMAIL!)
echo    Loglar icin: start-logs.bat
echo ==============================================================
echo.
pause
goto :EOF

:FAIL
echo.
echo ==============================================================
echo    [HATA] Test basarisiz oldu!
echo ==============================================================
pause