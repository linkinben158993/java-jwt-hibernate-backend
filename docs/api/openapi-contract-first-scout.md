# OpenAPI Contract-First — Phase 0 Scout Report

Read-only reconnaissance of the backend REST surface and the Angular client that
consumes it. No code changed. Feeds the design/impact doc and `openapi.yaml`.

---

## 1. Backend API inventory

Source: `src/main/java/io/linkinben/springbootsecurityjwt/controllers/**` +
`.../dtos/**`. All success responses today are wrapped in the
`{title, message, data}` envelope; the auth endpoints additionally wrap that map
inside `AuthenticationResponse` → `{ "response": { title, message, data } }`
(a **double** wrap). Auth uses the **custom** `access_token` / `refresh_token`
request headers, not `Authorization`.

| # | Method | Path | Auth (SecurityConfig) | Request shape | Response payload (inside envelope) | Smell |
|---|--------|------|-----------------------|---------------|------------------------------------|-------|
| 1 | POST | `/api/auth/login` | public | `AuthenticationRequest{username,password}` | `data{accessToken,refreshToken,uName,uId,role}` **inside** `AuthenticationResponse.response` | double-wrap; raw `Map` |
| 2 | POST | `/api/auth/oauth2/login` | public | raw `Map{credential}` | same as #1 | double-wrap; raw `Map` req+resp |
| 3 | POST | `/api/auth/logout` | public (reads `access_token` hdr) | — | raw `Map{}` or `{auth0LogoutUrl}` (NOT double-wrapped — plain `ResponseEntity.ok`) | reads custom header |
| 4 | GET | `/api/auth/okta` | public | query `code,state` | `data{client-id,client-secret,code,state}` | **leaks client-secret** (S-1) |
| 5 | POST | `/api/auth/token/refresh` | public (reads `refresh_token` hdr) | — | `data{accessToken,uName,uId}` inside `AuthenticationResponse` | reads custom header |
| 6 | GET | `/api/users/me` | authenticated | — | `data{email,fullName,role}` | raw `Map` |
| 7 | GET | `/api/users` | `ROLE_ADMIN` | — | `data: List<Users>` | **JPA entity leak** (password, roles graph) |
| 8 | GET | `/api/users/without-role` | `ROLE_ADMIN` | — | `data: "Whatsup"` (string) | untyped scalar |
| 9 | GET | `/api/users/roles` | `ROLE_ADMIN` | — | `data: "Whatsup"` (string) | untyped scalar |
| 10 | POST | `/api/users/admin` | `ROLE_ADMIN` | `RegisterRequest{email,fullName,password}` | `data: email` (string) | untyped scalar |
| 11 | POST | `/api/users` | public | `RegisterRequest` | `data: email` (string) | untyped scalar |
| 12 | PATCH | `/api/users/password` | authenticated | `ChangePasswordDTO{password}` | `data: ChangePasswordDTO` (echoes email) | echoes request DTO |
| 13 | PATCH | `/api/users/{id}` | authenticated + `@CanEditUser` | `UserInfoDTO{fullName,age,dob}` | `data: id` (string) | untyped scalar |
| 14 | PATCH | `/api/users/{id}/role` | `ROLE_ADMIN` + `@CanEditUser` | raw `Map{role}` | `data{uId,role}` | raw `Map` req |
| 15 | DELETE | `/api/users/{id}` | authenticated + `@CanEditUser` | — | `data: id` (string) | untyped scalar |
| 16 | PATCH | `/api/users/info` | authenticated | `UserInfoDTO` | `data: UserInfoDTO` | echoes request DTO |
| 17 | POST | `/api/roles` | `ROLE_ADMIN` | **raw `Roles` JPA entity** | `data: rName` (string) | **entity as request body** |
| 18 | GET | `/home/hello-world` | public | — | `{title,message}` (no `data`) | inconsistent envelope |

Error envelope (all non-2xx via `GlobalExceptionHandler`): `ErrorResponse{title,
message, errCode}` — already flat, kept in the target contract.

**Not REST (out of scope for OpenAPI):** WebSocket / STOMP controllers
`GreetingController` (`/hello`, `/join`) and `PublicChatController`
(`/join/{room}`, `/public/message/{room}`, …) are STOMP `@MessageMapping`
handlers, not HTTP endpoints — they are not part of the OpenAPI HTTP contract.

**Header handling today:**
- `RequestFilterConfig.doFilterInternal` reads `request.getHeader("access_token")`
  and expects `Bearer <jwt>`. `shouldNotFilter` skips `/api/auth/`.
- `AuthenticationController.logout` reads `access_token`; `.refreshToken` reads
  `refresh_token`.
- `TracingFilter` already honours inbound `X-Correlation-Id` and echoes it on the
  response (constant `MdcKeys.CORRELATION_ID_HEADER`). This is the one header the
  target contract keeps as-is.
- `SwaggerConfig` currently declares an `access_token` **APIKEY** security scheme
  — must flip to `http`/`bearer` (`Authorization`) at cutover.

---

## 2. Angular client scout (READ-ONLY)

Repo: `D:\Programming\Learning\jwt-client-with-angular`. Nothing modified.

**HTTP services / models that consume the API**

| File | Role | Envelope / header coupling |
|------|------|----------------------------|
| `src/app/services/API/auth.interceptor.ts` | attaches auth header; refresh-on-401 | sends `access_token: Bearer <t>` (`withAccessToken`); reads `res.response.data.accessToken` on refresh; **does not yet send `X-Correlation-Id`** |
| `src/app/services/API/endPoints.ts` | endpoint constants | paths only — unaffected by shape changes |
| `src/app/services/authService/auth-service.service.ts` | login / oauth2 / refresh / logout | reads `result.response.data.{uId,uName,accessToken,refreshToken,role}`; sends `refresh_token: Bearer` (refresh) and `access_token: Bearer` (logout) |
| `src/app/services/userService/admin-user.service.ts` | admin user list / me | reads `userItem.data` (envelope) and `res?.data`; sends `access_token: Bearer` on `getUserProfile` |
| `src/app/models/user.ts` | `User` model | field mapping from `response.data.*` |
| `src/app/models/admin-users/users.model.ts` | `Users` model | mirrors JPA entity: `password`, `roles` — coupled to the `List<Users>` leak (#7) |
| `src/app/stores/effects/auth.effects.ts` | NgRx `Login` effect | reads `response.response.data.{accessToken,refreshToken,uName,uId}` |
| `src/app/stores/actions/auth.actions.ts` | actions | `LogInSuccess` payload = the above fields |
| `src/app/stores/reducers/auth.reducers.ts` | reducer | consumes `LogInSuccess.payload` (indirect) |
| `src/app/components/login/login.component.ts` | login page | **duplicate** un-enveloping: reads `response.response.data.*` |

**AuthInterceptor specifics:** `isAuthEndpoint()` skips login/oauth2/refresh so
the interceptor never attaches a token to those; recovery is gated on
`isLoggedIn()` (session), refreshes via `auth.refreshAccessToken()`, and on
failure dispatches `LogOut`. The `X-Correlation-Id` sender is a **planned
addition** — not present today.

**What breaks under O-4b (flatten) + O-5b (`Authorization`):**
1. Every `*.response.data.*` read (auth-service ×11, effects ×4, login.component
   ×6, interceptor ×1) breaks — payload moves to the top level.
2. `admin-user.service` `userItem.data` / `res?.data` breaks — list becomes a
   top-level array; `me` becomes a top-level object.
3. All three custom-header senders (`access_token`, `refresh_token`) must move to
   `Authorization: Bearer` — interceptor `withAccessToken`, `refreshAccessToken`,
   `logoutBackend`, `getUserProfile`.
4. `users.model.ts` `roles`/`password` fields disappear (entity → `UserResponse`).
5. `X-Correlation-Id` sender must be added to the interceptor (new behaviour).
