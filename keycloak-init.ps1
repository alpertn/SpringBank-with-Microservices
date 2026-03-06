$ErrorActionPreference = "Stop"

Write-Host "Waiting for Keycloak to be ready..."
$ready = $false
while (-not $ready) {
    try {
        Start-Sleep -Seconds 5
        $adminTokenResponse = Invoke-RestMethod -Uri "http://localhost:8080/realms/master/protocol/openid-connect/token" -Method Post -Body @{
            client_id  = "admin-cli"
            username   = "alper123"
            password   = "alper123A"
            grant_type = "password"
        } -ErrorAction Stop
        $adminToken = $adminTokenResponse.access_token
        if ($adminToken) {
            $ready = $true
            Write-Host "Keycloak is ready!"
        }
    }
    catch {
        Write-Host "Not ready yet..."
    }
}

Write-Host "Creating Banking Realm..."
try {
    $realmConfig = @{ realm = "banking"; enabled = $true } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/admin/realms" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $realmConfig -ContentType "application/json"
}
catch {}

Write-Host "Creating client banking-app..."
try {
    $clientConfig = @{
        clientId                  = "banking-app"
        secret                    = "Iopu5gL8VfLtIX39701gkwd6iCd7gKW6"
        enabled                   = $true
        publicClient              = $false
        directAccessGrantsEnabled = $true
        serviceAccountsEnabled    = $true
        standardFlowEnabled       = $true
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/banking/clients" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $clientConfig -ContentType "application/json"
}
catch {}

Write-Host "Creating USER role..."
try {
    $roleConfig = @{
        name        = "USER"
        description = "Normal User Role"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/admin/realms/banking/roles" -Method Post -Headers @{ Authorization = "Bearer $adminToken" } -Body $roleConfig -ContentType "application/json"
}
catch {}
Write-Host "Keycloak initialized successfully!"
