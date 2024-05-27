# SpringBoot Camel Sample

1. Start SpringBoot from main samples repo directory:

       ./gradlew :springboot:bootRun

2. In your browser navigate to:

       http://localhost:3030/orders

This sample starts an Apache Camel route which starts our orders Workflow.
The workflow starts an activity which starts Camel route to get all orders JPA.