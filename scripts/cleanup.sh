#!/bin/bash

# ========================================
# TV Looker - Script de Limpieza
# Autor: TV Looker Team
# Version: 1.0
# ========================================

echo ""
echo "==================================="
echo "  TV Looker - Limpieza y Detención"
echo "==================================="
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "Selecciona una opción:"
echo ""
echo "1. Detener la base de datos (mantener datos)"
echo "2. Detener y eliminar contenedor (mantener datos)"
echo "3. Limpiar todo (ELIMINAR TODOS LOS DATOS)"
echo "4. Ver estado actual"
echo "5. Cancelar"
echo ""

read -p "Ingresa tu opción (1-5): " option

case $option in
    1)
        echo ""
        echo "Deteniendo PostgreSQL..."
        if docker-compose stop; then
            echo -e "${GREEN}[OK]${NC} PostgreSQL detenido correctamente"
            echo ""
            echo "Para reiniciar: docker-compose start"
            echo "O ejecuta: ./scripts/setup.sh"
        else
            echo -e "${RED}[ERROR]${NC} No se pudo detener PostgreSQL"
        fi
        ;;
    2)
        echo ""
        echo "Deteniendo y eliminando contenedor..."
        if docker-compose down; then
            echo -e "${GREEN}[OK]${NC} Contenedor eliminado correctamente"
            echo ""
            echo "NOTA: Los datos persisten en el volumen de Docker"
            echo "Para reiniciar: docker-compose up -d"
            echo "O ejecuta: ./scripts/setup.sh"
        else
            echo -e "${RED}[ERROR]${NC} No se pudo eliminar el contenedor"
        fi
        ;;
    3)
        echo ""
        echo -e "${YELLOW}[ADVERTENCIA]${NC} Esto eliminará TODOS los datos de la base de datos"
        read -p "¿Estás seguro? Escribe SI (en mayúsculas): " confirm
        # Eliminar espacios en blanco
        confirm=$(echo "$confirm" | xargs)
        if [[ "$confirm" == "SI" ]]; then
            echo ""
            echo "Eliminando contenedor y volúmenes..."
            if docker-compose down -v; then
                echo -e "${GREEN}[OK]${NC} Todo eliminado correctamente"
                echo ""
                echo "Para reiniciar desde cero: ./scripts/setup.sh"
            else
                echo -e "${RED}[ERROR]${NC} No se pudo eliminar todo"
            fi
        else
            echo ""
            echo "Operación cancelada - Se recibió: [$confirm]"
            echo "Debes escribir exactamente: SI"
        fi
        ;;
    4)
        echo ""
        echo "Estado actual del sistema:"
        echo ""
        echo "=== Contenedores Docker ==="
        if docker ps -a | grep postgres-tv-looker; then
            :
        else
            echo "No se encontró el contenedor postgres-tv-looker"
        fi
        echo ""
        echo "=== Volúmenes Docker ==="
        if docker volume ls | grep tv-looker; then
            :
        else
            echo "No se encontraron volúmenes de tv-looker"
        fi
        echo ""
        echo "=== Procesos Java ==="
        if command -v jps &> /dev/null; then
            if jps -l | grep tvlooker; then
                :
            else
                echo "No hay procesos Java de tv-looker corriendo"
            fi
        else
            echo "jps no está disponible"
        fi
        ;;
    5)
        echo ""
        echo "Operación cancelada"
        ;;
    *)
        echo ""
        echo -e "${RED}[ERROR]${NC} Opción inválida - Debe ser 1, 2, 3, 4 o 5"
        ;;
esac

echo ""


