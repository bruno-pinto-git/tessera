# =============================================================================
# demo-network.ps1
#
# Prepara a rede para a demo: detecta o IP do PC e o IP do telemovel-hotspot
# (gateway), escreve o .env com as URLs do MB WAY, e mostra o IP que deves
# introduzir no POS (MainMenu -> Configuracoes).
#
# Topologia assumida:
#   - O teu telemovel faz hotspot E corre a app mock-mbway  -> IP = gateway
#   - O PC corre o backend (Docker)                          -> IP = IPv4 local
#   - O POS (apG31) valida bilhetes                          -> aponta para o PC
#
# Uso:
#   .\scripts\run\demo-network.ps1            # detecta automaticamente
#   .\scripts\run\demo-network.ps1 -Up        # detecta e faz docker compose up -d
#   .\scripts\run\demo-network.ps1 -PcIp 192.168.43.10 -MockIp 192.168.43.1
# =============================================================================

param(
    [string]$PcIp,
    [string]$MockIp,
    [switch]$Up
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$envPath = Join-Path $root '.env'

function Get-HotspotInterface {
    # Interfaces Up com gateway IPv4 definido; prefere Wi-Fi.
    $candidates = Get-NetIPConfiguration | Where-Object {
        $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up'
    }
    if (-not $candidates) { return $null }
    $wifi = $candidates | Where-Object { $_.InterfaceAlias -match 'Wi-Fi|Wireless|WLAN' } | Select-Object -First 1
    if ($wifi) { return $wifi }
    return ($candidates | Select-Object -First 1)
}

# --- Detecta IPs se nao foram passados manualmente ---
if (-not $PcIp -or -not $MockIp) {
    $iface = Get-HotspotInterface
    if (-not $iface) {
        Write-Host "Nao consegui detectar a interface de rede. Passa os IPs a mao:" -ForegroundColor Red
        Write-Host "  .\scripts\run\demo-network.ps1 -PcIp <ip-pc> -MockIp <ip-telemovel>" -ForegroundColor Yellow
        exit 1
    }
    if (-not $PcIp)   { $PcIp   = $iface.IPv4Address.IPAddress }
    if (-not $MockIp) { $MockIp = $iface.IPv4DefaultGateway.NextHop }
    Write-Host "Interface detectada: $($iface.InterfaceAlias)" -ForegroundColor DarkGray
}

# --- Escreve o .env (UTF8 sem BOM) ---
$content = @"
MBWAY_GATEWAY_URL=http://${MockIp}:8443
MBWAY_WEBHOOK_BASE_URL=http://${PcIp}:8081
MBWAY_TERMINAL_ID=47215
MBWAY_CLIENT_ID=tessera-mock
"@
[System.IO.File]::WriteAllText($envPath, $content, (New-Object System.Text.UTF8Encoding $false))

Write-Host ""
Write-Host "=================================================================" -ForegroundColor Green
Write-Host " .env escrito em: $envPath" -ForegroundColor Green
Write-Host "-----------------------------------------------------------------"
Write-Host " IP do PC      (mete no POS -> Configuracoes) : $PcIp" -ForegroundColor Cyan
Write-Host " IP do mock    (telemovel hotspot, gateway)  : $MockIp" -ForegroundColor Cyan
Write-Host "-----------------------------------------------------------------"
Write-Host " MBWAY_GATEWAY_URL      = http://${MockIp}:8443"
Write-Host " MBWAY_WEBHOOK_BASE_URL = http://${PcIp}:8081"
Write-Host "=================================================================" -ForegroundColor Green
Write-Host ""

if ($Up) {
    Write-Host "A recriar a stack (docker compose up -d)..." -ForegroundColor Yellow
    Push-Location $root
    try {
        docker compose up -d
    } finally {
        Pop-Location
    }
} else {
    Write-Host "Para aplicar:  docker compose up -d" -ForegroundColor Yellow
}
