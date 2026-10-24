@echo off
cd /d "%~dp0"

set "PY=python"
where python >nul 2>nul || set "PY=py"

start "PROTEC - Site (3000)" cmd /k "cd /d site && %PY% -m http.server 3000"

timeout /t 4 /nobreak >nul
start "" "http://localhost:3000"