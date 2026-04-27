@echo off
chcp 65001 >nul
echo ============================================
echo   SpringBank - Keycloak Otomatik Kurulum
echo ============================================
echo.
echo Keycloak'un ayaga kalkmasi bekleniyor...
echo.

:WAIT_KEYCLOAK
timeout /t 5 /nobreak >nul
curl -s -o nul http://localhost:8080/realms/master
if %ERRORLEVEL% NEQ 0 (
    echo Keycloak henuz hazir degil, bekleniyor...
    goto WAIT_KEYCLOAK
)
echo Keycloak hazir!
echo.

echo Tum adimlar Node.js ile calistirilacak...
echo.

node keycloak-setup.js

echo.
pause
