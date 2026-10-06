FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY . .

# temporal-gcp-cloud-run-opentelemetry is unreleased; the composite build resolves it from a local SDK checkout (see README).
RUN ./gradlew --no-daemon :gcp:cloud-run:opentelemetry:installDist

FROM eclipse-temurin:17-jre-jammy

RUN useradd --create-home --uid 10001 temporal
WORKDIR /app
COPY --from=build --chown=temporal:temporal \
    /workspace/gcp/cloud-run/opentelemetry/build/install/cloud-run-worker/ /app/

USER 10001
ENTRYPOINT ["/app/bin/cloud-run-worker"]
