$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "Stopping and removing bff-service container..." -ForegroundColor Yellow
docker compose -f $composeFile stop bff-service
docker compose -f $composeFile rm -f bff-service

Write-Host "Building bff-service JAR..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "..\build\buildBffService.ps1"
& $buildScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting." -ForegroundColor Red
    exit 1
}

Write-Host "Rebuilding bff-service image..." -ForegroundColor Yellow
docker compose -f $composeFile build bff-service

Write-Host "Starting bff-service container..." -ForegroundColor Yellow
docker compose -f $composeFile up -d bff-service

Write-Host "Restarting NGINX..." -ForegroundColor Yellow
docker compose -f $composeFile restart nginx

Write-Host "Restart complete. Logs (Ctrl+C to stop following):" -ForegroundColor Green
docker compose -f $composeFile logs -f bff-service
