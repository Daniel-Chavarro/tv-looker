@echo off
REM ========================================
REM TV Looker - Script de Setup Inicial
REM Autor: TV Looker Team
REM Version: 1.0
REM ========================================

SETLOCAL EnableDelayedExpansion

echo.
echo ===================================
echo   TV Looker - Setup Inicial
echo ===================================
echo.

SET ERROR_COUNT=0

REM Verificar Java
echo [1/5] Verificando Java...
java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java no esta instalado o no esta en el PATH
    echo Por favor instala Java 21 o superior: https://adoptium.net/
    SET /A ERROR_COUNT+=1
) ELSE (
    echo [OK] Java encontrado
    java -version 2>&1 | findstr /C:"version"
)
echo.

REM Verificar Maven
echo [2/5] Verificando Maven...
call mvn -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven no esta instalado o no esta en el PATH
    echo Por favor instala Maven: https://maven.apache.org/download.cgi
    SET /A ERROR_COUNT+=1
) ELSE (
    echo [OK] Maven encontrado
    call mvn -version 2>&1 | findstr /C:"Apache Maven"
)
echo.

REM Verificar Docker
echo [3/5] Verificando Docker...
docker --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker no esta instalado o no esta en el PATH
    echo Por favor instala Docker Desktop: https://www.docker.com/products/docker-desktop/
    SET /A ERROR_COUNT+=1
) ELSE (
    echo [OK] Docker encontrado
    docker --version
)
echo.

REM Si hay errores, detener
IF %ERROR_COUNT% GTR 0 (
    echo.
    echo [ERROR] Se encontraron %ERROR_COUNT% errores. Por favor corrige los prerequisitos.
    echo.
    pause
    exit /b 1
)

REM Verificar que Docker Desktop este corriendo
echo Verificando que Docker Desktop este corriendo...
docker info >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ADVERTENCIA] Docker Desktop no esta corriendo
    echo Por favor inicia Docker Desktop y ejecuta este script nuevamente
    echo.
    pause
    exit /b 1
)
echo [OK] Docker Desktop esta corriendo
echo.

REM Iniciar PostgreSQL con Docker Compose
echo [4/5] Iniciando base de datos PostgreSQL...
docker-compose up -d 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No se pudo iniciar Docker Compose
    echo Verifica los logs arriba para mas detalles
    echo.
    pause
    exit /b 1
)
echo [OK] PostgreSQL iniciado correctamente
echo.

REM Esperar a que PostgreSQL este listo
echo Esperando a que PostgreSQL este listo...
timeout /t 5 /nobreak >nul
echo.

REM Verificar que PostgreSQL esta corriendo
echo Verificando contenedor de PostgreSQL...
docker ps | findstr postgres-tv-looker >nul
IF %ERRORLEVEL% NEQ 0 (
    echo [ADVERTENCIA] El contenedor postgres-tv-looker no se encuentra
    echo Ejecuta: docker-compose logs postgres
) ELSE (
    echo [OK] Contenedor postgres-tv-looker esta corriendo
)
echo.

REM Descargar dependencias
echo [5/5] Descargando dependencias de Maven...
call mvn dependency:resolve -q
IF %ERRORLEVEL% NEQ 0 (
    echo [ADVERTENCIA] Hubo problemas descargando algunas dependencias
    echo Esto es normal, se resolveran al compilar el proyecto
) ELSE (
    echo [OK] Dependencias descargadas
)
echo.

echo ===================================
echo   Setup completado exitosamente!
echo ===================================
echo.
echo Proximos pasos:
echo   1. Ejecutar: mvn spring-boot:run
echo   2. Abrir: http://localhost:8080
echo.
echo Comandos utiles:
echo   - Ver contenedores: docker ps
echo   - Ver logs DB: docker-compose logs postgres
echo   - Detener DB: docker-compose stop
echo   - Compilar proyecto: mvn clean install
echo   - Ejecutar tests: mvn test
echo.
echo Para limpiar todo, ejecuta: scripts\cleanup.bat
echo.
pause

