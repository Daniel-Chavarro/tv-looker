# 🚀 Guía de Inicio Rápido - TV Looker

Esta es una guía ultra-rápida para comenzar con el proyecto en menos de 5 minutos.

## ⚡ Setup Automático (Recomendado)

### En Windows:
```cmd
scripts/setup.bat
```

### En Linux/Mac:
```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

El script verificará automáticamente:
- ✅ Java 21+
- ✅ Maven 3.8+
- ✅ Docker
- ✅ Iniciará PostgreSQL
- ✅ Descargará dependencias

## 🎯 Setup Manual (3 Pasos)

### 1️⃣ Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/tv-looker.git
cd tv-looker
```

### 2️⃣ Iniciar la base de datos
```bash
docker-compose up -d
```

### 3️⃣ Ejecutar la aplicación
```bash
mvn spring-boot:run
```

🎉 **¡Listo!** La aplicación estará en: http://localhost:8080

## 🛑 Detener Todo

### Detener solo la base de datos:
```bash
docker-compose stop
```

### Detener y limpiar (mantener datos):
```bash
docker-compose down
```

### Limpiar TODO (⚠️ elimina datos):
```bash
docker-compose down -v
```

### O usar el script de limpieza:
```bash
# Windows
scripts/cleanup.bat

# Linux/Mac
./scripts/cleanup.sh
```

## 🧪 Ejecutar Tests

```bash
# Todos los tests
mvn test

# Solo tests de persistencia
mvn test -Dtest=ReviewPersistenceTest
```

## 📊 Verificar que Todo Funciona

### 1. Verificar PostgreSQL:
```bash
docker ps
# Debes ver: postgres-tv-looker
```

### 2. Verificar la aplicación:
```bash
curl http://localhost:8080/actuator/health
# Respuesta: {"status":"UP"}
```

### 3. Conectarse a la base de datos:
```bash
docker exec -it postgres-tv-looker psql -U tv-looker-admin -d tv-looker-db
```

## 🔑 Credenciales de Base de Datos

| Campo | Valor |
|-------|-------|
| Host | localhost |
| Puerto | 5432 |
| Base de Datos | tv-looker-db |
| Usuario | tv-looker-admin |
| Password | this-is-a-super-secure-password123 |

## 📝 Comandos Útiles

```bash
# Ver logs de la aplicación
mvn spring-boot:run

# Ver logs de PostgreSQL
docker-compose logs -f postgres

# Compilar sin ejecutar tests
mvn clean install -DskipTests

# Ejecutar solo la compilación
mvn clean compile

# Ver estado de Docker
docker ps -a

# Acceder al contenedor de PostgreSQL
docker exec -it postgres-tv-looker bash
```

## 🆘 Problemas Comunes

### Puerto 5432 ocupado:
```bash
# Windows
netstat -ano | findstr :5432

# Linux/Mac
lsof -i :5432
```

### Docker no responde:
1. Reinicia Docker Desktop
2. Verifica recursos en Settings → Resources
3. Limpia contenedores viejos: `docker system prune`

### Maven no descarga dependencias:
```bash
# Limpiar cache de Maven
mvn dependency:purge-local-repository

# Forzar actualización
mvn clean install -U
```

## 📚 Más Información

- **README completo:** [README.md](README.md)
- **Documentación de tests:** [docs/JDBC_PERSISTENCE_TESTS.md](docs/JDBC_PERSISTENCE_TESTS.md)
- **Licencia:** [LICENSE](LICENSE)

---

💡 **Tip:** Si es tu primera vez, ejecuta `scripts\setup.bat` (Windows) o `./scripts/setup.sh` (Linux/Mac) para configurar todo automáticamente.


