Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tessera - Build All Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    "buildBffService.ps1",
    "buildTicketService.ps1",
    "buildMatchService.ps1",
    "buildStatisticsService.ps1"
)

foreach ($script in $services) {
    $path = Join-Path $PSScriptRoot $script
    & $path
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build of $script failed. Aborting buildAll." -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Green
Write-Host "All services built successfully." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
