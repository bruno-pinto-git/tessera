$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "Stopping all services..." -ForegroundColor Yellow
docker compose -f $composeFile down

Write-Host ""
Write-Host "All services stopped. Database volumes are preserved." -ForegroundColor Green
Write-Host "Run start.ps1 to start again." -ForegroundColor White
