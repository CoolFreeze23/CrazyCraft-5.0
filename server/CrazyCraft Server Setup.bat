@echo off
cd /d "%~dp0"
:: Opens the setup window. startserver.bat does the same checks in the console before every start.
if defined CRAZYCRAFT_JAVA (
    "%CRAZYCRAFT_JAVA%" -jar crazycraft-loader.jar
    exit /b
)
where javaw >nul 2>&1
if not errorlevel 1 (
    start "" javaw -jar crazycraft-loader.jar
    exit /b 0
)
where java >nul 2>&1
if not errorlevel 1 (
    java -jar crazycraft-loader.jar
    exit /b
)
echo CrazyCraft 5.0 needs Java 21, and Java was not found.
echo Get it from https://adoptium.net/temurin/releases/?version=21
start "" "https://adoptium.net/temurin/releases/?version=21"
pause
