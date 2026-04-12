@echo off
REM ---------------------------------------------------------------------------
REM stop.bat - Stop all containers (data preserved)
REM ---------------------------------------------------------------------------

cd /d "%~dp0\.."

echo === Stopping all services ===
docker compose down

echo.
echo All services stopped. Database volumes are preserved.
echo Run start.bat to start again.
echo.
pause
