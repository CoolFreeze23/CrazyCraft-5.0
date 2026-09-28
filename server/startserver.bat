@echo off
setlocal
cd /d "%~dp0"
title CrazyCraft 5.0 Server
set NEOFORGE_VERSION=21.1.248
:: CRAZYCRAFT_JAVA          full path to java.exe, if "java" is not Java 21
:: CRAZYCRAFT_RESTART=false  stop instead of restarting when the server stops or crashes
:: CRAZYCRAFT_INSTALL_ONLY=true  install or update the server, then exit
:: CRAZYCRAFT_SKIP_SETUP=true    start without checking the mods first
:: CRAZYCRAFT_ACCEPT_EULA=true   accept the Minecraft EULA (https://aka.ms/MinecraftEULA) without asking

if not defined CRAZYCRAFT_JAVA set "CRAZYCRAFT_JAVA=java"
"%CRAZYCRAFT_JAVA%" -version >nul 2>&1 || (
    echo CrazyCraft 5.0 needs Java 21, and Java was not found.
    echo Get it from https://adoptium.net/temurin/releases/?version=21
    pause
    exit /b 1
)
for /f tokens^=2-5^ delims^=.-_^" %%j in ('"%CRAZYCRAFT_JAVA%" -fullversion 2^>^&1') do set "JAVA_MAJOR=%%j"
if %JAVA_MAJOR% LSS 21 (
    echo CrazyCraft 5.0 needs Java 21 or newer, but this is Java %JAVA_MAJOR%.
    echo Get Java 21 from https://adoptium.net/temurin/releases/?version=21
    pause
    exit /b 1
)

if /i "%CRAZYCRAFT_SKIP_SETUP%"=="true" goto run
"%CRAZYCRAFT_JAVA%" -jar crazycraft-loader.jar --nogui
set SETUP=%errorlevel%
if %SETUP%==3 (
    echo.
    echo A mod still has to be downloaded by hand; see above. Then run this again.
    pause
    exit /b 3
)
if %SETUP%==2 (
    if /i "%CRAZYCRAFT_INSTALL_ONLY%"=="true" exit /b 0
    echo.
    echo The server can't start until the Minecraft EULA is accepted.
    pause
    exit /b 2
)
if not %SETUP%==0 (
    echo.
    echo Setup did not finish; see above.
    pause
    exit /b 1
)
if /i "%CRAZYCRAFT_INSTALL_ONLY%"=="true" exit /b 0

:run
echo Starting the CrazyCraft 5.0 server...
"%CRAZYCRAFT_JAVA%" @user_jvm_args.txt @libraries\net\neoforged\neoforge\%NEOFORGE_VERSION%\win_args.txt nogui
if /i "%CRAZYCRAFT_RESTART%"=="false" goto :eof
echo.
echo The server stopped. Restarting in 10 seconds (press Ctrl+C to cancel).
timeout /t 10 /nobreak >nul
goto run
