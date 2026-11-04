@echo off
setlocal
cd /d "%~dp0"
echo ==============================================
echo   One For All - Minecraft 1.20.1 Mod Builder
echo ==============================================
echo.
powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-mod.ps1"
if errorlevel 1 (
  echo.
  echo Build failed. Read the error above, then check README.md.
  pause
  exit /b 1
)
echo.
echo Build finished. Your mod JAR is in the build\libs folder.
pause
