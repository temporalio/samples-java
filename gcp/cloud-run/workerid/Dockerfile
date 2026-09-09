FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY . .

# TEMPORARY (draft): this sample depends on io.temporal:temporal-gcp-cloud-run-worker-id, which is
# not yet released to Maven Central. Until it ships, the Gradle build resolves it from a local
# Temporal Java SDK checkout through a composite build (see README.md and settings.gradle). For an
# image build the local SDK checkout must be available in the build context (or the module published
# to Maven Local); once the module is released, bump javaSDKVersion in the samples root build.gradle
# and this builds unchanged from Maven Central.
RUN ./gradlew --no-daemon :gcp:cloud-run:workerid:installDist

FROM eclipse-temurin:17-jre-jammy

RUN useradd --create-home --uid 10001 temporal
WORKDIR /app
COPY --from=build --chown=temporal:temporal \
    /workspace/gcp/cloud-run/workerid/build/install/cloud-run-worker-id/ /app/

USER 10001
ENTRYPOINT ["/app/bin/cloud-run-worker-id"]
