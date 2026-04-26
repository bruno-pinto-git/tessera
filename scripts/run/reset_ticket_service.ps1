$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "Stopping and removing ticket-service container..." -ForegroundColor Yellow
docker compose -f $composeFile stop ticket-service
docker compose -f $composeFile rm -f ticket-service

Write-Host "Building ticket-service JAR..." -ForegroundColor Yellow
$buildScript = Join-Path $PSScriptRoot "..\build\buildTicketService.ps1"
& $buildScript

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting." -ForegroundColor Red
    exit 1
}

Write-Host "Rebuilding ticket-service image..." -ForegroundColor Yellow
docker compose -f $composeFile build ticket-service

Write-Host "Starting ticket-service container..." -ForegroundColor Yellow
docker compose -f $composeFile up -d ticket-service

Write-Host "Restarting NGINX..." -ForegroundColor Yellow
docker compose -f $composeFile restart nginx

Write-Host "Restart complete. Logs (Ctrl+C to stop following):" -ForegroundColor Green
docker compose -f $composeFile logs -f ticket-service
