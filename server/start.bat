@echo off
setlocal enabledelayedexpansion
title CrazyCraft 5.0 Server

REM ============ SETTINGS ============
REM RAM for the server. 6G minimum, 8G recommended.
set RAM=6G
set NEOFORGE=21.1.248
REM ==================================

cd /d "%~dp0"

REM --- check java 21 ---
java -version >nul 2>&1
if errorlevel 1 (
    echo [!] Java was not found. Install Java 21 from https://adoptium.net/temurin/releases/?version=21
    pause
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JVER=%%v
set JVER=%JVER:"=%
for /f "delims=. tokens=1" %%m in ("%JVER%") do set JMAJOR=%%m
if %JMAJOR% LSS 21 (
    echo [!] Java %JVER% found, but this server needs Java 21.
    echo     Install it from https://adoptium.net/temurin/releases/?version=21
    pause
    exit /b 1
)

REM --- first-run: install neoforge ---
if not exist "libraries\net\neoforged\neoforge\%NEOFORGE%\win_args.txt" (
    echo [*] First run - installing NeoForge %NEOFORGE% ^(needs internet, takes a minute^)...
    java -jar neoforge-%NEOFORGE%-installer.jar --install-server
    if errorlevel 1 (
        echo [!] NeoForge install failed. Check your internet connection and retry.
        pause
        exit /b 1
    )
)

REM --- eula ---
findstr /c:"eula=true" eula.txt >nul 2>&1
if errorlevel 1 (
    echo.
    echo  To run a Minecraft server you must accept the Minecraft EULA:
    echo  https://aka.ms/MinecraftEULA
    echo.
    set /p ACCEPT="Do you accept the EULA? (yes/no): "
    if /i "!ACCEPT!"=="yes" (
        echo eula=true> eula.txt
    ) else (
        echo [!] EULA not accepted - can't start the server.
        pause
        exit /b 1
    )
)

REM --- run ---
echo [*] Starting CrazyCraft 5.0 server with %RAM% RAM...
java -Xms2G -Xmx%RAM% -XX:+UseG1GC -XX:MaxGCPauseMillis=130 @libraries/net/neoforged/neoforge/%NEOFORGE%/win_args.txt nogui %*

echo.
echo Server stopped.
pause
