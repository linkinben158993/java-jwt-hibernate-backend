# java-jwt-hibernate-backend

Spring Boot backend with JWT authentication, Hibernate/JPA, OAuth2 (Okta + Google), and WebSocket.

## Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 26 (targeting Java 21 bytecode) |
| Framework | Spring Boot 3.5.1 |
| Build | Gradle 9.5.1 (Groovy DSL) |
| Security | Spring Security 6 — JWT + OAuth2 (Okta, Google) |
| Persistence | Hibernate 6 via JPA (`EntityManager`) |
| Database | MySQL 8 |
| API Docs | SpringDoc OpenAPI 3 (`/swagger-ui/index.html`) |
| Messaging | WebSocket / STOMP |

## Prerequisites

- Java 26 JDK
- Docker Desktop (MySQL is provided as a container)
- Gradle 9.5.1 (or use the included `./gradlew` wrapper — no local install needed)

## Environment variables

All secrets live in a `.env` file at the project root (gitignored). Copy the values from `docs/secrets.md` to create it:

```
DB_USERNAME=root
DB_PASSWORD=your_password
OKTA_CLIENT_ID=your_okta_client_id
OKTA_CLIENT_SECRET=your_okta_client_secret
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

Docker Compose reads `.env` automatically. For `bootRun`, load the same file into your shell first (see below).

## Running locally (dev profile)

MySQL is provided via Docker. The app runs on the host via `bootRun` and connects to MySQL on port **3307**.

```powershell
# 1. Start MySQL
docker-compose -f docker/docker-compose.yml --env-file .env up -d mysql-docker-standalone

# 2. Load .env into the shell, then set the DB URL to hit Docker's port 3307
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
    }
}
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3307/jwt_db?createDatabaseIfNotExist=true&autoReconnect=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"

# 3. Run
.\gradlew bootRun
```

App starts on **http://localhost:4201**. Confirmed start looks like:

```
HikariPool-1 - Start completed.          <- DB connected
Started SpringbootSecurityJwtApplication <- app fully up
```

Stop MySQL when done:

```powershell
docker-compose -f docker/docker-compose.yml --env-file .env down
```

## API documentation

Once running, open:

```
http://localhost:4201/swagger-ui/index.html
```

## Build JAR

```powershell
.\gradlew bootJar
# Output: build/libs/springboot-security-jwt-linkinben.jar
```

## Running with Docker Compose (full stack)

Builds the app image and runs both MySQL and the app as containers together.

```powershell
# 1. Build the JAR first — Docker copies it from build/libs/
.\gradlew bootJar

# 2. Set env vars (DB_PASSWORD, OKTA_*, GOOGLE_* — see above)
#    DB_USERNAME / DB_PASSWORD default to "root" if unset

# 3. Build image and start both services
docker-compose -f docker/docker-compose.yml --env-file .env up --build
```

App starts on **http://localhost:4201** once the MySQL healthcheck passes.

```powershell
# Stop and remove containers
docker-compose -f docker/docker-compose.yml --env-file .env down
```

## Profiles

| Profile | File | Purpose |
|---------|------|---------|
| `dev` (default) | `application-dev.yml` | Local development, localhost MySQL |

To run with a different profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
.\gradlew bootRun
```

## Key endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/authenticate/login` | Public | JWT login |
| `POST` | `/api/user/register` | Public | Register user |
| `GET` | `/api/user` | `ROLE_ADMIN` | List all users |
| `POST` | `/api/role/add` | `ROLE_ADMIN` | Add role |
| `GET` | `/oauth/login` | Public | OAuth2 login (Okta / Google) |
| `WS` | `/ws` | Public | WebSocket endpoint |
