# java-jwt-hibernate-backend

Spring Boot backend with JWT authentication, Hibernate/JPA, OAuth2 (Auth0/Google), request tracing, an
audit trail, and WebSocket.

> **Living source of truth:** `docs/current/CURRENT.md` describes the current architecture, features,
> config, and open follow-ups in detail. This README is the quick-start.

## Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 bytecode (`options.release = 21`), JVM toolchain 26 |
| Framework | Spring Boot 3.5.1 |
| Build | Gradle (`./gradlew` wrapper, Groovy DSL) |
| Security | Spring Security 6 — JWT (jjwt 0.12) + OAuth2 (Auth0 / Google) |
| Persistence | Hibernate 6 (native `SessionFactory` / `EntityManager`) |
| Migrations | **Flyway** (schema owned by migrations in every runtime profile) |
| Database | MySQL 8 |
| Observability | AOP request tracing (`X-Correlation-Id` / MDC), Actuator (`/actuator/health`) |
| API Docs | SpringDoc OpenAPI 3 (`/swagger-ui/index.html`) |
| Messaging | WebSocket / STOMP |

## Prerequisites

- Java 26 JDK (bytecode targets Java 21)
- Docker Desktop (MySQL runs as a container; also used by the Testcontainers integration tests)
- Use the bundled `./gradlew` wrapper — no local Gradle install needed

## Environment variables

Secrets live in a `.env` file at the project root (gitignored). See `docs/security/secrets.md` for values.

```
DB_USERNAME=root
DB_PASSWORD=your_password
OKTA_CLIENT_ID=your_auth0_client_id
OKTA_CLIENT_SECRET=your_auth0_client_secret
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# JWT signing secrets (>= 32 bytes each). REQUIRED in every profile EXCEPT `local`,
# which ships dev-only fallbacks in application-local.yml. The app fails fast without them.
JWT_ACCESS_SECRET=base64-or-random-string-at-least-32-bytes
JWT_CREDENTIAL_SECRET=another-random-string-at-least-32-bytes

# Optional token TTL overrides (Spring duration style, e.g. 10h / 7d / 1m). Defaults 10h / 7d / 7d;
# the `local` profile already sets access-ttl=1m for quick expiry/refresh testing.
# JWT_ACCESS_TTL=10h
# JWT_REFRESH_TTL=7d
# JWT_CREDENTIAL_TTL=7d
```

## Running locally (`bootRun` → profiles `dev,local`)

```powershell
# 1. Start MySQL (exposed on host port 3307)
docker-compose -f docker/docker-compose.yml --env-file .env up -d mysql-docker-standalone

# 2. Run — bootRun auto-loads .env, points at MySQL on 3307, and activates the dev,local profiles
#    (local supplies the JWT secret fallbacks, so you don't need to export JWT_* for local dev).
.\gradlew bootRun
```

The app starts on **http://localhost:4201**. On startup **Flyway applies the migrations**
(`V1__baseline_schema` → `V2__audit_log` → `V3__seed_roles`) and Hibernate `validate`s the schema. A clean
start looks like:

```
Successfully applied N migrations to schema ...   <- Flyway
HikariPool-1 - Start completed.                   <- DB connected
Started SpringbootSecurityJwtApplication          <- app up
```

> **Adopting Flyway on an existing DB:** a database previously built by the old `ddl-auto: update` may
> conflict with the migrations (Flyway now owns the schema). Simplest for a local/dev DB — drop and let
> Flyway rebuild it: `DROP DATABASE jwt_db; CREATE DATABASE jwt_db;` then `bootRun`. See
> `docs/current/CURRENT.md` §6. Every entity change now requires a matching `Vn__*.sql` migration.

Stop MySQL when done:

```powershell
docker-compose -f docker/docker-compose.yml --env-file .env down
```

## API documentation

```
http://localhost:4201/swagger-ui/index.html      # Swagger UI
http://localhost:4201/v3/api-docs                 # OpenAPI JSON
http://localhost:4201/actuator/health             # health (public)
```

## Build JAR

```powershell
.\gradlew bootJar
# Output: build/libs/springboot-security-jwt-linkinben.jar
```

## Running with Docker Compose (full stack)

```powershell
.\gradlew bootJar                                                   # 1. build the JAR (Docker copies it)
docker-compose -f docker/docker-compose.yml --env-file .env up --build   # 2. build image + run MySQL & app
```

App starts on **http://localhost:4201** once the MySQL healthcheck passes. `down` to stop.

## Tests

```powershell
.\gradlew test              # unit + @WebMvcTest slice tests
.\gradlew integrationTest   # @SpringBootTest security ITs + Testcontainers DB ITs (needs Docker)
.\gradlew check             # both
.\gradlew jacocoCombinedReport   # merged coverage → build/reports/jacoco/combined/
```

Integration tests that use a real database (e.g. `SchemaMigrationValidationIT`) spin up a throwaway MySQL
via Testcontainers and **skip cleanly if Docker is unavailable**.

## Profiles

| Profile | File | Purpose |
|---------|------|---------|
| `dev` (default) | `application-dev.yml` | datasource + OAuth2/Auth0 registration |
| `local` | `application-local.yml` | `bootRun` adds this — JWT secret fallbacks, `access-ttl=1m` |
| `test` | `application-test.yml` | tests only — DataSource/JPA/Flyway excluded, repos mocked |

`bootRun` activates `dev,local`. Override with `$env:SPRING_PROFILES_ACTIVE = "..."` before running.

## Key endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/login` | Public | Password login → access + refresh JWT |
| `POST` | `/api/auth/oauth2/login` | Public | Exchange an OAuth2 credential for tokens |
| `POST` | `/api/auth/token/refresh` | Public | Refresh access token (refresh token in `Authorization: Bearer`) |
| `POST` | `/api/auth/logout` | Public | Blacklist the token (+ Auth0 logout URL for OAuth2) |
| `POST` | `/api/users` | Public | Register a user (→ `ROLE_USER`) |
| `GET`  | `/api/users/me` | Authenticated | Current user's profile |
| `GET`  | `/api/users` | `ROLE_ADMIN` | List users |
| `POST` | `/api/users/admin` | `ROLE_ADMIN` | Create an admin user |
| `PATCH`| `/api/users/{id}/role` | `ROLE_ADMIN` | Assign a role (rank-checked) |
| `PATCH`/`DELETE` | `/api/users/{id}` | Owner or higher rank (`@CanEditUser`) | Edit / delete a user |
| `POST` | `/api/roles` | `ROLE_ADMIN` | Create a role |
| `GET`  | `/oauth2/authorization/auth0` | Public | Start the Auth0 OAuth2 flow (browser) |
| `WS`   | `/ws` | Public | WebSocket / STOMP endpoint |

> **Contract-first API (shipped):** the API is generated from `src/main/resources/openapi/openapi.yaml`
> (the source of truth) — controllers implement the generated interfaces, responses are flat typed DTOs
> (no `{title, message, data}` envelope), and auth uses the standard `Authorization: Bearer <jwt>` header.
> The client sends its access token — and, on refresh, its refresh token — in `Authorization`. Codegen runs
> automatically on every build (`compileJava`/`bootRun`/`check`). See `docs/current/CURRENT.md`.
