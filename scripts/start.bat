@echo off
REM ---------------------------------------------------------------------------
REM start.bat - Build and start all containers
REM ---------------------------------------------------------------------------

cd /d "%~dp0\.."

echo === Building and starting all services ===
docker compose up --build -d
if errorlevel 1 (
    echo.
    echo BUILD FAILED! Check the output above.
    pause
    exit /b 1
)

echo.
echo === Service status ===
docker compose ps

echo.
echo All services are up!
echo   - App:       http://localhost
echo   - BFF API:   http://localhost:8080/api
echo   - Keycloak:  http://localhost:8180
echo.
echo Logs: docker compose logs -f
echo.
pause
