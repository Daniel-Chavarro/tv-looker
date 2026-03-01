# TV Looker

Final proyect of FIS (Fundaments of Software Engineering)

> 🚀 **¿Primera vez aquí?** Consulta la [Guía de Inicio Rápido](QUICKSTART.md) para comenzar en menos de 5 minutos.

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Prerequisitos](#-prerequisitos)
- [Instalación](#-instalación)
- [Configuración de Base de Datos](#-configuración-de-base-de-datos)
- [Ejecutar el Proyecto](#-ejecutar-el-proyecto)
- [Tecnologías](#-tecnologías)
- [Testing](#-testing)

## 📖 Descripción

TV Looker es una aplicación web para gestionar y revisar películas y series de televisión. Permite a los usuarios:
- Buscar películas y series
- Crear y gestionar listas de favoritos
- Escribir y leer reviews
- Calificar contenido multimedia

## 🔧 Prerequisitos

Antes de comenzar, asegúrate de tener instalado lo siguiente:

### Requerimientos Obligatorios

- **Java 21** o superior
  - [Descargar OpenJDK](https://adoptium.net/)
  - Verificar instalación: `java -version`

- **Maven 3.8+**
  - [Descargar Maven](https://maven.apache.org/download.cgi)
  - Verificar instalación: `mvn -version`

- **Docker Desktop**
  - [Descargar Docker Desktop](https://www.docker.com/products/docker-desktop/)
  - Verificar instalación: `docker --version` y `docker-compose --version`

### Requerimientos Opcionales

- **Git**
  - [Descargar Git](https://git-scm.com/downloads)
  - Verificar instalación: `git --version`

- **IDE recomendado:**
  - IntelliJ IDEA (Community o Ultimate)
  - Eclipse
  - Visual Studio Code con extensiones Java

## 📥 Instalación

### Opción A: Setup Automático ⚡ (Recomendado)

Usa los scripts de setup automático que verifican prerequisites y configuran todo:

**En Windows:**
```cmd
setup.bat
```

**En Linux/Mac:**
```bash
chmod +x setup.sh
./setup.sh
```

El script automáticamente:
- ✅ Verifica Java, Maven y Docker
- ✅ Inicia PostgreSQL con Docker Compose
- ✅ Descarga todas las dependencias

### Opción B: Setup Manual

#### 1. Clonar el Repositorio

```bash
# Usando HTTPS
git clone https://github.com/tu-usuario/tv-looker.git

# O usando SSH
git clone git@github.com:tu-usuario/tv-looker.git

# Navegar al directorio del proyecto
cd tv-looker
```

#### 2. Descargar Dependencias

```bash
# Descargar todas las dependencias de Maven
mvn clean install

# O solo descargar sin compilar
mvn dependency:resolve
```

## 🐘 Configuración de Base de Datos

El proyecto utiliza **PostgreSQL** como base de datos principal. Para facilitar el desarrollo, se proporciona una configuración de Docker Compose.

### Archivo docker-compose.yml

El proyecto incluye un archivo `docker-compose.yml` que configura PostgreSQL con los siguientes parámetros:

```yaml
services:
  postgres:
    image: postgres:latest
    container_name: postgres-tv-looker
    restart: always
    environment:
      POSTGRES_USER: tv-looker-admin
      POSTGRES_PASSWORD: this-is-a-super-secure-password123
      POSTGRES_DB: tv-looker-db
    ports:
      - "5432:5432"
```

### Iniciar la Base de Datos

#### Paso 1: Asegúrate de que Docker Desktop esté ejecutándose

En Windows, verifica que Docker Desktop esté abierto y funcionando.

#### Paso 2: Levantar el contenedor de PostgreSQL

```bash
# Desde el directorio raíz del proyecto
docker-compose up -d
```

**Explicación de parámetros:**
- `up`: Crea e inicia los contenedores
- `-d`: Modo "detached" (segundo plano)

#### Paso 3: Verificar que el contenedor está corriendo

```bash
# Ver contenedores activos
docker ps

# Deberías ver algo como:
# CONTAINER ID   IMAGE             PORTS                    NAMES
# abc123def456   postgres:latest   0.0.0.0:5432->5432/tcp   postgres-tv-looker
```

#### Paso 4: Verificar los logs (opcional)

```bash
# Ver logs del contenedor
docker-compose logs postgres

# Seguir logs en tiempo real
docker-compose logs -f postgres
```

### Credenciales de Base de Datos

Las credenciales configuradas son:

| Parámetro | Valor |
|-----------|-------|
| **Host** | `localhost` |
| **Puerto** | `5432` |
| **Base de Datos** | `tv-looker-db` |
| **Usuario** | `tv-looker-admin` |
| **Contraseña** | `this-is-a-super-secure-password123` |

Estas credenciales están configuradas en `src/main/resources/application.properties`.

### Comandos Útiles de Docker

#### Comandos Manuales

```bash
# Detener la base de datos
docker-compose stop

# Iniciar la base de datos (si ya está creada)
docker-compose start

# Detener y eliminar el contenedor
docker-compose down

# Detener, eliminar y limpiar volúmenes (BORRA TODOS LOS DATOS)
docker-compose down -v

# Ver logs
docker-compose logs postgres

# Acceder a la consola de PostgreSQL
docker exec -it postgres-tv-looker psql -U tv-looker-admin -d tv-looker-db
```

#### Script de Limpieza Automática

También puedes usar los scripts de limpieza:

**En Windows:**
```cmd
scripts\cleanup.bat
```

**En Linux/Mac:**
```bash
./scripts/cleanup.sh
```

El script te permite elegir:
1. Detener la base de datos (mantener datos)
2. Detener y eliminar contenedor (mantener datos)
3. Limpiar todo (eliminar todos los datos)
4. Cancelar

### Conectarse a la Base de Datos con Cliente SQL

Puedes usar cualquier cliente SQL para conectarte:

**DBeaver (Recomendado):**
1. Descargar [DBeaver](https://dbeaver.io/download/)
2. Nueva Conexión → PostgreSQL
3. Usar las credenciales de la tabla anterior

**pgAdmin:**
1. Descargar [pgAdmin](https://www.pgadmin.org/download/)
2. Agregar nuevo servidor con las credenciales

**Desde línea de comandos:**
```bash
docker exec -it postgres-tv-looker psql -U tv-looker-admin -d tv-looker-db
```

## 🚀 Ejecutar el Proyecto

### Método 1: Usando Maven

```bash
# Compilar y ejecutar
mvn spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

### Método 2: Usando el JAR compilado

```bash
# Compilar el proyecto
mvn clean package

# Ejecutar el JAR
java -jar target/tv-looker-0.0.1-SNAPSHOT.jar
```

### Método 3: Desde el IDE

1. Abrir el proyecto en tu IDE
2. Localizar la clase `TvLookerApplication.java`
3. Hacer clic derecho → Run 'TvLookerApplication'

### Verificar que la aplicación está corriendo

```bash
# Verificar el endpoint de health
curl http://localhost:8080/actuator/health

# O abre en el navegador
http://localhost:8080/actuator/health
```

## 🛠 Tecnologías

### Backend
- **Java 21** - Lenguaje de programación
- **Spring Boot 4.0.2** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **Hibernate** - ORM
- **PostgreSQL** - Base de datos principal
- **H2 Database** - Base de datos en memoria para testing
- **Lombok** - Reducción de código boilerplate

### Testing
- **JUnit 5** - Framework de testing
- **Spring Boot Test** - Testing de aplicaciones Spring
- **JDBC Template** - Testing de persistencia

### Herramientas de Calidad de Código
- **Checkstyle** - Análisis de estilo de código
- **PMD** - Detección de problemas en código

### DevOps
- **Docker** - Contenedorización
- **Docker Compose** - Orquestación de contenedores
- **Maven** - Gestión de dependencias y build

## 🧪 Testing

### Ejecutar Todas las Pruebas

```bash
mvn test
```

### Ejecutar Pruebas Específicas

```bash
# Pruebas de persistencia
mvn test -Dtest=ReviewPersistenceTest

# Prueba específica
mvn test -Dtest=ReviewPersistenceTest#testPersistCompleteReview
```

### Cobertura de Pruebas

El proyecto incluye:
- ✅ Pruebas de persistencia con JDBC
- ✅ Pruebas unitarias de modelo de dominio
- ✅ Pruebas de integridad referencial

Para más información sobre las pruebas de persistencia, consulta: [`docs/JDBC_PERSISTENCE_TESTS.md`](docs/JDBC_PERSISTENCE_TESTS.md)

## 📝 Configuración del Proyecto

### Perfiles de Spring

El proyecto utiliza diferentes configuraciones según el entorno:

- **Desarrollo** (`application.properties`): Usa PostgreSQL en Docker
- **Testing** (`application-test.properties`): Usa H2 en memoria

### Estructura del Proyecto

```
tv-looker/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/tvl/tvlooker/
│   │   │       ├── TvLookerApplication.java
│   │   │       ├── domain/
│   │   │       │   └── model/          # Entidades JPA
│   │   │       └── persistence/
│   │   │           └── repository/     # Repositorios (próximamente)
│   │   └── resources/
│   │       ├── application.properties  # Configuración principal
│   │       ├── static/                 # Recursos estáticos
│   │       └── templates/              # Templates
│   └── test/
│       ├── java/                       # Pruebas Java
│       └── resources/
│           └── application-test.properties
├── docs/                               # Documentación
├── docker-compose.yml                  # Configuración Docker
├── pom.xml                             # Dependencias Maven
└── README.md
```

## 🔍 Troubleshooting

### Problema: Puerto 5432 ya está en uso

**Solución:**
```bash
# En Windows (PowerShell como Administrador)
netstat -ano | findstr :5432
taskkill /PID <PID> /F

# O cambiar el puerto en docker-compose.yml
ports:
  - "5433:5432"  # Mapear a puerto diferente
```

### Problema: Docker no inicia el contenedor

**Solución:**
```bash
# Ver logs detallados
docker-compose logs postgres

# Reiniciar Docker Desktop
# Verificar que Docker Desktop tenga recursos suficientes (Settings → Resources)
```

### Problema: Error de conexión a la base de datos

**Verificar:**
1. ¿El contenedor está corriendo? `docker ps`
2. ¿Las credenciales son correctas en `application.properties`?
3. ¿El puerto 5432 está disponible?

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo [LICENSE](LICENSE) para más detalles.

## 👥 Autores

- **TV Looker Team**

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:
1. Fork del proyecto
2. Crea tu Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la Branch (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

**Última actualización:** 2026-02-25

