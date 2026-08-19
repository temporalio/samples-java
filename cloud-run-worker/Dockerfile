FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY . .

# The worker depends on io.temporal:temporal-gcp-cloud-run, resolved through the samples'
# javaSDKVersion. Until a Temporal Java SDK release contains that module, build against a local
# SDK checkout with the composite build (see README), or publish the module to Maven Local first.
RUN ./gradlew --no-daemon :cloud-run-worker:installDist

FROM eclipse-temurin:17-jre-jammy

RUN useradd --create-home --uid 10001 temporal
WORKDIR /app
COPY --from=build --chown=temporal:temporal \
    /workspace/cloud-run-worker/build/install/cloud-run-worker/ /app/

USER 10001
ENTRYPOINT ["/app/bin/cloud-run-worker"]
