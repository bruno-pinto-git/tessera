# =============================================================================
# seed-demo.ps1
#
# Prepares a full, realistic dataset for the live demo so nothing has to be
# built from scratch on stage. Authenticates as `admin` and drives the public
# API (through the BFF), exactly like the other seed-*.ps1 scripts, so it can
# be pointed at Azure by overriding -BffUrl / -KeycloakUrl / -AdminPassword.
#
# What it creates:
#   - 2 stadiums, 2 clubs (SU 1o de Dezembro, Sport Uniao Sintrense)
#   - SU 1o de Dezembro:   Senior Masculina (+18 players)
#   - Sport Uniao Sintrense: Senior Masculina (+18 players), Sub-11, Sub-17
#   - 2 FINISHED matches (full squads + line-ups + goals + closed sheet),
#     back-dated to the past, box office PUBLISHED (sales are auto-blocked
#     because the match is finished, so it shows in "Terminados" with the score)
#   - 2 upcoming matches (full squads + line-ups), kickoff today+2 / today+3,
#     box office PUBLISHED (open)
#   - 1 demo match, kickoff today+7, full line-ups only (no box office) so the
#     match-sheet fill-in can be demonstrated live
#   - PAID ticket sales for BOTH clubs' home matches (so Sintrense is also a
#     "Tessera client"): 1o Dezembro 32 tickets (15 validated), Sintrense 34
#     tickets (13 validated). Lets the admin stats show everything while the
#     1o Dezembro manager sees only their own club.
#
# Payments are settled headless by acting as the mock-mbway poller: each ticket
# is paid by MB WAY (queues a relay request), then we drain /api/v1/mbway/relay/poll
# and confirm each via the /api/v1/webhooks/mbway callback. Finished-match tickets
# are then validated as admin (admin bypasses the staff/time-window checks).
#
# All dates are relative to the day the script runs.
#
# Usage:
#   .\scripts\seed\seed-demo.ps1
#   .\scripts\seed\seed-demo.ps1 -BffUrl https://tessera.swedencentral.cloudapp.azure.com `
#                                -KeycloakUrl https://tessera.swedencentral.cloudapp.azure.com/auth `
#                                -AdminPassword '<prod-admin-password>' `
#                                -MbwayUrl https://tessera.swedencentral.cloudapp.azure.com `
#                                -RelaySecret '<MBWAY_RELAY_SECRET-from-VM-.env>'
#
# NOTE: run against a CLEAN environment (matches/line-ups are not idempotent).
# NOTE: MB WAY relay/webhook bypass the BFF. Locally they live on the ticket-service
#       directly (http://localhost:8081, default). On Azure, Caddy routes them under
#       the public FQDN, so pass -MbwayUrl <FQDN> and the VM's -RelaySecret.
# =============================================================================

param(
    [string]$BffUrl = "http://localhost:8000",
    [string]$KeycloakUrl = "http://localhost:8180",
    [string]$Realm = "tessera",
    [string]$ClientId = "tessera-web",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin",
    # MB WAY mock endpoints (relay poll + webhook) bypass the BFF. Local: the
    # ticket-service directly on :8081. Azure: the public FQDN (Caddy routes it).
    [string]$MbwayUrl = "http://localhost:8081",
    [string]$RelaySecret = "dev-secret"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tessera - Seed DEMO dataset" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BFF:       $BffUrl"
Write-Host "Keycloak:  $KeycloakUrl"
Write-Host ""

# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------
Write-Host "[1] Authenticating as $AdminUser..." -ForegroundColor Yellow
$accessToken = (Invoke-RestMethod -Method Post `
    -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{ grant_type = "password"; client_id = $ClientId; username = $AdminUser; password = $AdminPassword }).access_token
Write-Host "  OK" -ForegroundColor Green

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
function StatusOf($err) { if ($err.Exception.Response) { return [int]$err.Exception.Response.StatusCode } return 0 }

function Api($method, $path, $body = $null) {
    $headers = @{ Authorization = "Bearer $accessToken" }
    if ($null -ne $body) {
        $json = $body | ConvertTo-Json -Compress -Depth 8
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        return Invoke-RestMethod -Method $method -Uri "$BffUrl$path" `
            -ContentType "application/json; charset=utf-8" -Headers $headers -Body $bytes
    }
    return Invoke-RestMethod -Method $method -Uri "$BffUrl$path" -Headers $headers
}

function IsoUtc($dt) { return $dt.ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ") }

function GetOrCreateVenue($name, $cap, $addr) {
    try {
        $v = Api POST "/api/v1/venues" @{ name = $name; capacity = $cap; address = $addr }
        Write-Host "  + estadio: $name" -ForegroundColor Green; return $v
    } catch {
        if ((StatusOf $_) -eq 409) {
            $page = Api GET "/api/v1/venues?size=200"
            $f = $page.content | Where-Object { $_.name -eq $name } | Select-Object -First 1
            if ($f) { Write-Host "  = estadio: $name (existe)" -ForegroundColor DarkGray; return $f }
        }
        throw
    }
}

function GetOrCreateClub($name, $founded) {
    try {
        $c = Api POST "/api/v1/clubs" @{ name = $name; foundedYear = $founded }
        Write-Host "  + clube: $name" -ForegroundColor Green; return $c
    } catch {
        if ((StatusOf $_) -eq 409) {
            $page = Api GET ("/api/v1/clubs?name=" + [uri]::EscapeDataString($name))
            $f = $page.content | Where-Object { $_.name -eq $name } | Select-Object -First 1
            if ($f) { Write-Host "  = clube: $name (existe)" -ForegroundColor DarkGray; return $f }
        }
        throw
    }
}

function CreateTeam($clubId, $category) { return Api POST "/api/v1/clubs/$clubId/teams" @{ category = $category } }

function SeedSquad($teamId, $names) {
    $players = New-Object System.Collections.ArrayList
    for ($i = 0; $i -lt $names.Count; $i++) {
        $parts = $names[$i] -split " ", 2
        $pos = if ($i -lt 2) { "GK" } elseif ($i -lt 8) { "DF" } elseif ($i -lt 14) { "MF" } else { "FW" }
        $p = Api POST "/api/v1/teams/$teamId/players" `
            @{ firstName = $parts[0]; lastName = $parts[1]; position = $pos; shirtNumber = ($i + 1); nationality = "PRT" }
        [void]$players.Add($p)
    }
    Write-Host "    convocados $($names.Count) jogadores (equipa $teamId)" -ForegroundColor DarkGray
    return $players
}

function CreateMatch($homeTeamId, $awayTeamId, $venueId, $kickoffIso, $referee) {
    return Api POST "/api/v1/matches" `
        @{ homeTeamId = $homeTeamId; awayTeamId = $awayTeamId; venueId = $venueId; kickoffAt = $kickoffIso; refereeName = $referee }
}

# Convocatoria: 11 starters + the rest as substitutes.
function FillLineup($matchId, $players) {
    for ($i = 0; $i -lt $players.Count; $i++) {
        $role = if ($i -lt 11) { "STARTER" } else { "SUBSTITUTE" }
        [void](Api POST "/api/v1/matches/$matchId/sheet/lineup" @{ playerId = $players[$i].id; role = $role })
    }
}

function AddGoal($matchId, $player, $minute) {
    [void](Api POST "/api/v1/matches/$matchId/sheet/occurrences" @{ minute = $minute; type = "GOAL"; playerId = $player.id })
}

function CloseSheet($matchId) { [void](Api POST "/api/v1/matches/$matchId/sheet/lock") }
function SetKickoff($matchId, $iso) { [void](Api PATCH "/api/v1/matches/$matchId" @{ kickoffAt = $iso }) }
function BoxOffice($matchId, $label, $status) {
    return Api POST "/api/v1/events" @{ matchId = $matchId; name = $label; priceNormal = 8.0; priceSupporter = 5.0; status = $status }
}

# --- Ticket sales (headless MB WAY) ----------------------------------------
# The relay/webhook endpoints bypass the BFF, so they use $MbwayUrl (not $BffUrl).
function MbwayApi($method, $path, $body = $null, $headers = @{}) {
    if ($null -ne $body) {
        $json = $body | ConvertTo-Json -Compress -Depth 8
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        return Invoke-RestMethod -Method $method -Uri "$MbwayUrl$path" `
            -ContentType "application/json; charset=utf-8" -Headers $headers -Body $bytes
    }
    return Invoke-RestMethod -Method $method -Uri "$MbwayUrl$path" -Headers $headers
}

# Create a PENDING ticket and start an MB WAY payment (confirmed later in bulk).
function BuyTicket($eventId, $supporter) {
    $t = Api POST "/api/v1/tickets" @{ eventId = $eventId; supporter = $supporter }
    [void](Api POST "/api/v1/tickets/$($t.id)/pay" @{ paymentMethod = "MBWAY"; phoneNumber = "351912345678" })
    return $t
}

# Sell $count tickets for an event (~1 in 3 as "socio"). Returns the created tickets.
function SellTickets($eventId, $count) {
    $sold = New-Object System.Collections.ArrayList
    for ($i = 0; $i -lt $count; $i++) {
        [void]$sold.Add((BuyTicket $eventId (($i % 3) -eq 0)))
    }
    return , $sold.ToArray()
}

# Act as mock-mbway: drain the relay queue and confirm every pending payment.
# NOTE: capture the poll result into a variable BEFORE wrapping with @().
# Windows PowerShell 5.1 does NOT unroll the array returned straight from
# Invoke-RestMethod, so `@(MbwayApi ...)` yields a single element holding the
# whole array and `$r.transactionID` becomes a LIST of ids -> the webhook then
# gets {"transactionID":[...]} and rejects it with 400. Capturing to $raw first
# and doing @($raw) enumerates correctly on both 5.1 and PowerShell 7.
function ConfirmAllMbway() {
    $total = 0
    while ($true) {
        $raw = MbwayApi POST "/api/v1/mbway/relay/poll" $null @{ "X-Relay-Secret" = $RelaySecret }
        $reqs = @($raw)
        if ($reqs.Count -eq 0) { break }
        foreach ($r in $reqs) {
            $txn = "$($r.transactionID)"
            if ([string]::IsNullOrWhiteSpace($txn)) { continue }
            [void](MbwayApi POST "/api/v1/webhooks/mbway" @{ transactionID = $txn; paymentStatus = "SUCCESS" })
            $total++
        }
    }
    return $total
}

# Validate the first $n tickets (admin bypasses the staff/time-window checks).
function ValidateSome($tickets, $n) {
    $count = [Math]::Min($n, $tickets.Count)
    for ($i = 0; $i -lt $count; $i++) {
        [void](Api POST "/api/v1/tickets/validate" @{ code = $tickets[$i].code })
    }
    return $count
}

# ---------------------------------------------------------------------------
# Names
# ---------------------------------------------------------------------------
$dezNames = @(
    "Joao Silva", "Miguel Costa", "Andre Ferreira", "Ruben Santos", "Tiago Oliveira", "Pedro Martins",
    "Diogo Sousa", "Bruno Rocha", "Ricardo Pereira", "Nuno Gomes", "Hugo Carvalho", "Fabio Lopes",
    "Luis Almeida", "Rafael Marques", "Goncalo Ribeiro", "Daniel Cardoso", "Vasco Pinto", "Simao Correia"
)
$sinNames = @(
    "Carlos Mendes", "Filipe Nunes", "Jose Antunes", "Marco Teixeira", "Paulo Freitas", "Sergio Barbosa",
    "Tomas Moreira", "Andre Cunha", "Miguel Faria", "Rui Barros", "Duarte Neves", "Ivo Ramos",
    "Henrique Matos", "Gabriel Fonseca", "Leandro Reis", "Afonso Castro", "Rodrigo Lima", "Nelson Vaz"
)

# ---------------------------------------------------------------------------
# Dates (relative to today)
# ---------------------------------------------------------------------------
$today = (Get-Date).Date
$dCompleted1 = IsoUtc $today.AddDays(-14).AddHours(18)   # 2 weeks ago
$dCompleted2 = IsoUtc $today.AddDays(-7).AddHours(18)    # 1 week ago
$dTmpFuture  = IsoUtc $today.AddDays(1).AddHours(18)     # placeholder to pass the "future kickoff" rule
$dUpcoming1  = IsoUtc $today.AddDays(2).AddHours(18)     # 2 days after presentation
$dUpcoming2  = IsoUtc $today.AddDays(3).AddHours(18)
$dDemo       = IsoUtc $today.AddDays(7).AddHours(18)     # 1 week after demo

# ---------------------------------------------------------------------------
# Stadiums & clubs
# ---------------------------------------------------------------------------
Write-Host ""; Write-Host "[2] Estadios & clubes..." -ForegroundColor Yellow
$vDez = GetOrCreateVenue "Estadio Municipal de Sintra" 3000 "Sintra"
$vSin = GetOrCreateVenue "Campo da Portela" 1500 "Sintra"
$dez = GetOrCreateClub "SU 1 de Dezembro" 1953
$sin = GetOrCreateClub "Sport Uniao Sintrense" 1910

# ---------------------------------------------------------------------------
# Teams & squads
# ---------------------------------------------------------------------------
Write-Host ""; Write-Host "[3] Equipas & jogadores..." -ForegroundColor Yellow
$dezSenior = CreateTeam $dez.id "SENIOR_M"
$sinSenior = CreateTeam $sin.id "SENIOR_M"
$null = CreateTeam $sin.id "SUB_11"
$null = CreateTeam $sin.id "SUB_17"
Write-Host "  + SU 1 de Dezembro: Senior Masculina" -ForegroundColor Green
Write-Host "  + Sport Uniao Sintrense: Senior Masculina, Sub-11, Sub-17" -ForegroundColor Green
$dezPlayers = SeedSquad $dezSenior.id $dezNames
$sinPlayers = SeedSquad $sinSenior.id $sinNames

# ---------------------------------------------------------------------------
# Matches
# ---------------------------------------------------------------------------
Write-Host ""; Write-Host "[4] Jogos..." -ForegroundColor Yellow

# --- Completed #1: 1 Dezembro (casa) 2-1 Sintrense, ha 2 semanas ---
$m1 = CreateMatch $dezSenior.id $sinSenior.id $vDez.id $dTmpFuture "Andre Marques"
$e1 = BoxOffice $m1.id "1 de Dezembro vs Sintrense" "PUBLISHED"
$dezSold1 = SellTickets $e1.id 20   # vendidos enquanto o jogo ainda e futuro
FillLineup $m1.id $dezPlayers
FillLineup $m1.id $sinPlayers
AddGoal $m1.id $dezPlayers[14] 23
AddGoal $m1.id $dezPlayers[15] 67
AddGoal $m1.id $sinPlayers[16] 81
CloseSheet $m1.id
SetKickoff $m1.id $dCompleted1
Write-Host "  + concluido: 1 de Dezembro 2-1 Sintrense (ficha fechada, 20 bilhetes vendidos)" -ForegroundColor Green

# --- Completed #2: Sintrense (casa) 1-1 1 Dezembro, ha 1 semana ---
$m2 = CreateMatch $sinSenior.id $dezSenior.id $vSin.id $dTmpFuture "Luis Godinho"
$e2 = BoxOffice $m2.id "Sintrense vs 1 de Dezembro" "PUBLISHED"
$sinSold2 = SellTickets $e2.id 18
FillLineup $m2.id $sinPlayers
FillLineup $m2.id $dezPlayers
AddGoal $m2.id $sinPlayers[14] 30
AddGoal $m2.id $dezPlayers[15] 55
CloseSheet $m2.id
SetKickoff $m2.id $dCompleted2
Write-Host "  + concluido: Sintrense 1-1 1 de Dezembro (ficha fechada, 18 bilhetes vendidos)" -ForegroundColor Green

# --- Upcoming #1: 1 Dezembro (casa) vs Sintrense, hoje+2, bilheteira aberta ---
$m3 = CreateMatch $dezSenior.id $sinSenior.id $vDez.id $dUpcoming1 "Fabio Veríssimo"
$e3 = BoxOffice $m3.id "1 de Dezembro vs Sintrense" "PUBLISHED"
$null = SellTickets $e3.id 12
FillLineup $m3.id $dezPlayers
FillLineup $m3.id $sinPlayers
Write-Host "  + por jogar: 1 de Dezembro vs Sintrense (hoje+2, bilheteira aberta, 12 bilhetes vendidos)" -ForegroundColor Green

# --- Upcoming #2: Sintrense (casa) vs 1 Dezembro, hoje+3, bilheteira aberta ---
$m4 = CreateMatch $sinSenior.id $dezSenior.id $vSin.id $dUpcoming2 "Joao Pinheiro"
$e4 = BoxOffice $m4.id "Sintrense vs 1 de Dezembro" "PUBLISHED"
$null = SellTickets $e4.id 16
FillLineup $m4.id $sinPlayers
FillLineup $m4.id $dezPlayers
Write-Host "  + por jogar: Sintrense vs 1 de Dezembro (hoje+3, bilheteira aberta, 16 bilhetes vendidos)" -ForegroundColor Green

# --- Demo match: 1 Dezembro (casa) vs Sintrense, hoje+7, so convocatoria ---
$m5 = CreateMatch $dezSenior.id $sinSenior.id $vDez.id $dDemo "Artur Soares Dias"
FillLineup $m5.id $dezPlayers
FillLineup $m5.id $sinPlayers
Write-Host "  + DEMO: 1 de Dezembro vs Sintrense (hoje+7, convocatoria feita, sem bilheteira)" -ForegroundColor Green

# ---------------------------------------------------------------------------
# Bilhetes: confirmar pagamentos MB WAY (em bloco) & validar jogos concluidos
# ---------------------------------------------------------------------------
Write-Host ""; Write-Host "[5] Bilhetes: confirmar pagamentos MB WAY & validar..." -ForegroundColor Yellow
$paid = ConfirmAllMbway
Write-Host "  MB WAY confirmados -> PAID: $paid bilhetes" -ForegroundColor Green
$v1 = ValidateSome $dezSold1 15
$v2 = ValidateSome $sinSold2 13
Write-Host "  validados (jogos concluidos): $v1 no 1 de Dezembro + $v2 no Sintrense" -ForegroundColor Green

# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Concluido" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Clubes:  $($dez.id) SU 1 de Dezembro | $($sin.id) Sport Uniao Sintrense"
Write-Host "Vendas:  1 Dezembro 32 bilhetes (15 validados) | Sintrense 34 bilhetes (13 validados)"
Write-Host "         -> admin ve o total dos dois clubes; gestor do 1 Dezembro ve so o seu."
Write-Host "Jogo de demo (ficha por preencher ao vivo): matchId $($m5.id)  ->  /matches/$($m5.id)/sheet"
Write-Host ""
