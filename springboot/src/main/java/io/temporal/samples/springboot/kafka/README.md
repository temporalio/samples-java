# SpringBoot Kafka Request / Reply Sample

1. Start SpringBoot from main samples repo directory:

       ./gradlew :springboot:bootRun

2. In your browser navigate to:

       http://localhost:3030/kafka

## Use Case
When you start adopting Temporal in your existing applications it can 
very often become a much better replacement to any queueing techs like Kafka.
Even tho we can replace big parts of our unreliable architecture with Temporal
often it's not a complete replacement and we still have services that produce 
messages/events to Kafka topics and workflow results need to be pushed to Kafka in order
to be consumed by these existing services.
In this sample we show how messages sent to Kafka topics can trigger workflow execution
as well as how via activities we can produce messages to Kafka topics that can be consumed
by other existing services you might have. 

## How to use
Enter a message you want to set to Kafka topic. Message consumer when it receives it 
will start a workflow execution and deliver message to it as signal. 
Workflow execution performs some sample steps. For each step it executes an activity which uses
Kafka producer to send message to Kafka topic. 
In the UI we listen on this kafka topic and you will see all messages produced by activities
show up dynamically as they are happening.

## Note
We use embedded (in-memory) Kafka to make it easier to run this sample.
You should not use this in your applications outside of tests. 