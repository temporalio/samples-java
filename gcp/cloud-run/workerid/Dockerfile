FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY . .

# temporal-gcp-cloud-run-id is unreleased; the composite build resolves it from a local SDK checkout (see README).
RUN ./gradlew --no-daemon :gcp:cloud-run:workerid:installDist

FROM eclipse-temurin:17-jre-jammy

RUN useradd --create-home --uid 10001 temporal
WORKDIR /app
COPY --from=build --chown=temporal:temporal \
    /workspace/gcp/cloud-run/workerid/build/install/cloud-run-worker-id/ /app/

USER 10001
ENTRYPOINT ["/app/bin/cloud-run-worker-id"]
