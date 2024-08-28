# SpringBoot Actuator Worker Info Endpoint - Sample

1. Start SpringBoot from main samples repo directory:

       ./gradlew bootRun

2. In your browser navigate to:

       http://localhost:3030/actuator/temporalworkerinfo

This sample shows how to create a custom Actuator Endpoint that
displays registered workflow and activity implementations per task queue.
This information comes from actually registered workers done by autoconfig module.