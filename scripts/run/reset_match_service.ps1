$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "Stopping and removing match-service container..." -ForegroundColor Yellow
docker compose -f $composeFile stop match-service
docker compose -f $composeFile rm -f match-service

Write-Host "Building match-service JAR..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "..\build\buildMatchService.ps1"
& $buildScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting." -ForegroundColor Red
    exit 1
}

Write-Host "Rebuilding match-service image..." -ForegroundColor Yellow
docker compose -f $composeFile build match-service

Write-Host "Starting match-service container..." -ForegroundColor Yellow
docker compose -f $composeFile up -d match-service

Write-Host "Restarting NGINX..." -ForegroundColor Yellow
docker compose -f $composeFile restart nginx

Write-Host "Restart complete. Logs (Ctrl+C to stop following):" -ForegroundColor Green
docker compose -f $composeFile logs -f match-service
