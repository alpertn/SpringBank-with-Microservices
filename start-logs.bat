@echo off
chcp 65001 >nul
setlocal

:MENU
cls
echo =====================================================
echo   SPRINGBANK - MIKROSERVIS KUBERNETES LOGLARI
echo =====================================================
echo.
echo   [1]  TUM MIKROSERVISLER (canli akis - tail)
echo   [2]  Gateway
echo   [3]  Auth Service
echo   [4]  User Service
echo   [5]  Transaction Service
echo   [6]  Money Service
echo   [7]  Fraud Service
echo.
echo   [8]  TUM MIKROSERVISLER - Son 100 Satir (anlik akmasiz)
echo   [9]  Sadece HATA (ERROR/WARN/Exception) Loglarini Goster
echo  [10]  Belirli Kelime Ara (tum servisler)
echo.
echo   [0]  Cikis
echo =====================================================
set /p "secim=Seciminizi yapin (0-10): "

if "%secim%"=="1" goto ALL_TAIL
if "%secim%"=="2" goto SVC_GATEWAY
if "%secim%"=="3" goto SVC_AUTH
if "%secim%"=="4" goto SVC_USER
if "%secim%"=="5" goto SVC_TRANSACTION
if "%secim%"=="6" goto SVC_MONEY
if "%secim%"=="7" goto SVC_FRAUD
if "%secim%"=="8" goto ALL_SNAPSHOT
if "%secim%"=="9" goto ALL_ERRORS
if "%secim%"=="10" goto SEARCH_WORD
if "%secim%"=="0" goto END

echo Gecersiz secim! Tekrar deneyin.
timeout /t 2 /nobreak >nul
goto MENU

:: -------------------------------------------------------
:ALL_TAIL
cls
echo [TUM MIKROSERVISLER - CANLI AKIS]
echo (Tum 12+ poddan loglar eksiksiz okunuyor...)
echo Cikmak icin CTRL+C ye basin...
echo -------------------------------------------------------
:: max-log-requests=50 yapildi cunku 6 servis * 2 = 12 pod var. Eger 10 birakilirsa bazilarini atlar!
kubectl logs -n banking-microservices -l "app in (gateway, auth-service, user-service, transaction-service, money-service, fraud-service)" -f --max-log-requests=50
goto AFTER

:: -------------------------------------------------------
:SVC_GATEWAY
cls
echo [GATEWAY - Son 200 Satir + Canli Akis]
echo Cikmak icin CTRL+C ye basin...
echo -------------------------------------------------------
kubectl logs -n banking-microservices -l app=gateway --tail=200 -f --max-log-requests=10
goto AFTER

:: -------------------------------------------------------
:SVC_AUTH
cls
echo [AUTH SERVICE - Son 200 Satir + Canli Akis]
echo Cikmak icin CTRL+C ye basin...
echo -------------------------------------------------------
kubectl logs -n banking-microservices -l app=auth-service --tail=200 -f --max-log-requests=10
goto AFTER

:: -------------------------------------------------------
:SVC_USER
cls
echo [USER SERVICE - Son 200 Satir + Canli Akis]
echo Cikmak icin CTRL+C ye basin...
echo -------------------------------------------------------
kubectl logs -n banking-microservices -l app=user-service --tail=200 -f --max-log-requests=10
goto AFTER

:: -------------------------------------------------------
:SVC_TRANSACTION
cls
echo [TRANSACTION SERVICE - Son 200 Satir + Canli Akis]
echo Cikmak icin CTRL+C ye basin...
echo -------------------------------------------------------
kubectl logs -n banking-microservices -l app=transaction-service --tail=200 -f --max-log-requests=10
goto AFTER

:: -------------------------------------------------------
:SVC_MONEY
cls
echo [MONEY SERVICE - Son 200 Satir + Canli Akis]
echo Cikmak icin CTRL+C ye basin...
echo -------------------------------------------------------
kubectl logs -n banking-microservices -l app=money-service --tail=200 -f --max-log-requests=10
goto AFTER

:: -------------------------------------------------------
:SVC_FRAUD
cls
echo [FRAUD SERVICE - Son 200 Satir + Canli Akis]
echo Cikmak icin CTRL+C ye basin...
echo -------------------------------------------------------
kubectl logs -n banking-microservices -l app=fraud-service --tail=200 -f --max-log-requests=10
goto AFTER

:: -------------------------------------------------------
:ALL_SNAPSHOT
cls
echo [TUM MIKROSERVISLER - ANLIK SNAPSHOT - Son 100 Satir]
echo -------------------------------------------------------
for %%S in (gateway auth-service user-service transaction-service money-service fraud-service) do (
    echo.
    echo === %%S ===
    kubectl logs -n banking-microservices -l app=%%S --tail=100 --max-log-requests=10
)
goto AFTER

:: -------------------------------------------------------
:ALL_ERRORS
cls
echo [TUM MIKROSERVISLER - SADECE HATALAR]
echo "ERROR" kelimesi gecen satirlar gosteriliyor...
echo -------------------------------------------------------
for %%S in (gateway auth-service user-service transaction-service money-service fraud-service) do (
    echo.
    echo === %%S - ERRORS ===
    kubectl logs -n banking-microservices -l app=%%S --tail=500 --max-log-requests=10 2>&1 | findstr /i "ERROR Exception error WARN"
)
goto AFTER

:: -------------------------------------------------------
:SEARCH_WORD
cls
set /p "keyword=Aramak istediginiz kelimeyi girin: "
echo.
echo [TUM SERVISLER - '%keyword%' ARANIMI]
echo -------------------------------------------------------
for %%S in (gateway auth-service user-service transaction-service money-service fraud-service) do (
    echo.
    echo === %%S ===
    kubectl logs -n banking-microservices -l app=%%S --tail=500 --max-log-requests=10 2>&1 | findstr /i "%keyword%"
)
goto AFTER

:: -------------------------------------------------------
:END
cls
echo Cikiliyor...
exit /b 0

:: -------------------------------------------------------
:AFTER
echo.
echo -------------------------------------------------------
echo [Bitti] Menuye donmek icin bir tusa basin...
pause >nul
goto MENU
