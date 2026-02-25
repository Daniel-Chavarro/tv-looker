@echo off
REM ========================================
REM TV Looker - Script de Limpieza
REM Autor: TV Looker Team
REM Version: 1.0
REM ========================================

echo.
echo ===================================
echo   TV Looker - Limpieza y Detencion
echo ===================================
echo.

echo Selecciona una opcion:
echo.
echo 1. Detener la base de datos (mantener datos)
echo 2. Detener y eliminar contenedor (mantener datos)
echo 3. Limpiar todo (ELIMINAR TODOS LOS DATOS)
echo 4. Ver estado actual
echo 5. Cancelar
echo.

set /p option="Ingresa tu opcion (1-5): "

if "%option%"=="1" goto OPCION_1
if "%option%"=="2" goto OPCION_2
if "%option%"=="3" goto OPCION_3
if "%option%"=="4" goto OPCION_4
if "%option%"=="5" goto OPCION_5
goto OPCION_INVALIDA

:OPCION_1
echo.
echo Deteniendo PostgreSQL...
docker-compose stop
IF %ERRORLEVEL% EQU 0 (
    echo [OK] PostgreSQL detenido correctamente
    echo.
    echo Para reiniciar: docker-compose start
    echo O ejecuta: scripts\setup.bat
) ELSE (
    echo [ERROR] No se pudo detener PostgreSQL
)
goto FIN

:OPCION_2
echo.
echo Deteniendo y eliminando contenedor...
docker-compose down
IF %ERRORLEVEL% EQU 0 (
    echo [OK] Contenedor eliminado correctamente
    echo.
    echo NOTA: Los datos persisten en el volumen de Docker
    echo Para reiniciar: docker-compose up -d
    echo O ejecuta: scripts\setup.bat
) ELSE (
    echo [ERROR] No se pudo eliminar el contenedor
)
goto FIN

:OPCION_3
echo.
echo [ADVERTENCIA] Esto eliminara TODOS los datos de la base de datos
set /p confirm="Estas seguro? Escribe SI (en mayusculas): "
echo.
echo DEBUG: Recibido [%confirm%]
if "%confirm%"=="SI" (
    echo Confirmacion aceptada. Eliminando...
    echo.
    docker-compose down -v
    IF %ERRORLEVEL% EQU 0 (
        echo [OK] Todo eliminado correctamente
        echo.
        echo Para reiniciar desde cero: scripts\setup.bat
    ) ELSE (
        echo [ERROR] No se pudo eliminar todo
    )
) else (
    echo Operacion cancelada - Se recibio: [%confirm%]
    echo Debes escribir exactamente: SI
)
goto FIN

:OPCION_4
echo.
echo Estado actual del sistema:
echo.
echo === Contenedores Docker ===
docker ps -a | findstr postgres-tv-looker
IF %ERRORLEVEL% NEQ 0 (
    echo No se encontro el contenedor postgres-tv-looker
)
echo.
echo === Volumenes Docker ===
docker volume ls | findstr tv-looker
IF %ERRORLEVEL% NEQ 0 (
    echo No se encontraron volumenes de tv-looker
)
echo.
echo === Procesos Java ===
jps -l | findstr tvlooker
IF %ERRORLEVEL% NEQ 0 (
    echo No hay procesos Java de tv-looker corriendo
)
goto FIN

:OPCION_5
echo.
echo Operacion cancelada
goto FIN

:OPCION_INVALIDA
echo.
echo Opcion invalida - Debe ser 1, 2, 3, 4 o 5
goto FIN

:FIN

echo.
pause



