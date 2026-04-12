@echo off
REM ---------------------------------------------------------------------------
REM reset-databases.bat - Wipe all database volumes and restart fresh
REM ---------------------------------------------------------------------------

cd /d "%~dp0\.."

echo === Stopping all services ===
docker compose down

echo.
echo === Removing database volumes ===
docker volume rm -f tessera_db-tickets-data 2>nul
docker volume rm -f tessera_db-matches-data 2>nul
docker volume rm -f tessera_db-statistics-data 2>nul

echo.
echo === Restarting everything with clean databases ===
docker compose up --build -d
if errorlevel 1 (
    echo.
    echo BUILD FAILED! Check the output above.
    pause
    exit /b 1
)

echo.
echo === Restarting NGINX ===
docker compose restart nginx

echo.
echo Databases have been reset. Flyway will re-run all migrations on startup.
echo.
pause
