$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tessera - Start" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build all JARs
Write-Host "Building all services..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "..\build\buildAll.ps1"
& $buildScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting." -ForegroundColor Red
    exit 1
}

# Step 2: Docker compose up
Write-Host ""
Write-Host "Starting Docker containers..." -ForegroundColor Yellow
docker compose -f $composeFile up -d --build

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "All services are up!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  App:       http://localhost:8000" -ForegroundColor White
    Write-Host "  BFF API:   http://localhost:8080/api" -ForegroundColor White
    Write-Host "  Keycloak:  http://localhost:8180" -ForegroundColor White
    Write-Host ""
    docker compose -f $composeFile ps
} else {
    Write-Host "Docker compose failed!" -ForegroundColor Red
    exit 1
}
