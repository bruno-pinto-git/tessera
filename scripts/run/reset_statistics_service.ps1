$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "Stopping and removing statistics-service container..." -ForegroundColor Yellow
docker compose -f $composeFile stop statistics-service
docker compose -f $composeFile rm -f statistics-service

Write-Host "Building statistics-service JAR..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "..\build\buildStatisticsService.ps1"
& $buildScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting." -ForegroundColor Red
    exit 1
}

Write-Host "Rebuilding statistics-service image..." -ForegroundColor Yellow
docker compose -f $composeFile build statistics-service

Write-Host "Starting statistics-service container..." -ForegroundColor Yellow
docker compose -f $composeFile up -d statistics-service

Write-Host "Restarting NGINX..." -ForegroundColor Yellow
docker compose -f $composeFile restart nginx

Write-Host "Restart complete. Logs (Ctrl+C to stop following):" -ForegroundColor Green
docker compose -f $composeFile logs -f statistics-service
