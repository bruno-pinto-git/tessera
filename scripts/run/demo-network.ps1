# =============================================================================
# demo-network.ps1
#
# Prepara a rede para a demo: detecta o IP do PC, atualiza no .env so as
# variaveis do MB WAY (preservando tudo o resto - Stripe, Google Wallet,
# etc.), e mostra o IP que deves introduzir no POS (MainMenu -> Configuracoes)
# e nas Definicoes da app mock-mbway.
#
# Topologia assumida:
#   - O PC corre o backend (Docker)                          -> IP = IPv4 local
#   - O teu telemovel corre a app mock-mbway                  -> faz *polling*
#     ao PC (nunca o contrario) - so precisa de saber o IP do PC.
#   - O POS (apG31) valida bilhetes                          -> aponta para o PC
#
# Uso:
#   .\scripts\run\demo-network.ps1            # detecta automaticamente
#   .\scripts\run\demo-network.ps1 -Up        # detecta e faz docker compose up -d
#   .\scripts\run\demo-network.ps1 -PcIp 192.168.43.10
# =============================================================================

param(
    [string]$PcIp,
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

# --- Detecta o IP do PC se nao foi passado manualmente ---
if (-not $PcIp) {
    $iface = Get-HotspotInterface
    if (-not $iface) {
        Write-Host "Nao consegui detectar a interface de rede. Passa o IP a mao:" -ForegroundColor Red
        Write-Host "  .\scripts\run\demo-network.ps1 -PcIp <ip-pc>" -ForegroundColor Yellow
        exit 1
    }
    $PcIp = $iface.IPv4Address.IPAddress
    Write-Host "Interface detectada: $($iface.InterfaceAlias)" -ForegroundColor DarkGray
}

# --- Atualiza so as variaveis do MB WAY no .env, preservando o resto ---
# (Stripe, Google Wallet, etc. ficam intactos - o script so mexe nestas linhas.)
$mbwayVars = [ordered]@{
    MBWAY_WEBHOOK_BASE_URL = "http://${PcIp}:8081"
    MBWAY_TERMINAL_ID      = "47215"
}

$lines = [System.Collections.Generic.List[string]]::new()
if (Test-Path $envPath) {
    [System.IO.File]::ReadAllLines($envPath, [System.Text.Encoding]::UTF8) | ForEach-Object { $lines.Add($_) }
}

foreach ($key in $mbwayVars.Keys) {
    $newLine = "$key=$($mbwayVars[$key])"
    $idx = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^$key=") { $idx = $i; break }
    }
    if ($idx -ge 0) { $lines[$idx] = $newLine } else { $lines.Add($newLine) }
}

# Junta sempre com \n e garante uma quebra de linha final, para nunca colar
# a proxima variavel que outra ferramenta venha a acrescentar por baixo.
$content = ($lines -join "`n") + "`n"
[System.IO.File]::WriteAllText($envPath, $content, (New-Object System.Text.UTF8Encoding $false))

Write-Host ""
Write-Host "=================================================================" -ForegroundColor Green
Write-Host " .env escrito em: $envPath" -ForegroundColor Green
Write-Host "-----------------------------------------------------------------"
Write-Host " IP do PC — introduz isto em DOIS sítios:" -ForegroundColor Cyan
Write-Host "   1) POS (apG31)          -> MainMenu -> Configuracoes" -ForegroundColor Cyan
Write-Host "   2) App mock-mbway       -> Definicoes" -ForegroundColor Cyan
Write-Host "   IP: $PcIp" -ForegroundColor Cyan
Write-Host "-----------------------------------------------------------------"
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
