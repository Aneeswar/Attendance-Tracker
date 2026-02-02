@echo off
REM Docker Helper Scripts for Attendance App (Windows)

if "%1"=="" (
    echo Attendance App - Docker Management
    echo ====================================
    echo.
    echo Available commands:
    echo   docker-helper.bat up        - Start containers (build if needed)
    echo   docker-helper.bat down      - Stop and remove containers
    echo   docker-helper.bat logs      - View application logs
    echo   docker-helper.bat logs-db   - View database logs
    echo   docker-helper.bat rebuild   - Rebuild Docker image
    echo   docker-helper.bat clean     - Remove containers, volumes, and images
    echo   docker-helper.bat shell     - Open psql shell to database
    echo.
    exit /b 0
)

if "%1"=="up" (
    echo Starting containers...
    docker-compose up -d
    echo.
    echo Containers started
    echo App: http://localhost:8080
    echo Database: localhost:5432
    exit /b 0
)

if "%1"=="down" (
    echo Stopping containers...
    docker-compose down
    echo Containers stopped
    exit /b 0
)

if "%1"=="logs" (
    echo Application logs:
    docker-compose logs -f app
    exit /b 0
)

if "%1"=="logs-db" (
    echo Database logs:
    docker-compose logs -f postgres
    exit /b 0
)

if "%1"=="rebuild" (
    echo Rebuilding Docker image...
    docker-compose build --no-cache
    echo Image rebuilt
    exit /b 0
)

if "%1"=="clean" (
    echo Cleaning up Docker resources...
    docker-compose down -v
    docker rmi attendance-app:latest
    echo Cleanup complete
    exit /b 0
)

if "%1"=="shell" (
    echo Opening psql shell...
    docker-compose exec postgres psql -U postgres -d attendance_db
    exit /b 0
)

echo Usage: docker-helper.bat {up^|down^|logs^|logs-db^|rebuild^|clean^|shell}
exit /b 1
