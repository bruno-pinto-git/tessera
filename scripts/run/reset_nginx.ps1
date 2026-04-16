$composeFile = Join-Path $PSScriptRoot "..\..\docker-compose.yml"

Write-Host "Stopping and removing nginx container..." -ForegroundColor Yellow
docker compose -f $composeFile stop nginx
docker compose -f $composeFile rm -f nginx

Write-Host "Rebuilding nginx image (includes frontend build)..." -ForegroundColor Yellow
docker compose -f $composeFile build nginx

Write-Host "Starting nginx container..." -ForegroundColor Yellow
docker compose -f $composeFile up -d nginx

Write-Host "Restart complete. Logs (Ctrl+C to stop following):" -ForegroundColor Green
docker compose -f $composeFile logs -f nginx
