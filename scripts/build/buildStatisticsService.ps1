$serviceDir = Join-Path $PSScriptRoot "..\..\backend\statistics-service"

Write-Host "Building statistics-service JAR..." -ForegroundColor Yellow

Push-Location $serviceDir
try {
    & .\gradlew.bat clean bootJar
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exit -ne 0) {
    Write-Host "statistics-service build failed (exit $exit)." -ForegroundColor Red
    exit $exit
}

Write-Host "statistics-service JAR built successfully." -ForegroundColor Green
