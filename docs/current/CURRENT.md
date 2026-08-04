# CURRENT — java-jwt-hibernate-backend

> **Source of truth for the system's present state.** Read this first; update it when a feature lands.
> This file is tracked in git. The detailed design/decision/plan docs under `docs/**` are **untracked
> workspace history** (see §10) — this doc stands on its own and does not depend on them.
>
> **Structure:** lives in `docs/current/` so it can grow. When a section gets too big, split it into a
> sibling file (e.g. `docs/current/auth.md`, `docs/current/observability.md`) and link it from the
> section heading here — keep this file the index + at-a-glance summary.

_Last updated: 2026-08-03 · Build: `.\gradlew check` green (129 tests, 0 failures)._

---

## 1. System at a glance

- **Stack:** Java 21 (`options.release=21`, JVM toolchain 26) · Spring Boot 3.5.1 · Spring Security 6 ·
  Spring Data JPA (native Hibernate `SessionFactory`) · jjwt 0.12 (HS256) · OAuth2/Auth0 · WebSocket ·
  MySQL · Flyway · AOP + Actuator · Gradle · Windows/PowerShell dev.
- **Shape:** stateless JWT REST API (`/api/**`), CSRF disabled, plus an OAuth2/Auth0 browser login and a
  WebSocket chat surface.

**Request lifecycle (one line):**
```
TracingFilter (correlationId → MDC, HIGHEST_PRECEDENCE)
  → Spring Security chain → RequestFilterConfig (access_token JWT filter, MDC uId)
    → controller → service → repository (Hibernate)     [AOP TracingAspect wraps ctrl/svc/repo]
```

---

## 2. Feature status

| Area | State | Key code | Notes |
|---|---|---|---|
| Password login | ✅ | `AuthenticationController.createAuthJWT` | `POST /api/auth/login` → access + refresh JWT |
| OAuth2 / Auth0 SSO | ✅ | `AuthenticationHandler`, `…/oauth2/login` | credential hand-off token → `POST /api/auth/oauth2/login`; email whitelist |
| Logout + SSO teardown | ✅ | `AuthenticationController.logout` | publishes event → sync blacklist; oauth2 → Auth0 logout URL |
| Refresh flow | ✅ | `…/token/refresh` | validates refresh token → new access token, preserves `loginMethod` |
| JWT keys / secrets | ✅ | `jwt/KeyProvider` | externalized (G10), fail-fast < 32 bytes |
| Token creation (Factory) | ✅ | `jwt/TokenFactory`, `TokenType` | one type-keyed builder; public API unchanged |
| **Configurable token TTL** | ✅ | `TokenFactory` + `jwt.*-ttl` | env-overridable; **local access-ttl = 1m**; defaults 10h/7d/7d |
| Token blacklist (logout) | ✅ (instance-local) | `TokenBlacklistService` | in-memory `Map` + `@Scheduled` cleanup — **not shared across instances** (roadmap) |
| Authorization: filter rules | ✅ | `SecurityConfig` | see §4 |
| Authorization: ownership/rank | ✅ | `@CanEditUser`, `authz` | `UserAuthorizationService` rank matrix; denial → 404 (hide existence) |
| **Observability: correlationId** | ✅ | `tracing/*` | `X-Correlation-Id` honour-inbound-or-generate; MDC `correlationId`; echoed on response |
| **AOP request tracing** | ✅ | `TracingAspect` | controllers INFO, services/repos DEBUG (component#method + timing) |
| **Audit trail (Observer)** | ✅ | `events/*`, `audit/*` | async listeners → `audit_log` (`!test`) / log-only (`test`) |
| **Design patterns** | ✅ | Observer + Factory | events/listeners; KeyProvider/TokenFactory |
| **Flyway migrations** | ✅ | `db/migration/V1,V2` | per-profile split (§6) |
| Actuator | ✅ | `SecurityConfig` | `/actuator/health` anonymous; rest authenticated |
| Centralized errors + `@Valid` | ✅ | `GlobalExceptionHandler` | consistent `ErrorResponse` envelope, no stack-trace leaks |
| WebSocket chat | ⚠ public | `SocketConfig`, chat controllers | no STOMP auth (G7, deprioritized) |
| Front-end sends correlationId | ❌ TODO | (Angular repo) | backend honours it; client not sending yet |

---

## 3. Auth & tokens

- **Access/refresh** issued on login; refresh exchanged at `POST /api/auth/token/refresh`. `loginMethod`
  (`password` / `oauth2`) is carried in both tokens so logout can still tear down Auth0.
- **Keys:** `KeyProvider` owns two `SecretKey`s from `jwt.access-secret` / `jwt.credential-secret`
  (env). No hardcoded secret except the `local` profile fallback; every other env fails fast.
- **TTLs (configurable):** `jwt.access-ttl` / `jwt.refresh-ttl` / `jwt.credential-ttl` (Spring duration
  style, e.g. `10h`/`7d`/`1m`; env `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` / `JWT_CREDENTIAL_TTL`).
  Defaults **10h / 7d / 7d**; **local overrides access-ttl to `1m`** for quick expiry/refresh testing.
- **Logout revocation:** the access token is added to `TokenBlacklistService` (in-memory) by a sync
  event listener; the filter rejects blacklisted tokens.

## 4. Authorization

- **Filter rules (`SecurityConfig`):** `permitAll` → `/api/auth/**`, `POST /api/users`, swagger,
  `/actuator/health`. `ROLE_ADMIN` → `GET /api/users`, `/api/users/roles`, `/api/users/without-role`,
  `POST /api/users/admin`, `PATCH /api/users/*/role`, `/api/roles/**`. `authenticated` →
  `PATCH`/`DELETE /api/users/*`. Everything else authenticated.
- **Method security:** `@CanEditUser` → `@authz.canEdit(auth, #id)` — caller must **be** the target or
  **strictly outrank** it. `canAssignRole` — a granted role must rank **strictly below** the caller.
  Ranks in code: `ADMIN=20 > USER=10`. An ownership/rank denial is masked as **404** (controller-local
  handler); a global authz denial is **403**.

## 5. Observability (correlationId + audit)

- **Correlation id:** `TracingFilter` (before Spring Security) reuses an inbound **`X-Correlation-Id`**
  or generates a UUID, puts it in MDC (`correlationId`), echoes it on the response, and clears MDC in a
  `finally`. `uId` is added to MDC after authentication. Logback prints `[%X{correlationId:-}]`.
  → **Front-end is meant to generate + send the id; backend honours it, else generates.**
- **AOP tracing:** `TracingAspect` logs `-> Class#method` / `<- Class#method (N ms)` — controllers at
  INFO, services/repositories at DEBUG. ASCII markers (non-ASCII mojibakes on non-UTF-8 consoles).
- **Audit (Observer):** auth-lifecycle events (`UserLoggedOutEvent`, `UserRegisteredEvent`,
  `RoleAssignedEvent`) are published by the controllers; `AuthLifecycleListeners` handles them — the
  blacklist **sync**, audit **`@Async`**. `AuditService` is profile-split: `PersistentAuditService`
  (`!test`, writes `audit_log`, log-only fallback) / `LoggingAuditService` (`test`, log-only).
  `MdcTaskDecorator` carries the correlationId across the async hop.

## 6. Persistence & migrations (Flyway — everywhere)

| Profile | `ddl-auto` | Flyway | Owner |
|---|---|---|---|
| `local` (bootRun) | `validate` | on | Flyway owns schema; Hibernate validates |
| default / `dev` / prod | `validate` | on (`baseline-on-migrate`) | Flyway owns schema; Hibernate validates |
| `test` (IT) | n/a — JPA + Flyway autoconfig excluded | mocked repos |

- **Flyway everywhere:** `local`/`dev`/prod all run `ddl-auto: validate` + Flyway on — one migration
  mechanism across every runtime profile. Only the mocked-repo `test` profile excludes DataSource/JPA/Flyway.
- **Migrations:** `V1__baseline_schema.sql` (users / roles / owned_roles / keywords) + `V2__audit_log.sql`
  + `V3__seed_roles.sql` (role seed — idempotent `INSERT IGNORE` of `ROLE_USER` / `ROLE_ADMIN`).
  Entities: `users`, `roles`, `owned_roles` (join), `keywords`, `audit_log`.
- **Tripwire:** an entity change without a matching `Vn__*.sql` now fails startup in **all** profiles
  (local included) — every entity change needs a migration.
  ⚠ `V1` is hand-authored; **regenerate it from `mysqldump --no-data` before provisioning a fresh DB.**
- *2026-08-04: reversed the local-Hibernate split -> Flyway everywhere, to seed roles uniformly (G8).*

## 7. Config & profiles

- `application.yml` — base: JWT secrets + TTLs (env), `ddl-auto:validate`, Flyway on.
- `application-local.yml` — dev-only JWT fallback secrets, `ddl-auto:validate`, Flyway on
  (was `ddl-auto:update` + Flyway off), `access-ttl:1m`.
- `application-dev.yml` — datasource + OAuth2/Auth0 registration (from `.env`).
- `application-test.yml` — static test secrets; excludes DataSource / JPA / Flyway autoconfig.
- **Env vars:** `JWT_ACCESS_SECRET`, `JWT_CREDENTIAL_SECRET`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`,
  `JWT_CREDENTIAL_TTL`, `DB_USERNAME`, `DB_PASSWORD`, `OKTA_CLIENT_ID`, `OKTA_CLIENT_SECRET`,
  `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`. `bootRun` loads `.env` and activates `dev,local`.

## 8. Testing & build

- **Two-tier (do not mix):** unit `@WebMvcTest` + `TestSecurityConfig` in `src/test/` (security bypassed,
  controller logic only); integration `@SpringBootTest` + `@ActiveProfiles("test")` in
  `src/integration-test/` (`*IT.java`, real `SecurityFilterChain`, 401/403 live). JaCoCo combined.
- **Commands:** `.\gradlew test` · `integrationTest` · `check` · `jacocoCombinedReport` · `bootRun`.
- **Now:** 129 tests, 0 failures. JaCoCo emits a non-fatal `Unsupported class file major version 70`
  warning (JDK 26 bootstrap classes) — application classes (Java 21) unaffected.

---

## 9. CURRENT follow-ups / roadmap

**Backend**
- **Front-end correlation id:** ready to honour `X-Correlation-Id`; the Angular interceptor doesn't send
  it yet → the id is backend-generated today. (client task)
- **Distributed token blacklist** (G11 persistence): in-memory `Map` is instance-local; move to a shared
  store (Redis/DB) for horizontal scaling.
- **State-mutating GET endpoints** (G13): `GET /api/users/roles` and `/without-role` perform writes —
  should be `POST`/`PATCH`.
- **WebSocket auth** (G7, deprioritized): `/ws`, `/app`, `/topic` are `permitAll`, no STOMP auth.
- **Flyway `V1` baseline:** regenerate from a real `mysqldump` before a fresh-DB provision.
- **OpenAPI contract-first** (parked): current API models are code-first/loose; revisit if a typed client
  contract is needed (would also formalise the `X-Correlation-Id` header).

**Client (Angular repo)**
- Send `X-Correlation-Id` per request; proactive token-expiry handling (G6); `uId`-in-localStorage
  hygiene (G9); ensure auth headers on all calls (G2).

**Testing / cosmetic**
- **Audit DB-write path** is not exercised by the IT suite (`test` mocks JPA); verified live instead — add
  a real-DB/`@DataJpaTest` IT if you want it covered automatically. A reusable Testcontainers real-DB IT
  pattern now exists (`RoleSeedIT` — throwaway MySQL + real Flyway) — a candidate to later close this gap.
- Optional: role-assign **404 hide-existence** path IT (happy + forbidden are covered).
- Cosmetic: em-dashes remain in **comments** (never reach the console) — harmless.

---

## 10. Design history (workspace, **untracked** — not in git)

These capture the decisions/plans behind the above. They live in the working tree but are **not
committed**; treat them as background, not source of truth (this file is).

- Observability: `docs/observability/request-tracing-plan.md`
- Design patterns: `docs/design/observer-pattern-rollout-plan.md`, `pattern-opportunities.md`,
  `post-impl-async-audit-and-logout-relocation.md`
- Security: `docs/security/security-gaps.md`, `authorization-layer-decision.md`,
  `jwt-secret-externalization/plan.md`, `refresh-token-flow/plan.md`
- Testing: `docs/design/testing/test-plan-async-audit-logout-relocation.md`,
  `docs/design/test-plan.md`, `docs/migration/testing/verification-checklist.md`
- Client follow-up: `docs/client/followup-correlation-id-sender.md`
- API: `docs/api/openapi-contract-revamp-tldr.md`
