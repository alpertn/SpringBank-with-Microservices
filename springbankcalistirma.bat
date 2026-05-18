@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "NAMESPACE=banking-microservices"

cd /d "%ROOT%"

:MENU
cls
echo ============================================================
echo              SpringBank Calistirma Menusu
echo ============================================================
echo.
echo  1. Her seyi yap
echo     - Image build
echo     - Kubernetes dosyalarini uygula/guncelle
echo     - Deployment restart
echo     - Pod durumunu goster
echo.
echo  2. Sadece build et
echo.
echo  3. Calistir / Kubernetes dosyalarini uygula-guncelle
echo.
echo  4. Deployment restart et
echo.
echo  5. Durum goster
echo.
echo  6. Loglari izle
echo.
echo  7. Test calistir ^(dev^)
echo.
echo  8. Cikis
echo.
set /p "CHOICE=Secim: "

if "%CHOICE%"=="1" goto ALL
if "%CHOICE%"=="2" goto BUILD
if "%CHOICE%"=="3" goto APPLY
if "%CHOICE%"=="4" goto RESTART
if "%CHOICE%"=="5" goto STATUS
if "%CHOICE%"=="6" goto LOGS
if "%CHOICE%"=="7" goto TEST_DEV
if "%CHOICE%"=="8" goto END

echo.
echo Gecersiz secim.
pause
goto MENU

:ALL
call :BUILD_IMAGES || goto FAIL
call :APPLY_K8S || goto FAIL
call :RESTART_DEPLOYMENTS || goto FAIL
call :SHOW_STATUS
echo.
echo Her sey tamamlandi.
pause
goto MENU

:BUILD
call :BUILD_IMAGES || goto FAIL
echo.
echo Build tamamlandi.
pause
goto MENU

:APPLY
call :APPLY_K8S || goto FAIL
call :SHOW_STATUS
echo.
echo Kubernetes dosyalari uygulandi/guncellendi.
pause
goto MENU

:RESTART
call :RESTART_DEPLOYMENTS || goto FAIL
call :SHOW_STATUS
echo.
echo Deployment restart tamamlandi.
pause
goto MENU

:STATUS
call :SHOW_STATUS
pause
goto MENU

:LOGS
call ".scriptsandhelpers\start-logs.bat"
goto MENU

:TEST_DEV
call ".scriptsandhelpers\test-everything.bat" dev
pause
goto MENU

:BUILD_IMAGES
echo.
echo [1/10] admin-service image build...
docker build -t springbankwithmicroservices/admin-service:1.0.0 ./admin-service || exit /b 1

echo.
echo [2/10] admin-service-command image build...
docker build -t springbankwithmicroservices/admin-service-command:1.0.0 ./admin-service-command || exit /b 1

echo.
echo [3/10] admin-service-query image build...
docker build -t springbankwithmicroservices/admin-service-query:1.0.0 ./admin-service-query || exit /b 1

echo.
echo [4/10] user-service image build...
docker build -t springbankwithmicroservices/user-service:1.0.0 ./user-service || exit /b 1

echo.
echo [5/10] money-service image build...
docker build -t springbankwithmicroservices/money-service:1.0.0 ./money-service || exit /b 1

echo.
echo [6/10] money-service-command image build...
docker build -t springbankwithmicroservices/money-service-command:1.0.0 ./money-service-command || exit /b 1

echo.
echo [7/10] money-service-query image build...
docker build -t springbankwithmicroservices/money-service-query:1.0.0 ./money-service-query || exit /b 1

echo.
echo [8/10] transaction-service image build...
docker build -t springbankwithmicroservices/transaction-service:1.0.0 ./transaction-service || exit /b 1

echo.
echo [9/10] gateway image build...
docker build -t springbankwithmicroservices/gateway:1.0.0 ./gateway || exit /b 1

echo.
echo [10/10] fraud-service image build...
docker build -t springbankwithmicroservices/fraud-service:1.0.0 ./fraud-service || exit /b 1

exit /b 0

:APPLY_K8S
echo.
echo Kubernetes base dosyasi uygulaniyor...
kubectl apply -f k8s/01-Base.yaml || exit /b 1

echo.
echo Kubernetes app dosyasi uygulaniyor...
kubectl apply -f k8s/02-Apps.yaml || exit /b 1

exit /b 0

:RESTART_DEPLOYMENTS
echo.
echo Deployment'lar restart ediliyor...
kubectl rollout restart deployment -n %NAMESPACE% || exit /b 1
exit /b 0

:SHOW_STATUS
echo.
echo Deployment, HPA ve Pod durumlari:
kubectl -n %NAMESPACE% get deploy,hpa,pods
echo.
echo Servisler:
kubectl -n %NAMESPACE% get svc
echo.
echo Gateway:
echo   http://localhost:8095
echo.
echo Gateway localhost'ta acilmiyorsa:
echo   kubectl -n %NAMESPACE% port-forward svc/gateway 8095:8095
exit /b 0

:FAIL
echo.
echo [HATA] Islem basarisiz oldu. Yukaridaki loglari kontrol et.
pause
goto MENU

:END
endlocal
exit /b 0
