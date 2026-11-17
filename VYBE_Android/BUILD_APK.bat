@echo off
setlocal
cd /d "%~dp0"
title VYBE APK Builder
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0BUILD_APK.ps1"
set "RESULT=%ERRORLEVEL%"
echo.
if "%RESULT%"=="0" (echo APK build finished successfully.) else (echo APK build stopped. Read the message above.)
pause
exit /b %RESULT%
