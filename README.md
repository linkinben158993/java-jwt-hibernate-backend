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
- MySQL 8 running on `localhost:3306` with a schema named `jwt_db`
- Gradle 9.5.1 (or use the included `./gradlew` wrapper — no local install needed)

## Environment variables

All secrets are injected via environment variables. Copy the values from `docs/secrets.md` (gitignored).

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `OKTA_CLIENT_ID` | Okta OAuth2 client ID |
| `OKTA_CLIENT_SECRET` | Okta OAuth2 client secret |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |

### Set variables (PowerShell)

```powershell
$env:DB_USERNAME        = "root"
$env:DB_PASSWORD        = "your_password"
$env:OKTA_CLIENT_ID     = "your_okta_client_id"
$env:OKTA_CLIENT_SECRET = "your_okta_client_secret"
$env:GOOGLE_CLIENT_ID   = "your_google_client_id"
$env:GOOGLE_CLIENT_SECRET = "your_google_client_secret"
```

## Running locally (dev profile)

The `dev` profile is active by default. It connects to `localhost:3306/jwt_db`.

```powershell
# 1. Start MySQL and make sure jwt_db schema exists (created automatically on first run)

# 2. Set env vars (see above)

# 3. Run
.\gradlew bootRun
```

App starts on **http://localhost:4201**

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
