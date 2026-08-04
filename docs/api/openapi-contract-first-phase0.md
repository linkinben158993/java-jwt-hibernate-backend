# OpenAPI Contract-First Revamp — Phase 0 Design & Impact

> **STATUS: AWAITING USER CONTRACT SIGN-OFF (Gate A′).**
> This document + `src/main/resources/openapi/openapi.yaml` are the review
> artifacts. **No implementation, codegen, or destructive change happens until
> the user signs the contract.** Phase 0 only ADDED files (spec + docs); the
> build stays green (`.\gradlew compileJava`).

Branch: `feature/openapi-contract-first` · Locked decisions: O-1a (full
contract-first), O-3c (big-bang all endpoints), O-4b (flatten envelope — BREAKING),
O-5b (`Authorization: Bearer` — BREAKING), O-6 (typed DTOs, entities never on the
wire), O-8 (Angular client in scope as a later track). Cutover is hard
backend+client lockstep.

Companion scout notes: [`openapi-contract-first-scout.md`](./openapi-contract-first-scout.md).

---

## 1. Endpoint inventory

All 18 REST endpoints today wrap success in `{title, message, data}`; the auth
endpoints additionally double-wrap inside `AuthenticationResponse.response`. Auth
uses the custom `access_token` / `refresh_token` headers.

| # | Method · Path | Auth | Request | Response payload (in envelope) | Smell |
|---|---------------|------|---------|--------------------------------|-------|
| 1 | POST `/api/auth/login` | public | `{username,password}` | `{accessToken,refreshToken,uName,uId,role}` (double-wrapped) | raw Map, double-wrap |
| 2 | POST `/api/auth/oauth2/login` | public | raw `{credential}` | as #1 | raw Map req+resp |
| 3 | POST `/api/auth/logout` | reads `access_token` | — | `{}` or `{auth0LogoutUrl}` | custom header |
| 4 | GET `/api/auth/okta` | public | `?code&state` | `{client-id,client-secret,code,state}` | **secret leak (S-1)** |
| 5 | POST `/api/auth/token/refresh` | reads `refresh_token` | — | `{accessToken,uName,uId}` (double-wrapped) | custom header |
| 6 | GET `/api/users/me` | authenticated | — | `{email,fullName,role}` | raw Map |
| 7 | GET `/api/users` | ADMIN | — | `List<Users>` | **JPA entity leak** |
| 8 | GET `/api/users/without-role` | ADMIN | — | `"Whatsup"` | untyped scalar |
| 9 | GET `/api/users/roles` | ADMIN | — | `"Whatsup"` | untyped scalar |
| 10 | POST `/api/users/admin` | ADMIN | `RegisterRequest` | `email` string | untyped scalar |
| 11 | POST `/api/users` | public | `RegisterRequest` | `email` string | untyped scalar |
| 12 | PATCH `/api/users/password` | authenticated | `{password}` | `ChangePasswordDTO` (echoes email) | echoes DTO |
| 13 | PATCH `/api/users/{id}` | auth + `@CanEditUser` | `UserInfoDTO` | `id` string | untyped scalar |
| 14 | PATCH `/api/users/{id}/role` | ADMIN + `@CanEditUser` | raw `{role}` | `{uId,role}` | raw Map req |
| 15 | DELETE `/api/users/{id}` | auth + `@CanEditUser` | — | `id` string | untyped scalar |
| 16 | PATCH `/api/users/info` | authenticated | `UserInfoDTO` | `UserInfoDTO` | echoes DTO |
| 17 | POST `/api/roles` | ADMIN | **raw `Roles` entity** | `rName` string | **entity as request** |
| 18 | GET `/home/hello-world` | public | — | `{title,message}` | inconsistent |

Errors (all): `ErrorResponse{title,message,errCode}` — already flat, retained.
WebSocket/STOMP handlers (`GreetingController`, `PublicChatController`) are **not**
HTTP and are out of scope for the OpenAPI contract.

---

## 2. Before → after (per endpoint)

Shorthand: envelope `{title,message,data:X}` → flat `X`; auth header
`access_token: Bearer` → `Authorization: Bearer`.

| Endpoint | Before (body) | After (flat, typed) |
|----------|---------------|---------------------|
| POST `/api/auth/login` | `{response:{title,message,data:{accessToken,…}}}` | `LoginResponse{accessToken,refreshToken,uId,uName,role}` |
| POST `/api/auth/oauth2/login` | same double-wrap | `LoginResponse` |
| POST `/api/auth/logout` | `{}` / `{auth0LogoutUrl}` | `LogoutResponse{auth0LogoutUrl?}` |
| GET `/api/auth/okta` | `{…,data:{client-secret,…}}` | `OktaInfoResponse{clientId,code,state}` — **secret dropped (S-1)** |
| POST `/api/auth/token/refresh` | double-wrap `{data:{accessToken,…}}` | `RefreshResponse{accessToken,uId,uName}` |
| GET `/api/users/me` | `{…,data:{email,fullName,role}}` | `UserProfileResponse{email,fullName,role}` |
| GET `/api/users` | `{…,data:List<Users>}` | `UserResponse[]` (no password/roles graph) |
| GET `/api/users/without-role` | `{…,data:"Whatsup"}` | `MessageResponse{message}` |
| GET `/api/users/roles` | `{…,data:"Whatsup"}` | `MessageResponse{message}` |
| POST `/api/users/admin` | `{…,data:email}` | `RegisterResponse{email}` |
| POST `/api/users` | `{…,data:email}` | `RegisterResponse{email}` |
| PATCH `/api/users/password` | `{…,data:ChangePasswordDTO}` | `MessageResponse{message}` (no echo) |
| PATCH `/api/users/{id}` | `{…,data:id}` | `UserResponse` |
| PATCH `/api/users/{id}/role` | `{…,data:{uId,role}}` | `RoleAssignmentResponse{uId,role}` |
| DELETE `/api/users/{id}` | `{…,data:id}` | `MessageResponse{message}` |
| PATCH `/api/users/info` | `{…,data:UserInfoDTO}` | `UserResponse` |
| POST `/api/roles` | body raw `Roles`; `{…,data:rName}` | body `CreateRoleRequest{rName}`; `RoleResponse{rId?,rName}` |
| GET `/home/hello-world` | `{title,message}` | `WelcomeResponse{title,message}` (unchanged) |

**Auth header change (O-5b)** applies to: the access-token filter
(`RequestFilterConfig`, reads `access_token` → `Authorization`), logout (`access_token`
→ `Authorization`), and refresh (`refresh_token` → `Authorization`). `SwaggerConfig`
flips its APIKEY `access_token` scheme to `http`/`bearer`.

**Security notes surfaced (fix at cutover, not silently):**
- **S-1** `GET /api/auth/okta` leaks the OAuth2 **client secret**. Contract drops it.
- **S-2** `GET /api/users` serialises the `Users` entity (password field, roles
  graph). Contract replaces with `UserResponse`.
- **S-3** `POST /api/roles` accepts the raw `Roles` entity (mass-assignment /
  back-reference). Contract replaces with `CreateRoleRequest`.

---

## 3. Backend test breakage

Scan of `src/test` + `src/integration-test` for assertions on `$.response…` /
`$.data…`, the `access_token` / `refresh_token` headers, and entity fields.
**~40+ test methods across 14 of 31 test classes** will need rewriting.
(Two authorization ITs — `SecurityFilterChainIT`, `UserResourceAuthorizationIT` —
assert only status codes via mock users and are **not** affected.)

### Unit tests (`src/test`)
| Class | Why it breaks | Rough count |
|-------|---------------|-------------|
| `controllers/AuthenticationControllerTest` | `$.response.data.*` reads; `.header("access_token", …)` | ~11 / 13 |
| `controllers/api/UserAPIControllerTest` | `$.data`, `$.data.email/fullName/role` | ~4 / 9 |
| `controllers/api/RoleAPIControllerTest` | `$.message` on role create (envelope gone) | 1 / 1 |
| `configs/RequestFilterConfigTest` | mocks `getHeader("access_token")` → header renamed | ~5 / 5 |

`HomeControllerTest` (`$.title`/`$.message`) survives — `WelcomeResponse` keeps
those fields.

### Integration tests (`src/integration-test`)
| Class | Why it breaks | Rough count |
|-------|---------------|-------------|
| `security/RefreshTokenIT` | `.header("refresh_token", …)` + `$.response.data.accessToken` | ~6 / 6 |
| `security/TokenRoundTripIT` | `.header("access_token", …)` | 3 / 3 |
| `tracing/RequestTracingIT` | `.header("access_token", …)` (keeps X-Correlation-Id asserts) | 3 / 3 |
| `security/RoleAssignAuditIT` | `.header("access_token", …)` + `$.data.uId/role` | 2 / 2 |
| `security/LogoutBlacklistIT` | `.header("access_token", …)` | 1 / 1 |
| `security/AuditFailureIsolationIT` | `.header("access_token", …)` | 1 / 1 |
| `security/ChangePasswordOwnershipIT` | `.header("access_token", …)` | 1 / 1 |
| `security/ExpiredTokenNoRefreshIT` | `.header("access_token", …)` | 1 / 1 |
| `security/MalformedRefreshTokenIT` | `.header("access_token"/"refresh_token", …)` | 1 / 1 |
| `security/RegisterContractIT` | `$.data` value (env. gone); errCode asserts survive | 1 / 3 |

---

## 4. Client (Angular) impact

Repo `D:\jwt-client-with-angular` (scouted read-only; unchanged in Phase 0).

**Breaks under O-4b (flatten):** every `*.response.data.*` de-envelope —
`auth-service.service.ts` (login/oauth2/refresh, ~11 reads),
`stores/effects/auth.effects.ts` (~4), `components/login/login.component.ts`
(~6, duplicate un-enveloping), `auth.interceptor.ts` refresh read
(`res.response.data.accessToken`), and `userService/admin-user.service.ts`
(`userItem.data`, `res?.data`). `models/admin-users/users.model.ts` mirrors the
`Users` entity (`password`, `roles`) and must become the `UserResponse` shape.

**Breaks under O-5b (`Authorization`):** the three custom-header senders —
`auth.interceptor.ts#withAccessToken` (`access_token`),
`auth-service.service.ts#refreshAccessToken` (`refresh_token`) and
`#logoutBackend` (`access_token`), and `admin-user.service.ts#getUserProfile`
(`access_token`).

**New behaviour:** the interceptor must additionally send `X-Correlation-Id`
(not currently sent; backend already honours + echoes it).

---

## 5. Implementation step plan (post-sign-off)

### Track A — Backend
1. **Wire the plugin.** Add `org.openapi.generator` to `build.gradle` (spring
   generator, `delegate-pattern`, `interfaceOnly`-style API interfaces + model
   DTOs), input `src/main/resources/openapi/openapi.yaml`, output under
   `build/generated`. Respect the ordering rule (`sourceSets` before
   `configurations` — see CLAUDE.md).
2. **Generate & inspect DTOs/interfaces.** Run codegen; confirm generated models
   match the signed schemas; add the generated dir to the source set.
3. **Introduce mappers.** Entity→DTO mapping (`Users`→`UserResponse`,
   `Roles`→`RoleResponse`) so entities never reach controllers' return types (O-6).
4. **Migrate controllers to the generated API interfaces**, endpoint by endpoint:
   return flat DTOs, delete the `ok(title,message,data)` helper and
   `AuthenticationResponse`, drop the `{title,message,data}` map building.
5. **Auth header cutover (O-5b).** `RequestFilterConfig`, logout, refresh read
   `Authorization`; flip `SwaggerConfig` to `http`/`bearer`. Keep `/api/auth/**`
   `shouldNotFilter`.
6. **Security fixes** S-1 (drop client secret), S-2 (`UserResponse`), S-3
   (`CreateRoleRequest`).
7. **Rewrite tests** (the 14 classes in §3): replace `$.response.data.*` /
   `$.data` with flat JSONPaths; swap `.header("access_token"/"refresh_token")`
   for `.header("Authorization", …)`; drop entity-field assertions. Keep the
   two-tier split (CLAUDE.md): unit `@WebMvcTest`, security `*IT` in
   `src/integration-test`.
8. **CI drift check.** Gradle task (`openApiValidate` + a "generated sources are
   up to date / controllers implement every operation" check) failing the build
   when code and `openapi.yaml` diverge; wire into `.\gradlew check`.
9. **Verify:** `.\gradlew check` green; `jacocoCombinedReport` coverage restored.

### Track B — Angular client
1. **Regenerate a TS client** from the signed `openapi.yaml`
   (`openapi-generator` `typescript-angular`) into a generated module.
2. **Replace hand-written types** — retire the `*.response.data.*` reads in
   `auth-service.service.ts`, `auth.effects.ts`, `login.component.ts`,
   `auth.interceptor.ts`; delete the entity-shaped `users.model.ts` in favour of
   generated `UserResponse`.
3. **Interceptor:** switch `withAccessToken` from `access_token` to
   `Authorization: Bearer`; update the refresh read to the flat `accessToken`;
   **add the `X-Correlation-Id` sender.**
4. **auth-service:** `refreshAccessToken` / `logoutBackend` → `Authorization`
   header; `admin-user.service.getUserProfile` likewise; `getAllUser` reads the
   top-level array; `me` reads the top-level object.
5. **NgRx:** update `Login` effect + `LogInSuccess` payload mapping to flat
   bodies; reducer unaffected (consumes the action payload).
6. **Lockstep cutover** with the backend; smoke-test login → refresh → admin list
   → logout (incl. OAuth2 `auth0LogoutUrl`).

---

## 6. Top breaking-change risks for the review gate

1. **Hard lockstep, no version skew.** Flatten + header change are simultaneous
   BREAKING; an old client against a new backend fails to read tokens *and* sends
   the wrong auth header. Cutover must be atomic (or add a short back-compat shim).
2. **Refresh via `Authorization`.** Both the access-token filter and the refresh
   endpoint would read `Authorization`; confirm the refresh endpoint stays under
   `shouldNotFilter("/api/auth/")` so an expired access token can't block refresh.
3. **`login.component.ts` duplicates the effect's un-enveloping** — two code paths
   read `response.response.data.*`; both must change or login silently breaks.
4. **Entity-leak removals are behaviour changes** (S-1 secret, S-2 `List<Users>`
   → `UserResponse`, S-3 role entity body). Confirm no client relies on the leaked
   fields (`password`, `roles`, `client-secret`) before removing them.
5. **`MessageResponse` for former scalar/echo endpoints** (`without-role`,
   `roles`, password change, delete) changes those bodies; low client usage but
   part of the contract to confirm.
6. **`X-Correlation-Id` newly required from the client** — not sent today; adding
   it is safe (backend generates one when absent) but should be verified in the
   interceptor.
