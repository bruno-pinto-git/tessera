# =============================================================================
# seed-clubs.ps1
#
# Seeds the Tessera platform with Portuguese football clubs sourced from
# Wikidata. Authenticates against Keycloak as `admin` and POSTs each club
# to the BFF's `/api/v1/clubs` endpoint.
#
# Wikidata coverage: ~607 PT football clubs total, ~14 with crest images.
# We filter by competition (default: Campeonato Nacional de Seniores, 61 clubs)
# to get a manageable, project-relevant dataset.
#
# Usage:
#   .\scripts\seed\seed-clubs.ps1                          # default settings
#   .\scripts\seed\seed-clubs.ps1 -DryRun                  # show what would be posted
#   .\scripts\seed\seed-clubs.ps1 -CompetitionQId Q754488  # Liga Portugal 2
#   .\scripts\seed\seed-clubs.ps1 -Limit 20                # cap the number imported
# =============================================================================

param(
    [string]$BffUrl = "http://localhost:8000",
    [string]$KeycloakUrl = "http://localhost:8180",
    [string]$Realm = "tessera",
    [string]$ClientId = "tessera-web",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",

    # Default: Campeonato Nacional de Seniores (Q13668768) - 61 lower-division
    # clubs that match the project's domain. Other useful values:
    #   Q754488   - Segunda Liga (Liga Portugal 2)         ~32 clubs
    #   Q182994   - Primeira Liga                          ~22 clubs
    #   Q2648473  - III Divisao (extinct)                  ~59 clubs
    #   Q618131   - II Divisao (extinct)                   ~49 clubs
    [string]$CompetitionQId = "Q13668768",

    [int]$Limit = 100,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

# Force UTF-8 everywhere - both for the console (so accents display correctly)
# and for outbound HTTP bodies (so Postgres stores them right, not mangled
# through the Windows-1252 default).
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tessera - Seed Clubs from Wikidata" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "BFF:         $BffUrl"
Write-Host "Keycloak:    $KeycloakUrl"
Write-Host "Competition: wd:$CompetitionQId"
Write-Host "Limit:       $Limit"
Write-Host "Dry run:     $DryRun"
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
# Step 2: Query Wikidata for clubs in the chosen competition.
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/3] Querying Wikidata..." -ForegroundColor Yellow

$sparqlTemplate = @'
SELECT DISTINCT ?club ?clubLabel ?founded ?crest WHERE {
  ?club wdt:P31 wd:Q476028 ;
        wdt:P17 wd:Q45 ;
        wdt:P118 wd:__COMPETITION__ .
  OPTIONAL { ?club wdt:P571 ?founded . }
  OPTIONAL { ?club wdt:P154 ?crest . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "pt,en" . }
}
ORDER BY ?clubLabel
LIMIT __LIMIT__
'@

$sparql = $sparqlTemplate.Replace("__COMPETITION__", $CompetitionQId).Replace("__LIMIT__", $Limit.ToString())

try {
    $encodedQuery = [uri]::EscapeDataString($sparql)
    $wikidata = Invoke-RestMethod `
        -Uri "https://query.wikidata.org/sparql?query=$encodedQuery&format=json" `
        -Headers @{
            "User-Agent" = "TesseraSeedScript/1.0 (academic-project; ISEL)"
            "Accept"     = "application/sparql-results+json"
        }
} catch {
    Write-Host "  Wikidata query failed: $_" -ForegroundColor Red
    exit 1
}

$clubs = @($wikidata.results.bindings)
Write-Host "  Got $($clubs.Count) clubs." -ForegroundColor Green

if ($clubs.Count -eq 0) {
    Write-Host ""
    Write-Host "No results - competition may have no clubs tagged in Wikidata." -ForegroundColor Yellow
    exit 0
}

# -----------------------------------------------------------------------------
# Step 3: POST each club to the BFF (or print on dry-run).
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "[3/3] Posting to $BffUrl/api/v1/clubs..." -ForegroundColor Yellow

$created = 0
$skipped = 0
$failed = 0

foreach ($binding in $clubs) {
    $name = $null
    if ($binding.clubLabel) {
        $name = $binding.clubLabel.value
    }

    # Skip entries whose label is just the bare Q-id (no translation in pt/en).
    if (-not $name -or $name -match '^Q\d+$') {
        continue
    }

    $foundedYear = $null
    if ($binding.founded -and $binding.founded.value) {
        $year = $binding.founded.value.Substring(0, 4)
        if ($year -match '^\d{4}$') {
            $foundedYear = [int]$year
        }
    }

    $crestUrl = $null
    if ($binding.crest -and $binding.crest.value) {
        $crestUrl = $binding.crest.value
    }

    $payload = @{
        name        = $name
        foundedYear = $foundedYear
        crestUrl    = $crestUrl
    }
    $body = $payload | ConvertTo-Json -Compress

    if ($DryRun) {
        Write-Host "  [DRY] $body"
        $created++
        continue
    }

    try {
        # Encode body as UTF-8 bytes so accented characters survive the wire.
        # PS5's default for string bodies is Windows-1252.
        $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
        $null = Invoke-RestMethod `
            -Method Post `
            -Uri "$BffUrl/api/v1/clubs" `
            -ContentType "application/json; charset=utf-8" `
            -Headers @{ "Authorization" = "Bearer $accessToken" } `
            -Body $bodyBytes
        Write-Host "  + $name" -ForegroundColor Green
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