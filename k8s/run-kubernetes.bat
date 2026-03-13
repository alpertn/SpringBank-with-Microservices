@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

echo ============================================
echo   SpringBank - Kubernetes Deployment Script
echo ============================================
echo.

echo [1/6] Eski Kubernetes baglantilari ve kaynaklari temizleniyor...
kubectl delete namespace banking-microservices --ignore-not-found=true
echo Eski namespace silinmesi bildirildi. Namespace'in tamamen silinmesi biraz zaman alabilir, bekleniyor...
:WAIT_NS
kubectl get namespace banking-microservices >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    timeout /t 5 /nobreak >nul
    goto WAIT_NS
)
echo Namespace basariyla temizlendi.
echo.

echo [2/6] Eski Docker imageleri temizleniyor...
for /f "tokens=*" %%i in ('docker images "springbankwithmicroservices/*" -q') do (
    docker rmi -f %%i
)
echo Eski Docker imageleri temizlendi.
echo.

echo [3/6] Mikroservisler Maven ile build ediliyor...
set SERVICES=auth-service fraud-service gateway money-service transaction-service user-service
for %%s in (%SERVICES%) do (
    echo.
    echo ------------------------------------------
    echo Buidling %%s ...
    echo ------------------------------------------
    pushd %%s
    call mvn clean package -DskipTests
    if !ERRORLEVEL! NEQ 0 (
        echo HATA: %%s build edilemedi! Islemler durduruluyor.
        exit /b 1
    )
    popd
)
echo.
echo Maven build islemleri tamamlandi.
echo.

echo [4/6] Docker Imageleri olusturuluyor...
for %%s in (%SERVICES%) do (
    echo.
    echo ------------------------------------------
    echo Dockerizing %%s ...
    echo ------------------------------------------
    docker build -t SpringBankWithMicroservices/%%s:1.0.0 ./%%s
    if !ERRORLEVEL! NEQ 0 (
        echo HATA: %%s docker imaji olusturulamadi!
        exit /b 1
    )
)
echo.
echo Docker imageleri basariyla olusturuldu.
echo.

echo [5/6] Kubernetes altyapisi baslatiliyor (Base)...
echo kubectl apply -f k8s/01-Base.yaml (Namespace dahil olmak uzere altyapi bilesenleri)
kubectl apply -f k8s/01-Base.yaml

echo.
echo Altyapi bilesenlerinin ayaga kalkmasi icin 60 saniye bekleniyor...
timeout /t 60 /nobreak
echo Mikroservisler baslatiliyor (Apps)...
kubectl apply -f k8s/02-Apps.yaml

echo Uygulamalarin hazir olmasi bekleniyor...
timeout /t 30 /nobreak
echo.

echo [6/6] Keycloak yapilandirmasi baslatiliyor...
call keycloak-setup.bat

echo.
echo ============================================
echo   TUM ISLEMLER BASARIYLA TAMAMLANDI!
echo ============================================
echo.
echo Pod durumlarini kontrol etmek iicn: kubectl get pods -n banking-microservices
echo Gateway (Frontend) erisimi: http://localhost:8095
echo.
pause
