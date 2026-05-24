$serviceDir = Join-Path $PSScriptRoot "..\..\backend\bff-service"

Write-Host "Building bff-service JAR..." -ForegroundColor Yellow

Push-Location $serviceDir
try {
    & .\gradlew.bat clean bootJar
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exit -ne 0) {
    Write-Host "bff-service build failed (exit $exit)." -ForegroundColor Red
    exit $exit
}

Write-Host "bff-service JAR built successfully." -ForegroundColor Green
