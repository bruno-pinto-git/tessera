$serviceDir = Join-Path $PSScriptRoot "..\..\backend\match-service"

Write-Host "Building match-service JAR..." -ForegroundColor Yellow

Push-Location $serviceDir
try {
    & .\gradlew.bat clean bootJar
    $exit = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exit -ne 0) {
    Write-Host "match-service build failed (exit $exit)." -ForegroundColor Red
    exit $exit
}

Write-Host "match-service JAR built successfully." -ForegroundColor Green
