FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY . .

# Replace this snapshot with the first released version that contains temporal-gcp.
ARG TEMPORAL_GCP_VERSION=1.37.0-SNAPSHOT
RUN ./gradlew --no-daemon :gcp-opentelemetry:installDist \
    -PtemporalGcpVersion=${TEMPORAL_GCP_VERSION}

FROM eclipse-temurin:17-jre-jammy

RUN useradd --create-home --uid 10001 temporal
WORKDIR /app
COPY --from=build --chown=temporal:temporal \
    /workspace/gcp-opentelemetry/build/install/gcp-opentelemetry/ /app/

USER 10001
ENTRYPOINT ["/app/bin/gcp-opentelemetry"]
