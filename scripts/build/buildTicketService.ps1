$serviceDir = Join-Path $PSScriptRoot "..\..\backend\ticket-service"

Write-Host "Building ticket-service JAR..." -ForegroundColor Yellow

Push-Location $serviceDir
try {
    & .\gradlew.bat clean bootJar
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exit -ne 0) {
    Write-Host "ticket-service build failed (exit $exit)." -ForegroundColor Red
    exit $exit
}

Write-Host "ticket-service JAR built successfully." -ForegroundColor Green
