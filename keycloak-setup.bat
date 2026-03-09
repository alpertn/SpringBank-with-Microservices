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

echo Tum adimlar PowerShell ile calistirilacak...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$adminTokenResponse = Invoke-RestMethod -Uri 'http://localhost:8080/realms/master/protocol/openid-connect/token' -Method Post -Body @{ client_id = 'admin-cli'; username = 'alper123'; password = 'alper123A'; grant_type = 'password' }; " ^
  "$adminToken = $adminTokenResponse.access_token; " ^
  "if (-not $adminToken) { Write-Host 'HATA: Admin token alinamadi!'; exit 1 }; " ^
  "Write-Host '[1/6] Admin token alindi.'; " ^
  "" ^
  "$realmConfig = @{ realm = 'banking'; enabled = $true } | ConvertTo-Json; " ^
  "try { Invoke-RestMethod -Uri 'http://localhost:8080/admin/realms' -Method Post -Headers @{ Authorization = \"Bearer $adminToken\" } -Body $realmConfig -ContentType 'application/json'; Write-Host '[2/6] Realm olusturuldu.' } catch { Write-Host '[2/6] Realm zaten mevcut.' }; " ^
  "" ^
  "try { Invoke-RestMethod -Uri 'http://localhost:8080/admin/realms/banking/roles' -Method Post -Headers @{ Authorization = \"Bearer $adminToken\" } -Body '{\"name\":\"USER\"}' -ContentType 'application/json'; Write-Host '[3/6] USER rolu olusturuldu.' } catch { Write-Host '[3/6] USER rolu zaten mevcut.' }; " ^
  "" ^
  "try { Invoke-RestMethod -Uri 'http://localhost:8080/admin/realms/banking/roles' -Method Post -Headers @{ Authorization = \"Bearer $adminToken\" } -Body '{\"name\":\"ADMIN\"}' -ContentType 'application/json'; Write-Host '[4/6] ADMIN rolu olusturuldu.' } catch { Write-Host '[4/6] ADMIN rolu zaten mevcut.' }; " ^
  "" ^
  "$clientConfig = @{ clientId = 'banking-app'; secret = 'Iopu5gL8VfLtIX39701gkwd6iCd7gKW6'; enabled = $true; publicClient = $false; directAccessGrantsEnabled = $true; serviceAccountsEnabled = $true; standardFlowEnabled = $true } | ConvertTo-Json; " ^
  "try { Invoke-RestMethod -Uri 'http://localhost:8080/admin/realms/banking/clients' -Method Post -Headers @{ Authorization = \"Bearer $adminToken\" } -Body $clientConfig -ContentType 'application/json'; Write-Host '[5/6] Client olusturuldu.' } catch { Write-Host '[5/6] Client zaten mevcut.' }; " ^
  "" ^
  "$userConfig = @{ username = 'testuser@bank.com'; email = 'testuser@bank.com'; enabled = $true; emailVerified = $true; credentials = @(@{ type = 'password'; value = 'pass123'; temporary = $false }) } | ConvertTo-Json -Depth 5; " ^
  "try { Invoke-RestMethod -Uri 'http://localhost:8080/admin/realms/banking/users' -Method Post -Headers @{ Authorization = \"Bearer $adminToken\" } -Body $userConfig -ContentType 'application/json'; Write-Host '[6/6] Kullanici olusturuldu.' } catch { Write-Host '[6/6] Kullanici zaten mevcut.' }; " ^
  "Write-Host ''; Write-Host 'Kurulum tamamlandi!'"

echo.
echo ============================================
echo   Kurulum Tamamlandi!
echo ============================================
echo.
echo Test Kullanicisi: testuser@bank.com / pass123
echo Frontend: http://localhost:8095
echo.
echo ============================================
echo   KEYCLOAK ADMIN UI ILE MANUEL KURULUM
echo ============================================
echo.
echo Bu bat dosyasi hata verirse asagidaki adimlari
echo Keycloak Admin UI uzerinden yapabilirsiniz:
echo.
echo 1. KEYCLOAK ADMIN PANELINE GIRIS:
echo    - Tarayicida http://localhost:8080 adresine gidin
echo    - Kullanici adi: alper123
echo    - Sifre: alper123A
echo    - "Sign In" butonuna tiklayin
echo.
echo 2. REALM OLUSTURMA:
echo    - Sol ust kosede "master" yazan dropdown'a tiklayin
echo    - "Create realm" butonuna tiklayin
echo    - "Realm name" alanina: banking yazin
echo    - "Enabled" toggle'ini ON yapin
echo    - "Create" butonuna tiklayin
echo.
echo 3. REALM ROLLERI OLUSTURMA (ONEMLI!):
echo    - Sol menuden "Realm roles" sekmesine tiklayin
echo    - "Create role" butonuna tiklayin
echo    - Role name: USER yazin, "Save" tiklayin
echo    - Tekrar "Create role" tiklayin
echo    - Role name: ADMIN yazin, "Save" tiklayin
echo    NOT: Bu roller olusturulmazsa kayit sirasinda
echo    "assign role" hatasi alinir!
echo.
echo 4. CLIENT OLUSTURMA (banking realm secili iken):
echo    - Sol menuden "Clients" sekmesine tiklayin
echo    - "Create client" butonuna tiklayin
echo    - Client type: OpenID Connect (varsayilan)
echo    - Client ID: banking-app yazin
echo    - "Next" tiklayin
echo    - "Client authentication" toggle'ini ON yapin
echo    - "Direct access grants" checkbox'ini isaretleyin
echo    - "Service accounts roles" checkbox'ini isaretleyin
echo    - "Save" tiklayin
echo    - Acilan sayfada "Credentials" sekmesine gidin
echo    - "Client secret" alanini kopyalayin
echo    - Beklenen secret: Iopu5gL8VfLtIX39701gkwd6iCd7gKW6
echo.
echo 5. TEST KULLANICISI OLUSTURMA (banking realm secili iken):
echo    - Sol menuden "Users" sekmesine tiklayin
echo    - "Add user" butonuna tiklayin
echo    - Username: testuser@bank.com
echo    - Email: testuser@bank.com
echo    - "Email verified" toggle'ini ON yapin
echo    - "Create" tiklayin
echo    - Olusturulan kullaniciya tiklayin
echo    - "Credentials" sekmesine gidin
echo    - "Set password" tiklayin
echo    - Password: pass123
echo    - Password confirmation: pass123
echo    - "Temporary" toggle'ini OFF yapin (onemli!)
echo    - "Save" tiklayin
echo    - Onay kutusunda "Save password" tiklayin
echo.
echo ============================================
echo.
pause
