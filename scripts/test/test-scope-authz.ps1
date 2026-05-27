# =============================================================================
# test-scope-authz.ps1
#
# End-to-end smoke test for the scope-aware authorization wired through
# Keycloak groups and ClubAuthorizationService. Walks the matrix:
#
#   anonymous          -> PATCH team => 401
#   gestor (no group)  -> POST   team in club X => 403
#   gestor (in X)      -> POST   team in club X => 201/409 (accepted)
#   gestor (in X)      -> POST   team in club Y => 403   (different club)
#   admin              -> POST   team in club X => 201/409 (always allowed)
#
# Setup the script does for you:
#   - logs in as `admin/admin` (master realm) to get an Admin API token
#   - ensures two clubs exist (creates them on the fly if not)
#   - resolves the `gestor` user id and the /clubs/<X>/managers group id
#   - adds gestor to /clubs/<X>/managers, re-acquires gestor's token, runs
#     the matrix, then removes the membership at the end so re-runs are clean
#
# Requires the docker stack to be up. Run from the repo root:
#   .\scripts\test\test-scope-authz.ps1
# =============================================================================

param(
    [string]$BffUrl       = "http://localhost:8000",
    [string]$KeycloakUrl  = "http://localhost:8180",
    [string]$Realm        = "tessera",
    [string]$AdminUser    = "admin",
    [string]$AdminPass    = "admin",
    [string]$GestorUser   = "gestor",
    [string]$GestorPass   = "gestor"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$pass = 0; $fail = 0
function Check($label, $actual, $expected) {
    if ($expected -contains $actual) {
        Write-Host "  PASS  $label => $actual" -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host "  FAIL  $label => $actual (expected: $($expected -join ' or '))" -ForegroundColor Red
        $script:fail++
    }
}

# Token grabber. Returns just the access token string.
function Get-Token($user, $pass, $clientId = "tessera-web") {
    $body = @{ grant_type = "password"; client_id = $clientId; username = $user; password = $pass }
    $r = Invoke-RestMethod -Method Post `
        -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" -Body $body
    return $r.access_token
}

# Returns HTTP status code without throwing on 4xx/5xx. Works on both
# Windows PowerShell 5.1 and PowerShell 7+ (5.1 lacks -SkipHttpErrorCheck).
function Try-Request($method, $url, $token, $bodyJson = $null) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{
        Method = $method; Uri = $url; Headers = $headers; UseBasicParsing = $true
    }
    if ($bodyJson) {
        $params["ContentType"] = "application/json; charset=utf-8"
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($bodyJson)
    }
    try {
        $r = Invoke-WebRequest @params
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

# Same idea but for one-shot calls where we only care that it succeeded.
function Try-Void($method, $url, $token) {
    try {
        Invoke-WebRequest -Method $method -Uri $url `
            -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing | Out-Null
    } catch {
        # Ignore non-success on cleanup paths.
    }
}

# --- 0. Acquire admin token + Keycloak master admin token ------------------

Write-Host "[0] Acquiring tokens..." -ForegroundColor Cyan
$adminToken = Get-Token $AdminUser $AdminPass
$kcAdminToken = (Invoke-RestMethod -Method Post `
    -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{ grant_type = "password"; client_id = "admin-cli"; username = $AdminUser; password = $AdminPass }).access_token
Write-Host "  OK" -ForegroundColor Green

# --- 1. Ensure two test clubs exist --------------------------------------

Write-Host "[1] Ensuring test clubs exist..." -ForegroundColor Cyan
function Find-ClubByName($name) {
    $list = Invoke-RestMethod -Uri "$BffUrl/api/v1/clubs?size=100" -Headers @{ Authorization = "Bearer $adminToken" }
    return $list.content | Where-Object { $_.name -eq $name } | Select-Object -First 1
}
function Ensure-Club($name) {
    $existing = Find-ClubByName $name
    if ($existing) { return $existing.id }

    $body = '{"name":"' + $name + '"}'
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    try {
        $r = Invoke-RestMethod -Method Post -Uri "$BffUrl/api/v1/clubs" `
            -ContentType "application/json; charset=utf-8" `
            -Headers @{ Authorization = "Bearer $adminToken" } -Body $bytes
        return $r.id
    } catch {
        # 409 race or stale search: re-fetch.
        $again = Find-ClubByName $name
        if ($again) { return $again.id }
        throw
    }
}
$clubXId = Ensure-Club "Authz Test Club X"
$clubYId = Ensure-Club "Authz Test Club Y"
Write-Host "  Club X id=$clubXId, Club Y id=$clubYId" -ForegroundColor Green

# Give Keycloak a moment if these were just created (group provisioning is sync but
# eventual consistency in the JWT is on next login).
Start-Sleep -Milliseconds 500

# --- 2. Resolve gestor user id + /clubs/<X>/managers group id ------------

Write-Host "[2] Resolving Keycloak identifiers..." -ForegroundColor Cyan
$gestorObj = (Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/users?username=$GestorUser&exact=true" `
    -Headers @{ Authorization = "Bearer $kcAdminToken" })[0]
if (-not $gestorObj) { Write-Host "  user '$GestorUser' not found in Keycloak -- aborting." -ForegroundColor Red; exit 1 }
$gestorId = $gestorObj.id

$managersGroup = Invoke-RestMethod -Uri "$KeycloakUrl/admin/realms/$Realm/group-by-path/clubs/$clubXId/managers" `
    -Headers @{ Authorization = "Bearer $kcAdminToken" }
$managersGroupId = $managersGroup.id
Write-Host "  gestor id=$gestorId, /clubs/$clubXId/managers id=$managersGroupId" -ForegroundColor Green

# --- 3. Matrix part A: anonymous + gestor WITHOUT membership ------------

Write-Host ""
Write-Host "[3] Tests BEFORE membership is granted" -ForegroundColor Cyan

# Anonymous PATCH on a (nonexistent) team -- Spring Security must reject before reaching the handler.
$rc = Try-Request "PATCH" "$BffUrl/api/v1/teams/1" $null '{"category":"SENIOR_M"}'
Check "anonymous PATCH /teams/1 => 401" $rc @(401)

$gestorToken = Get-Token $GestorUser $GestorPass
$rc = Try-Request "POST" "$BffUrl/api/v1/clubs/$clubXId/teams" $gestorToken '{"category":"SENIOR_M"}'
Check "gestor (no group)  POST club X team => 403" $rc @(403)

# --- 4. Grant /clubs/<X>/managers to gestor + re-login -----------------

Write-Host ""
Write-Host "[4] Adding gestor to /clubs/$clubXId/managers..." -ForegroundColor Cyan
Try-Void "Put" "$KeycloakUrl/admin/realms/$Realm/users/$gestorId/groups/$managersGroupId" $kcAdminToken
Write-Host "  OK" -ForegroundColor Green

# Force a fresh token so the new group claim is in there.
$gestorToken = Get-Token $GestorUser $GestorPass

# --- 5. Matrix part B: gestor WITH membership in X -----------------------

Write-Host ""
Write-Host "[5] Tests AFTER membership is granted" -ForegroundColor Cyan

# In X: should succeed. 201 first time, 409 if a team of that category already exists from a prior run.
$rc = Try-Request "POST" "$BffUrl/api/v1/clubs/$clubXId/teams" $gestorToken '{"category":"SENIOR_M"}'
Check "gestor (in X)   POST club X team   => 201 or 409" $rc @(201, 409)

# In Y: still must be denied.
$rc = Try-Request "POST" "$BffUrl/api/v1/clubs/$clubYId/teams" $gestorToken '{"category":"SENIOR_M"}'
Check "gestor (in X)   POST club Y team   => 403" $rc @(403)

# admin always works.
$rc = Try-Request "POST" "$BffUrl/api/v1/clubs/$clubYId/teams" $adminToken '{"category":"SENIOR_M"}'
Check "admin           POST club Y team   => 201 or 409" $rc @(201, 409)

# --- 6. Cleanup ----------------------------------------------------------

Write-Host ""
Write-Host "[6] Removing gestor from /clubs/$clubXId/managers..." -ForegroundColor Cyan
Try-Void "Delete" "$KeycloakUrl/admin/realms/$Realm/users/$gestorId/groups/$managersGroupId" $kcAdminToken
Write-Host "  OK" -ForegroundColor Green

# --- Summary -------------------------------------------------------------

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Pass: $pass   Fail: $fail" -ForegroundColor $(if ($fail -gt 0) { "Red" } else { "Green" })
Write-Host "========================================" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 } else { exit 0 }