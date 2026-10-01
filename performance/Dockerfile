# syntax=docker/dockerfile:1
# Container-ready Gatling performance runner for the jwt-security backend.
# Build context = repo root:
#   docker build -f performance/Dockerfile -t jwt-perf .
#
# NOTE (N1): the gatling-java-learning framework is published to mavenLocal only. For a self-contained run,
# mount the host ~/.m2 until a shared artifact repo (Nexus/Artifactory) is available:
#   docker run --rm -v "$HOME/.m2:/root/.m2" \
#     -e HTTP_BASE_URL=http://host.docker.internal:4201 \
#     -e DB_URL=jdbc:mysql://host.docker.internal:3307/jwt_db -e DB_USER=root -e DB_PASSWORD=... \
#     jwt-perf
# Reports are written under /app/performance/build/reports/gatling (mount a volume to keep them).
#
# This image is groundwork for the deferred local-Kubernetes perf testing (N7/N10); the step-08 acceptance
# round runs ./gradlew :performance:runLoadTest locally, not this container.
FROM eclipse-temurin:26-jdk
WORKDIR /app
COPY . .
ENV HTTP_BASE_URL=http://host.docker.internal:4201
ENTRYPOINT ["./gradlew", ":performance:runLoadTest", "--no-daemon"]
