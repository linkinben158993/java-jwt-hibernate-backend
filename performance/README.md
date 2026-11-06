# Performance module (Gatling)

Load tests for the jwt-security backend, built on the `gatling-java-learning` framework. Two scenarios:

| Scenario | Type | Target | Verifies |
|---|---|---|---|
| `LoginHttpScenario` | HTTP→response | `POST /api/auth/login` | 200 + `accessToken` in body |
| `RegisterHttpDbScenario` | HTTP→DB | `POST /api/users` → `users` row | row present via JDBC (`SELECT u_id … WHERE email=?`) |

TPS is 1 each (`totalTps: 2`) in `src/main/resources/default-test-plan.yaml` — a smoke/wiring baseline; raise
for real load. No Kafka scenario (the backend has none — Phase 2 / future).

## Prerequisites

1. **Publish the framework to mavenLocal** (consumed by this module):
   ```
   cd ../../CV/Algo/gatling-java-learning && ./gradlew publishToMavenLocal
   ```
2. **Start MySQL + the backend** (the target):
   ```
   docker-compose -f docker/docker-compose.yml --env-file .env up -d mysql-docker-standalone
   ./gradlew bootRun        # backend on :4201 (profiles dev,local)
   ```
   ⚠ `bootRun` eagerly contacts Auth0 (`.well-known/openid-configuration`); if Auth0 is unreachable it
   fails fast (see `docs/current/CURRENT.md` §9). Run when reachable.
3. **Pre-seed the login account** (`LoginHttpScenario` needs an existing user):
   ```
   curl -s -X POST http://localhost:4201/api/users -H "Content-Type: application/json" \
     -d '{"email":"perf@test.local","fullName":"Perf User","password":"pw12345"}'
   ```

## Run

```
# targets via env (defaults shown); N5 local source — Vault/SSM are future extension points
export HTTP_BASE_URL=http://localhost:4201
export DB_URL=jdbc:mysql://localhost:3307/jwt_db
export DB_USER=root
export DB_PASSWORD=...
export DB_POOL_SIZE=2        # N3: keep minimal — don't exhaust the app's DB connections

./gradlew :performance:runLoadTest
```

Report: `performance/build/reports/gatling/<run>/index.html`. Confirm both scenarios drove traffic,
requests succeeded, latency/percentiles recorded, and HTTP→DB rows were verified under load.

## Container (groundwork for k8s — N7/N10, deferred)

```
docker build -f performance/Dockerfile -t jwt-perf .
docker run --rm -v "$HOME/.m2:/root/.m2" -e HTTP_BASE_URL=http://host.docker.internal:4201 jwt-perf
```
The `-v ~/.m2` mount provides the framework until a shared artifact repo exists (N1).

## Notes / follow-ups

- **Gatling 3.10.5** across framework + module (N4 fallback: 3.14.3's `ChainableAction` API change — loop-back).
- **Schema coupling:** `RegisterHttpDbScenario`'s JDBC verify depends on the `users.email` column — a future
  migration could break it.
- **Phase 2 (deferred):** a simple kafka-to-kafka flow in the backend + scenario (framework already supports
  kafka-to-kafka); local k8s deployment + perf-module profiles.
