# =============================================================================
# seed-liga3.ps1
#
# Seeds the Tessera platform with the 2024-25 Liga 3 (Portuguese third tier)
# clubs. Authenticates against Keycloak as `admin` and POSTs each club to the
# BFF's `/api/v1/clubs` endpoint.
#
# Why a curated list of Wikidata QIds instead of a competition filter (like
# seed-clubs.ps1): Liga 3 is recent (2021) and poorly modelled in Wikidata -
# only ~3 clubs are reachable via the competition property (P118) and the
# season entities carry no participating-team statements. So the 20-team roster
# below is curated by hand from the season's standings. We pin each club by its
# stable QId and resolve the display name (pt label), founding year (P571) and
# crest (P154) from Wikidata at run time, so accents are always correct.
#
# Usage:
#   .\scripts\seed\seed-liga3.ps1                 # default settings
#   .\scripts\seed\seed-liga3.ps1 -DryRun         # show what would be posted
#   .\scripts\seed\seed-liga3.ps1 -Limit 5        # cap the number imported
# =============================================================================

param(
    [string]$BffUrl = "http://localhost:8000",
    [string]$KeycloakUrl = "http://localhost:8180",
    [string]$Realm = "tessera",
    [string]$ClientId = "tessera-web",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",

    [int]$Limit = 100,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

# Force UTF-8 everywhere - both for the console (so accents display correctly)
# and for outbound HTTP bodies (so Postgres stores them right, not mangled
# through the Windows-1252 default).
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# 2024-25 Liga 3 roster (20 clubs), curated from the season standings.
# `Qid` pins the club; `Name` is an ASCII fallback used only if the Wikidata
# label lookup fails (the real, accented name comes from Wikidata at run time).
$Liga3Teams = @(
    @{ Qid = "Q757422";   Name = "Atletico Clube de Portugal" },
    @{ Qid = "Q2841205";  Name = "Amarante FC" },
    @{ Qid = "Q2845081";  Name = "Anadia FC" },
    @{ Qid = "Q243235";   Name = "Associacao Academica de Coimbra - O.A.F." },
    @{ Qid = "Q290860";   Name = "AD Fafe" },
    @{ Qid = "Q2868240";  Name = "AD Sanjoanense" },
    @{ Qid = "Q2933726";  Name = "Caldas SC" },
    @{ Qid = "Q216510";   Name = "CF Os Belenenses" },
    @{ Qid = "Q1023233";  Name = "CD Trofense" },
    @{ Qid = "Q3091245";  Name = "FC Oliveira do Hospital" },
    @{ Qid = "Q6705180";  Name = "Lusitania de Lourosa FC" },
    @{ Qid = "Q16240044"; Name = "SU 1 Dezembro" },
    @{ Qid = "Q953331";   Name = "SC Lusitania (Angra)" },
    @{ Qid = "Q1891367";  Name = "SC Covilha" },
    @{ Qid = "Q7387158";  Name = "SC Braga B" },
    @{ Qid = "Q3494112";  Name = "Sporting CP B" },
    @{ Qid = "Q10373917"; Name = "SC Sao Joao de Ver" },
    @{ Qid = "Q3551888";  Name = "Uniao Desportiva de Santarem" },
    @{ Qid = "Q1429473";  Name = "Varzim SC" },
    @{ Qid = "Q7930110";  Name = "Vilaverdense FC" }
) | Select-Object -First $Limit

$wdHeaders = @{
    "User-Agent" = "TesseraSeedScript/1.0 (academic-project; ISEL)"
    "Accept"     = "application/json"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tessera - Seed Liga 3 (2024-25)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "BFF:       $BffUrl"
Write-Host "Keycloak:  $KeycloakUrl"
Write-Host "Teams:     $($Liga3Teams.Count)"
Write-Host "Dry run:   $DryRun"
Write-Host ""

# -----------------------------------------------------------------------------
# Fetch a club's display name, founding year and crest URL from Wikidata, by
# QId. Returns @{ name; foundedYear; crestUrl } - any value may be $null.
# Retries on HTTP 429 (Wikidata rate-limits bursts of anonymous requests).
# -----------------------------------------------------------------------------
function Resolve-WikidataClub {
    param([string]$Qid)

    $result = @{ name = $null; foundedYear = $null; crestUrl = $null }
    $uri = "https://www.wikidata.org/wiki/Special:EntityData/$Qid.json"

    $entity = $null
    for ($attempt = 1; $attempt -le 4; $attempt++) {
        try {
            $entity = Invoke-RestMethod -Headers $wdHeaders -Uri $uri
            break
        } catch {
            $status = 0
            if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
            if ($status -eq 429 -and $attempt -lt 4) {
                Start-Sleep -Seconds ($attempt * 2)
                continue
            }
            return $result
        }
    }
    if (-not $entity) { return $result }

    $e = $entity.entities.$Qid

    # Display name: prefer the Portuguese label, fall back to English.
    if ($e.labels.pt) {
        $result.name = $e.labels.pt.value
    } elseif ($e.labels.en) {
        $result.name = $e.labels.en.value
    }

    $claims = $e.claims

    # P571 = inception / founding date -> take the year.
    if ($claims.P571) {
        $time = $claims.P571[0].mainsnak.datavalue.value.time
        if ($time -and $time -match '\+(\d{4})-') {
            $result.foundedYear = [int]$Matches[1]
        }
    }

    # P154 = logo image (Commons filename) -> build a Special:FilePath URL.
    if ($claims.P154) {
        $file = $claims.P154[0].mainsnak.datavalue.value
        if ($file) {
            $result.crestUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/" +
                [uri]::EscapeDataString($file)
        }
    }

    return $result
}

# -----------------------------------------------------------------------------
# Step 1: Get an admin access token from Keycloak.
# -----------------------------------------------------------------------------
$accessToken = $null
if (-not $DryRun) {
    Write-Host "[1/2] Authenticating as $AdminUser..." -ForegroundColor Yellow
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
    Write-Host "[1/2] Skipping authentication (dry run)." -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# Step 2: For each club, enrich from Wikidata then POST to the BFF.
# -----------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/2] Posting to $BffUrl/api/v1/clubs..." -ForegroundColor Yellow

$created = 0
$skipped = 0
$failed = 0

foreach ($team in $Liga3Teams) {
    $wd = Resolve-WikidataClub -Qid $team.Qid

    $name = $team.Name
    if ($wd.name) { $name = $wd.name }

    $payload = @{
        name        = $name
        foundedYear = $wd.foundedYear
        crestUrl    = $wd.crestUrl
    }
    $body = $payload | ConvertTo-Json -Compress

    # Be polite to Wikidata - small gap between entity lookups.
    Start-Sleep -Milliseconds 300

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
