@echo off
REM ---------------------------------------------------------------------------
REM reload-services.bat - Rebuild JARs, recreate service containers, restart NGINX
REM
REM Usage:
REM   reload-services.bat                              (reload ALL services)
REM   reload-services.bat ticket-service               (reload one)
REM   reload-services.bat match-service bff-service    (reload specific ones)
REM ---------------------------------------------------------------------------

cd /d "%~dp0\.."

if "%~1"=="" (
    echo === Rebuilding ALL services ===
    set SERVICES=bff-service ticket-service match-service statistics-service
) else (
    echo === Rebuilding: %* ===
    set SERVICES=%*
)

echo.
echo === Building new images (fresh JARs) ===
docker compose build --no-cache %SERVICES%
if errorlevel 1 (
    echo.
    echo BUILD FAILED! Check the output above.
    pause
    exit /b 1
)

echo.
echo === Recreating containers ===
docker compose up -d --force-recreate %SERVICES%

echo.
echo === Restarting NGINX ===
docker compose restart nginx

echo.
echo === Current status ===
docker compose ps

echo.
echo Done! Services reloaded.
echo.
pause
