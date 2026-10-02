FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY . .

# The temporal-gcp-cloud-run-* modules are unreleased; the composite build resolves them from a local SDK checkout (see README).
RUN ./gradlew --no-daemon :gcp:cloud-run:installDist

FROM eclipse-temurin:17-jre-jammy

RUN useradd --create-home --uid 10001 temporal
WORKDIR /app
COPY --from=build --chown=temporal:temporal \
    /workspace/gcp/cloud-run/build/install/cloud-run-worker/ /app/

USER 10001
ENTRYPOINT ["/app/bin/cloud-run-worker"]
