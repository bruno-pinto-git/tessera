$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tessera - Full Reset" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Stopping containers and removing volumes..." -ForegroundColor Yellow
docker compose -f $composeFile down -v

Write-Host "Removing local images and orphan containers..." -ForegroundColor Yellow
docker compose -f $composeFile down -v --rmi local --remove-orphans

# Build all JARs
Write-Host ""
Write-Host "Building all services..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "..\build\buildAll.ps1"
& $buildScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Recreating containers..." -ForegroundColor Yellow
docker compose -f $composeFile up -d --build

Write-Host ""
Write-Host "Full reset complete. All databases are fresh." -ForegroundColor Green
docker compose -f $composeFile ps
