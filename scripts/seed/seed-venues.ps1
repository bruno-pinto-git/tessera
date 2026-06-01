# =============================================================================
# seed-venues.ps1
#
# Seeds the Tessera platform with the home stadiums of the 2024-25 Liga 3
# clubs. Authenticates against Keycloak as `admin` and POSTs each venue to the
# match-service's `/api/v1/venues` endpoint (proxied through the BFF).
#
# Data comes from Wikidata: for each curated club QId we follow P115 (home
# venue) to its stadium entity and read the name (label), capacity (P1083) and
# location (P131 -> city), which map onto the venue model's name / capacity /
# address fields. A single SPARQL query fetches everything in one round-trip.
#
# Only ~11 of the 20 Liga 3 clubs have a home venue tagged in Wikidata; the
# rest are skipped. There is no Club<->Venue relationship in the schema, so
# this only populates the venues table (matches reference venues via venueId).
#
# Usage:
#   .\scripts\seed\seed-venues.ps1                # default settings
#   .\scripts\seed\seed-venues.ps1 -DryRun        # show what would be posted
# =============================================================================

param(
    [string]$BffUrl = "http://localhost:8000",
    [string]$KeycloakUrl = "http://localhost:8180",
    [string]$Realm = "tessera",
    [string]$ClientId = "tessera-web",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

# Force UTF-8 everywhere - both for the console (so accents display correctly)
# and for outbound HTTP bodies (so Postgres stores them right, not mangled
# through the Windows-1252 default).
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# 2024-25 Liga 3 roster (20 club QIds). Same list as seed-liga3.ps1.
$ClubQIds = @(
    "Q757422", "Q2841205", "Q2845081", "Q243235", "Q290860",
    "Q2868240", "Q2933726", "Q216510", "Q1023233", "Q3091245",
    "Q6705180", "Q16240044", "Q953331", "Q1891367", "Q7387158",
    "Q3494112", "Q10373917", "Q3551888", "Q1429473", "Q7930110"
)

$wdHeaders = @{
    "User-Agent" = "TesseraSeedScript/1.0 (academic-project; ISEL)"
    "Accept"     = "application/sparql-results+json"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tessera - Seed Liga 3 Venues" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "BFF:       $BffUrl"
Write-Host "Keycloak:  $KeycloakUrl"
Write-Host "Clubs:     $($ClubQIds.Count)"
Write-Host "Dry run:   $DryRun"
Write-Host ""

# -----------------------------------------------------------------------------
# Step 1: Get an admin access token from Keycloak.
# -----------------------------------------------------------------------------
$accessToken = $null
if (-not $DryRun) {
    Write-Host "[1/3] Authenticating as $AdminUser..." -ForegroundColor Yellow
    try {
        $tokenResponse = Invoke-RestMethod `
            -Method Post `
            -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" `
            -ContentType "application/x-www-form-urlencoded" `
            -Body @{
                grant_type = "password"
                client_id  = $ClientId
                username   = $AdminUser
                password   = $AdminPassword
            }
        $accessToken = $tokenResponse.access_token
        Write-Host "  OK (token expires in $($tokenResponse.expires_in)s)" -ForegroundColor Green
    } catch {
        Write-Host "  Failed to authenticate: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[1/3] Skipping authentication (dry run)." -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# Step 2: One SPARQL query for all clubs -> home venue, capacity, location.
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/3] Querying Wikidata for home venues..." -ForegroundColor Yellow

$values = ($ClubQIds | ForEach-Object { "wd:$_" }) -join " "
$sparql = @"
SELECT ?venue ?venueLabel ?cap ?locLabel WHERE {
  VALUES ?club { $values }
  ?club wdt:P115 ?venue .
  OPTIONAL { ?venue wdt:P1083 ?cap . }
  OPTIONAL { ?venue wdt:P131 ?loc . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "pt,en" . }
}
"@

try {
    $encoded = [uri]::EscapeDataString($sparql)
    $results = Invoke-RestMethod -Headers $wdHeaders `
        -Uri "https://query.wikidata.org/sparql?format=json&query=$encoded"
} catch {
    Write-Host "  Wikidata query failed: $_" -ForegroundColor Red
    exit 1
}

$bindings = @($results.results.bindings)
Write-Host "  Got $($bindings.Count) venue rows." -ForegroundColor Green

# -----------------------------------------------------------------------------
# Step 3: POST each venue to the BFF (dedup by venue QId in case two clubs
# share a stadium).
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "[3/3] Posting to $BffUrl/api/v1/venues..." -ForegroundColor Yellow

$created = 0
$skipped = 0
$failed = 0
$seen = @{}

foreach ($b in $bindings) {
    $venueQid = $b.venue.value
    if ($seen.ContainsKey($venueQid)) { continue }
    $seen[$venueQid] = $true

    $name = $null
    if ($b.venueLabel) { $name = $b.venueLabel.value }

    # Skip rows whose label is just the bare Q-id (no translation).
    if (-not $name -or $name -match '^Q\d+$') { continue }

    # Capacity is required by the model; default to 0 when Wikidata has none.
    $capacity = 0
    if ($b.cap -and $b.cap.value -match '^\d+$') { $capacity = [int]$b.cap.value }

    $address = $null
    if ($b.locLabel -and $b.locLabel.value -and $b.locLabel.value -notmatch '^Q\d+$') {
        $address = $b.locLabel.value
    }

    $payload = @{
        name     = $name
        capacity = $capacity
        address  = $address
    }
    $body = $payload | ConvertTo-Json -Compress

    if ($DryRun) {
        Write-Host "  [DRY] $body"
        $created++
        continue
    }

    try {
        # Encode body as UTF-8 bytes so accented characters survive the wire.
        $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
        $null = Invoke-RestMethod `
            -Method Post `
            -Uri "$BffUrl/api/v1/venues" `
            -ContentType "application/json; charset=utf-8" `
            -Headers @{ "Authorization" = "Bearer $accessToken" } `
            -Body $bodyBytes
        Write-Host "  + $name ($capacity)" -ForegroundColor Green
        $created++
    } catch {
        $status = 0
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        if ($status -eq 409) {
            Write-Host "  = $name (already exists)" -ForegroundColor DarkGray
            $skipped++
        } else {
            Write-Host "  ! $name : $($_.Exception.Message)" -ForegroundColor Red
            $failed++
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Created: $created" -ForegroundColor Green
Write-Host "Skipped: $skipped" -ForegroundColor DarkGray
$failedColor = "Green"
if ($failed -gt 0) { $failedColor = "Red" }
Write-Host "Failed:  $failed" -ForegroundColor $failedColor

if ($failed -gt 0) { exit 1 } else { exit 0 }
