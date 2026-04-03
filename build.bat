@echo off
echo ============================
echo   Bowshot Build Script
echo ============================
echo.

call mvn clean package -DskipTests

if %ERRORLEVEL% == 0 (
    echo.
    echo [SUCCESS] Build completed!
    echo JAR: target\Bowshot-1.0-SNAPSHOT.jar
) else (
    echo.
    echo [FAILED] Build failed!
)

echo.
pause
