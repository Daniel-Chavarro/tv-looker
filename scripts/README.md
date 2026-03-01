# 📜 Scripts de TV Looker

Esta carpeta contiene scripts de utilidad para facilitar el setup, gestión y limpieza del proyecto TV Looker.

## 📁 Scripts Disponibles

### 🚀 Setup Scripts

#### `setup.bat` (Windows)
Script para configurar automáticamente el entorno de desarrollo en Windows.

**Uso:**
```cmd
scripts\setup.bat
```

**Funcionalidades:**
- ✅ Verifica prerequisitos (Java, Maven, Docker)
- ✅ Comprueba que Docker Desktop esté corriendo
- ✅ Inicia PostgreSQL con Docker Compose
- ✅ Descarga dependencias de Maven
- ✅ Muestra comandos útiles para comenzar

#### `setup.sh` (Linux/Mac)
Script equivalente para sistemas Unix/Linux/Mac.

**Uso:**
```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

**Funcionalidades:**
- ✅ Igual que setup.bat pero con sintaxis Unix
- ✅ Colores en la terminal para mejor visualización
- ✅ Validación de errores y rollback automático

---

### 🧹 Cleanup Scripts

#### `cleanup.bat` (Windows)
Script interactivo para detener y limpiar el entorno.

**Uso:**
```cmd
scripts\cleanup.bat
```

**Opciones:**
1. **Detener la base de datos** - Detiene PostgreSQL pero mantiene los datos
2. **Detener y eliminar contenedor** - Elimina el contenedor pero mantiene datos en volúmenes
3. **Limpiar todo** - ⚠️ ELIMINA TODO incluyendo datos (requiere confirmación)
4. **Ver estado actual** - Muestra el estado de contenedores, volúmenes y procesos
5. **Cancelar** - Sale sin hacer cambios

#### `cleanup.sh` (Linux/Mac)
Script equivalente para sistemas Unix/Linux/Mac.

**Uso:**
```bash
./scripts/cleanup.sh
```

**Opciones:**
- Iguales que cleanup.bat
- Interface con colores para mejor experiencia

---

## 🎯 Casos de Uso Comunes

### Primer Setup del Proyecto

```bash
# Windows
scripts\setup.bat

# Linux/Mac
./scripts/setup.sh
```

### Reiniciar Todo Desde Cero

```bash
# 1. Limpiar todo
# Windows: scripts\cleanup.bat → Opción 3
# Linux/Mac: ./scripts/cleanup.sh → Opción 3

# 2. Volver a configurar
# Windows: scripts\setup.bat
# Linux/Mac: ./scripts/setup.sh
```

### Solo Detener la Base de Datos

```bash
# Windows
scripts\cleanup.bat
# → Seleccionar opción 1

# Linux/Mac
./scripts/cleanup.sh
# → Seleccionar opción 1
```

### Ver Estado del Sistema

```bash
# Windows
scripts\cleanup.bat
# → Seleccionar opción 4

# Linux/Mac
./scripts/cleanup.sh
# → Seleccionar opción 4
```

---

## 🔍 Qué Verifica cada Script

### Setup Script Verifica:

| Componente | Verificación |
|------------|--------------|
| **Java** | Instalado y en PATH |
| **Maven** | Instalado y en PATH |
| **Docker** | Instalado y en PATH |
| **Docker Desktop** | Corriendo activamente |
| **PostgreSQL** | Contenedor iniciado correctamente |
| **Dependencias** | Maven puede descargarlas |

### Cleanup Script Muestra:

| Información | Descripción |
|-------------|-------------|
| **Contenedores** | Estado de postgres-tv-looker |
| **Volúmenes** | Volúmenes de Docker relacionados |
| **Procesos Java** | Instancias de la aplicación corriendo |

---

## 🛡️ Seguridad

### Confirmación Requerida

La opción "Limpiar todo" (opción 3) requiere que escribas **"SI"** (en mayúsculas) para confirmar la operación. Esto previene eliminación accidental de datos.

### Validación de Errores

Ambos scripts validan cada paso y muestran mensajes claros de error si algo falla, permitiéndote identificar y solucionar problemas rápidamente.

---

## 🐛 Troubleshooting

### Problema: "Script no se puede ejecutar"

**Windows:**
```cmd
# Asegúrate de estar en el directorio correcto
cd C:\ruta\a\tv-looker
scripts\setup.bat
```

**Linux/Mac:**
```bash
# Dar permisos de ejecución
chmod +x scripts/setup.sh
chmod +x scripts/cleanup.sh

# Ejecutar
./scripts/setup.sh
```

### Problema: "Docker no está corriendo"

**Solución:**
1. Abre Docker Desktop
2. Espera a que se inicie completamente
3. Ejecuta el script nuevamente

### Problema: "Maven no descarga dependencias"

**Solución:**
```bash
# Limpiar cache de Maven y reintentar
mvn dependency:purge-local-repository
mvn clean install -U
```

---

## 📋 Prerequisitos

Antes de usar estos scripts, asegúrate de tener instalado:

- ✅ Java 21+
- ✅ Maven 3.8+
- ✅ Docker Desktop
- ✅ Git (opcional)

Para más información sobre los prerequisitos, consulta el [README principal](../README.md#-prerequisitos).

---

## 🔄 Flujo de Trabajo Recomendado

### Día a Día

```bash
# 1. Iniciar el entorno (primera vez o después de reiniciar PC)
scripts\setup.bat  # Windows
./scripts/setup.sh  # Linux/Mac

# 2. Desarrollar tu código...

# 3. Al terminar, detener la BD (opcional, mantiene datos)
scripts\cleanup.bat → Opción 1  # Windows
./scripts/cleanup.sh → Opción 1  # Linux/Mac
```

### Testing

```bash
# Los tests usan H2 en memoria, no afectan PostgreSQL
mvn test

# No necesitas detener PostgreSQL para ejecutar tests
```

### Limpieza Completa

```bash
# Solo cuando necesites resetear todo
scripts\cleanup.bat → Opción 3  # Windows
./scripts/cleanup.sh → Opción 3  # Linux/Mac
```

---

## 📚 Documentación Relacionada

- [README Principal](../README.md) - Documentación completa del proyecto
- [Guía de Inicio Rápido](../QUICKSTART.md) - Setup en 5 minutos
- [Checklist de Verificación](../CHECKLIST.md) - Verificar que todo funciona
- [Pruebas de Persistencia](../docs/JDBC_PERSISTENCE_TESTS.md) - Documentación de tests

---

## 🤝 Contribuir

Si mejoras estos scripts o encuentras bugs, por favor:
1. Reporta el issue en GitHub
2. Crea un Pull Request con la mejora
3. Documenta los cambios en este README

---

**Autor:** TV Looker Team  
**Versión:** 1.0  
**Última actualización:** 2026-02-25

