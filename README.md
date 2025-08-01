# Temporal Java SDK Samples

This repository contains samples that demonstrate various capabilities of 
Temporal using the [Java SDK](https://github.com/temporalio/sdk-java).

It contains two modules:
* [Core](/core): showcases many different SDK features.
* [SpringBoot](/springboot): showcases SpringBoot autoconfig integration.
* [SpringBoot Basic](/springboot-basic): Minimal sample showing SpringBoot autoconfig integration without any extra external dependencies.

## Learn more about Temporal and Java SDK

- [Temporal Server repo](https://github.com/temporalio/temporal)
- [Java SDK repo](https://github.com/temporalio/sdk-java)
- [Java SDK Guide](https://docs.temporal.io/dev-guide/java)

## Requirements

- Java 1.8+ for build and runtime of core samples
- Java 1.8+ for build and runtime of SpringBoot samples when using SpringBoot 2
- Java 1.17+ for build and runtime of Spring Boot samples when using SpringBoot 3
- Local Temporal Server, easiest to get started would be using [Temporal CLI](https://github.com/temporalio/cli).
For more options see docs [here](https://docs.temporal.io/kb/all-the-ways-to-run-a-cluster).


## Build and run tests

1. Clone this repository:

       git clone https://github.com/temporalio/samples-java
       cd samples-java

2. Build and run Tests

       ./gradlew build

## Running Samples:

You can run both "Core" and "SpringBoot" samples from the main samples project directory.
Details on how to run each sample can be found in following two sections.
To skip to SpringBoot samples click [here](#Running-SpringBoot-Samples).

### Running "Core" samples
See the README.md file in each main sample directory for cut/paste Gradle command to run specific example.

<!-- @@@SNIPSTART samples-java-readme-samples-directory -->

#### Hello samples

- [**Hello**](/core/src/main/java/io/temporal/samples/hello): This sample includes a number of individual Workflows that can be executed independently. Each one demonstrates something specific.

    - [**HelloAccumulator**](/core/src/main/java/io/temporal/samples/hello/HelloAccumulator.java): Demonstrates a Workflow Definition that accumulates signals and continues as new.
    - [**HelloActivity**](/core/src/main/java/io/temporal/samples/hello/HelloActivity.java): Demonstrates a Workflow Definition that executes a single Activity.
    - [**HelloActivityRetry**](/core/src/main/java/io/temporal/samples/hello/HelloActivityRetry.java): Demonstrates how to Retry an Activity Execution.
    - [**HelloActivityExclusiveChoice**](/core/src/main/java/io/temporal/samples/hello/HelloActivityExclusiveChoice.java): Demonstrates how to execute Activities based on dynamic input.
    - [**HelloAsync**](/core/src/main/java/io/temporal/samples/hello/HelloAsync.java): Demonstrates how to execute Activities asynchronously and wait for them using Promises.
    - [**HelloAwait**](/core/src/main/java/io/temporal/samples/hello/HelloAwait.java): Demonstrates how to use Await statement to wait for a condition.
    - [**HelloParallelActivity**](/core/src/main/java/io/temporal/samples/hello/HelloParallelActivity.java): Demonstrates how to execute multiple Activities in parallel, asynchronously, and wait for them using `Promise.allOf`.
    - [**HelloAsyncActivityCompletion**](/core/src/main/java/io/temporal/samples/hello/HelloAsyncActivityCompletion.java): Demonstrates how to complete an Activity Execution asynchronously.
    - [**HelloAsyncLambda**](/core/src/main/java/io/temporal/samples/hello/HelloAsyncLambda.java): Demonstrates how to execute part of a Workflow asynchronously in a separate task (thread).
    - [**HelloCancellationScope**](/core/src/main/java/io/temporal/samples/hello/HelloCancellationScope.java): Demonstrates how to explicitly cancel parts of a Workflow Execution.
    - [**HelloCancellationScopeWithTimer**](/core/src/main/java/io/temporal/samples/hello/HelloCancellationScopeWithTimer.java): Demonstrates how to cancel activity when workflow timer fires and complete execution. This can prefered over using workflow run/execution timeouts.
    - [**HelloDetachedCancellationScope**](/core/src/main/java/io/temporal/samples/hello/HelloDetachedCancellationScope.java): Demonstrates how to execute cleanup code after a Workflow Execution has been explicitly cancelled.
    - [**HelloChild**](/core/src/main/java/io/temporal/samples/hello/HelloChild.java): Demonstrates how to execute a simple Child Workflow.
    - [**HelloCron**](/core/src/main/java/io/temporal/samples/hello/HelloCron.java): Demonstrates how to execute a Workflow according to a cron schedule.
    - [**HelloDynamic**](/core/src/main/java/io/temporal/samples/hello/HelloDynamic.java): Demonstrates how to use `DynamicWorkflow` and `DynamicActivity` interfaces.
    - [**HelloEagerWorkflowStart**](/core/src/main/java/io/temporal/samples/hello/HelloEagerWorkflowStart.java): Demonstrates the use of a eager workflow start.
    - [**HelloPeriodic**](/core/src/main/java/io/temporal/samples/hello/HelloPeriodic.java): Demonstrates the use of the Continue-As-New feature.
    - [**HelloException**](/core/src/main/java/io/temporal/samples/hello/HelloException.java): Demonstrates how to handle exception propagation and wrapping.
    - [**HelloLocalActivity**](/core/src/main/java/io/temporal/samples/hello/HelloLocalActivity.java): Demonstrates the use of a [Local Activity](https://docs.temporal.io/docs/jargon/mesh/#local-activity).
    - [**HelloPolymorphicActivity**](/core/src/main/java/io/temporal/samples/hello/HelloPolymorphicActivity.java): Demonstrates Activity Definitions that extend a common interface.
    - [**HelloQuery**](/core/src/main/java/io/temporal/samples/hello/HelloQuery.java): Demonstrates how to Query the state of a Workflow Execution.
    - [**HelloSchedules**](/core/src/main/java/io/temporal/samples/hello/HelloSchedules.java): Demonstrates how to create and interact with a Schedule.
    - [**HelloSignal**](/core/src/main/java/io/temporal/samples/hello/HelloSignal.java): Demonstrates how to send and handle a Signal.
    - [**HelloSaga**](/core/src/main/java/io/temporal/samples/hello/HelloSaga.java): Demonstrates how to use the SAGA feature.
    - [**HelloSearchAttributes**](/core/src/main/java/io/temporal/samples/hello/HelloSearchAttributes.java): Demonstrates how to add custom Search Attributes to Workflow Executions.
    - [**HelloSideEffect**](/core/src/main/java/io/temporal/samples/hello/HelloSideEffect.java)**: Demonstrates how to implement a Side Effect.
    - [**HelloUpdate**](/core/src/main/java/io/temporal/samples/hello/HelloUpdate.java): Demonstrates how to create and interact with an Update.
    - [**HelloDelayedStart**](/core/src/main/java/io/temporal/samples/hello/HelloDelayedStart.java): Demonstrates how to use delayed start config option when starting a Workflow Executions.
    - [**HelloSignalWithTimer**](/core/src/main/java/io/temporal/samples/hello/HelloSignalWithTimer.java): Demonstrates how to use collect signals for certain amount of time and then process last one. 
    - [**HelloWorkflowTimer**](/core/src/main/java/io/temporal/samples/hello/HelloWorkflowTimer.java): Demonstrates how we can use workflow timer to restrict duration of workflow execution instead of workflow run/execution timeouts.
    - [**Auto-Heartbeating**](/core/src/main/java/io/temporal/samples/autoheartbeat/): Demonstrates use of Auto-heartbeating utility via activity interceptor. 
    - [**HelloSignalWithStartAndWorkflowInit**](/core/src/main/java/io/temporal/samples/hello/HelloSignalWithStartAndWorkflowInit.java): Demonstrates how WorkflowInit can be useful with SignalWithStart to initialize workflow variables.

#### Scenario-based samples

- [**File Processing Sample**](/core/src/main/java/io/temporal/samples/fileprocessing): Demonstrates how to route tasks to specific Workers. This sample has a set of Activities that download a file, processes it, and uploads the result to a destination. Any Worker can execute the first Activity. However, the second and third Activities must be executed on the same host as the first one.

- [**Booking SAGA**](/core/src/main/java/io/temporal/samples/bookingsaga): Demonstrates Temporals take on the Camunda BPMN "trip booking" example.

- [**Synchronous Booking SAGA**](/core/src/main/java/io/temporal/samples/bookingsyncsaga): Demonstrates low latency SAGA with potentially long compensations.

- [**Money Transfer**](/core/src/main/java/io/temporal/samples/moneytransfer): Demonstrates the use of a dedicated Activity Worker.

- [**Money Batch**](/core/src/main/java/io/temporal/samples/moneybatch): Demonstrates a situation where a single deposit should be initiated for multiple withdrawals. For example, a seller might want to be paid once per fixed number of transactions. This sample can be easily extended to perform a payment based on more complex criteria, such as at a specific time or an accumulated amount. The sample also demonstrates how to Signal the Workflow when it executes (*Signal with start*). If the Workflow is already executing, it just receives the Signal. If it is not executing, then the Workflow executes first, and then the Signal is delivered to it. *Signal with start* is a "lazy" way to execute Workflows when Signaling them.

- [**Domain-Specific-Language - Define sequence of steps in JSON**](/core/src/main/java/io/temporal/samples/dsl): Demonstrates using domain specific language (DSL) defined in JSON to specify sequence of steps to be performed in our workflow.

- [**Polling Services**](/core/src/main/java/io/temporal/samples/polling): Recommended implementation of an activity that needs to periodically poll an external resource waiting its successful completion

- [**Heartbeating Activity Batch**](/core/src/main/java/io/temporal/samples/batch/heartbeatingactivity): Batch job implementation using a heartbeating activity.

- [**Iterator Batch**](/core/src/main/java/io/temporal/samples/batch/iterator): Batch job implementation using the workflow iterator pattern.

- [**Sliding Window Batch**](/core/src/main/java/io/temporal/samples/batch/slidingwindow): A batch implementation that maintains a configured number of child workflows during processing.

- [**Safe Message Passing**](/core/src/main/java/io/temporal/samples/safemessagepassing): Safely handling concurrent updates and signals messages.

- [**Custom Annotation**](/core/src/main/java/io/temporal/samples/customannotation): Demonstrates how to create a custom annotation using an interceptor.

- [**Asnyc Packet Delivery**](/core/src/main/java/io/temporal/samples/packetdelivery): Demonstrates running multiple execution paths async within single execution.


#### API demonstrations

- [**Async Untyped Child Workflow**](/core/src/main/java/io/temporal/samples/asyncuntypedchild): Demonstrates how to invoke an untyped child workflow async, that can complete after parent workflow is already completed.

- [**Updatable Timer**](/core/src/main/java/io/temporal/samples/updatabletimer): Demonstrates the use of a helper class which relies on `Workflow.await` to implement a blocking sleep that can be updated at any moment.

- [**Workflow Count Interceptor**](/core/src/main/java/io/temporal/samples/countinterceptor): Demonstrates how to create and register a simple Workflow Count Interceptor.

- [**Workflow Retry On Signal Interceptor**](/core/src/main/java/io/temporal/samples/retryonsignalinterceptor): Demonstrates how to create and register an interceptor that retries an activity on a signal.

- [**List Workflows**](/core/src/main/java/io/temporal/samples/listworkflows): Demonstrates the use of custom search attributes and ListWorkflowExecutionsRequest with custom queries.

- [**Payload Converter - CloudEvents**](/core/src/main/java/io/temporal/samples/payloadconverter/cloudevents): Demonstrates the use of a custom payload converter for CloudEvents.

- [**Payload Converter - Crypto**](/core/src/main/java/io/temporal/samples/payloadconverter/crypto): Demonstrates the use of a custom payload converter using jackson-json-crypto.

- [**Async Child Workflow**](/core/src/main/java/io/temporal/samples/asyncchild): Demonstrates how to invoke a child workflow async, that can complete after parent workflow is already completed.

- [**Terminate Workflow**](/core/src/main/java/io/temporal/samples/terminateworkflow): Demonstrates how to terminate a workflow using client API.

- [**Get Workflow Results Async**](/core/src/main/java/io/temporal/samples/getresultsasync): Demonstrates how to start and get workflow results in async manner.

- [**Per Activity Type Options**](/core/src/main/java/io/temporal/samples/peractivityoptions): Demonstrates how to set per Activity type options.

- [**Configure WorkflowClient to use mTLS**](/core/src/main/java/io/temporal/samples/ssl): Demonstrates how to configure WorkflowClient when using mTLS.

- [**Configure WorkflowClient to use API Key**](/core/src/main/java/io/temporal/samples/apikey): Demonstrates how to configure WorkflowClient when using API Keys.

- [**Payload Codec**](/core/src/main/java/io/temporal/samples/encodefailures): Demonstrates how to use simple codec to encode/decode failure messages.

- [**Exclude Workflow/ActivityTypes from Interceptors**](/core/src/main/java/io/temporal/samples/excludefrominterceptor): Demonstrates how to exclude certain workflow / activity types from interceptors.

#### SDK Metrics

- [**Set up SDK metrics**](/core/src/main/java/io/temporal/samples/metrics): Demonstrates how to set up and scrape SDK metrics.

#### Tracing Support

- [**Set up OpenTracing and/or OpenTelemetry with Jaeger**](/core/src/main/java/io/temporal/samples/tracing): Demonstrates how to set up OpenTracing and/or OpenTelemetry and view traces using Jaeger.

#### Encryption Support

- [**Encrypted Payloads**](/core/src/main/java/io/temporal/samples/encryptedpayloads): Demonstrates how to use simple codec to encrypt and decrypt payloads.

- [**AWS Encryption SDK**](/core/src/main/java/io/temporal/samples/keymanagementencryption/awsencryptionsdk): Demonstrates how to use the AWS Encryption SDK to encrypt and decrypt payloads with AWS KMS.

#### Nexus Samples

- [**Getting Started**](/core/src/main/java/io/temporal/samples/nexus): Demonstrates how to get started with Temporal and Nexus.

- [**Mapping Multiple Arguments**](/core/src/main/java/io/temporal/samples/nexus): Demonstrates how map a Nexus operation to a Workflow that takes multiple arguments.

- [**Cancellation**](/core/src/main/java/io/temporal/samples/nexuscancellation): Demonstrates how to cancel an async Nexus operation.

- [**Context/Header Propagation**](/core/src/main/java/io/temporal/samples/nexuscontextpropagation): Demonstrates how to propagate context through Nexus operation headers.

<!-- @@@SNIPEND -->

### Running SpringBoot Samples

These samples use SpringBoot 2 by default. To switch to using SpringBoot 3 look at the [gradle.properties](gradle.properties) file
and follow simple instructions there.

1. Start SpringBoot from main repo dir:

       ./gradlew :springboot:bootRun

To run the basic sample run

       ./gradlew :springboot-basic:bootRun


2. Navigate to [localhost:3030](http://localhost:3030)

3. Select which sample you want to run

More info on each sample:
- [**Hello**](/springboot/src/main/java/io/temporal/samples/springboot/hello): Invoke simple "Hello" workflow from a GET endpoint
- [**SDK Metrics**](/springboot/src/main/java/io/temporal/samples/springboot/metrics): Learn how to set up SDK Metrics
- [**Synchronous Update**](/springboot/src/main/java/io/temporal/samples/springboot/update): Learn how to use Synchronous Update feature with this purchase sample
- [**Kafka Request / Reply**](/springboot/src/main/java/io/temporal/samples/springboot/kafka): Sample showing possible integration with event streaming platforms such as Kafka
- [**Customize Options**](/springboot/src/main/java/io/temporal/samples/springboot/customize): Sample showing how to customize options such as WorkerOptions, WorkerFactoryOptions, etc (see options config [here](springboot/src/main/java/io/temporal/samples/springboot/customize/TemporalOptionsConfig.java))
- [**Apache Camel Route**](/springboot/src/main/java/io/temporal/samples/springboot/camel): Sample showing how to start Workflow execution from a Camel Route
- [**Custom Actuator Endpoint**](/springboot/src/main/java/io/temporal/samples/springboot/actuator): Sample showing how to create a custom Actuator endpoint that shows registered Workflow and Activity impls per task queue.

#### Temporal Cloud
To run any of the SpringBoot samples in your Temporal Cloud namespace:

1. Edit the [application-tc.yaml](/springboot/src/main/resources/application-tc.yaml) to set your namespace and client certificates.

2. Start SpringBoot from main repo dir with the `tc` profile:

       ./gradlew bootRun --args='--spring.profiles.active=tc'

3. Follow the previous section from step 2
