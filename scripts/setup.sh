#!/bin/bash

# ========================================
# TV Looker - Script de Setup Inicial
# Autor: TV Looker Team
# Version: 1.0
# ========================================

set -e  # Salir si hay errores

echo ""
echo "==================================="
echo "  TV Looker - Setup Inicial"
echo "==================================="
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERROR_COUNT=0

# Verificar Java
echo -e "${BLUE}[1/5] Verificando Java...${NC}"
if command -v java &> /dev/null; then
    echo -e "${GREEN}[OK]${NC} Java encontrado"
    java -version 2>&1 | head -n 1
else
    echo -e "${RED}[ERROR]${NC} Java no está instalado o no está en el PATH"
    echo "Por favor instala Java 21 o superior: https://adoptium.net/"
    ((ERROR_COUNT++))
fi
echo ""

# Verificar Maven
echo -e "${BLUE}[2/5] Verificando Maven...${NC}"
if command -v mvn &> /dev/null; then
    echo -e "${GREEN}[OK]${NC} Maven encontrado"
    mvn -version 2>&1 | head -n 1
else
    echo -e "${RED}[ERROR]${NC} Maven no está instalado o no está en el PATH"
    echo "Por favor instala Maven: https://maven.apache.org/download.cgi"
    ((ERROR_COUNT++))
fi
echo ""

# Verificar Docker
echo -e "${BLUE}[3/5] Verificando Docker...${NC}"
if command -v docker &> /dev/null; then
    echo -e "${GREEN}[OK]${NC} Docker encontrado"
    docker --version
else
    echo -e "${RED}[ERROR]${NC} Docker no está instalado o no está en el PATH"
    echo "Por favor instala Docker: https://www.docker.com/products/docker-desktop/"
    ((ERROR_COUNT++))
fi
echo ""

# Si hay errores, detener
if [ $ERROR_COUNT -gt 0 ]; then
    echo ""
    echo -e "${RED}[ERROR]${NC} Se encontraron $ERROR_COUNT errores. Por favor corrige los prerequisitos."
    echo ""
    exit 1
fi

# Verificar que Docker esté corriendo
echo "Verificando que Docker esté corriendo..."
if docker info &> /dev/null; then
    echo -e "${GREEN}[OK]${NC} Docker está corriendo"
else
    echo -e "${RED}[ERROR]${NC} Docker no está corriendo"
    echo "Por favor inicia Docker Desktop y ejecuta este script nuevamente"
    echo ""
    exit 1
fi
echo ""

# Iniciar PostgreSQL con Docker Compose
echo -e "${BLUE}[4/5] Iniciando base de datos PostgreSQL...${NC}"
if docker-compose up -d; then
    echo -e "${GREEN}[OK]${NC} PostgreSQL iniciado correctamente"
else
    echo -e "${RED}[ERROR]${NC} No se pudo iniciar Docker Compose"
    echo "Verifica los logs arriba para más detalles"
    echo ""
    exit 1
fi
echo ""

# Esperar a que PostgreSQL esté listo
echo "Esperando a que PostgreSQL esté listo..."
sleep 5
echo ""

# Verificar que PostgreSQL está corriendo
echo "Verificando contenedor de PostgreSQL..."
if docker ps | grep -q postgres-tv-looker; then
    echo -e "${GREEN}[OK]${NC} Contenedor postgres-tv-looker está corriendo"
else
    echo -e "${YELLOW}[ADVERTENCIA]${NC} El contenedor postgres-tv-looker no se encuentra"
    echo "Ejecuta: docker-compose logs postgres"
fi
echo ""

# Descargar dependencias
echo -e "${BLUE}[5/5] Descargando dependencias de Maven...${NC}"
if mvn dependency:resolve -q; then
    echo -e "${GREEN}[OK]${NC} Dependencias descargadas"
else
    echo -e "${YELLOW}[ADVERTENCIA]${NC} Hubo problemas descargando algunas dependencias"
    echo "Esto es normal, se resolverán al compilar el proyecto"
fi
echo ""

echo "==================================="
echo "  Setup completado exitosamente!"
echo "==================================="
echo ""
echo "Próximos pasos:"
echo "  1. Ejecutar: mvn spring-boot:run"
echo "  2. Abrir: http://localhost:8080"
echo ""
echo "Comandos útiles:"
echo "  - Ver contenedores: docker ps"
echo "  - Ver logs DB: docker-compose logs postgres"
echo "  - Detener DB: docker-compose stop"
echo "  - Compilar proyecto: mvn clean install"
echo "  - Ejecutar tests: mvn test"
echo ""
echo "Para limpiar todo, ejecuta: ./scripts/cleanup.sh"
echo ""

