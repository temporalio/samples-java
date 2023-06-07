# Java SDK OpenTracing and OpenTelemetry Sample

This sample shows the [Temporal Java SDK OpenTracing](https://github.com/temporalio/sdk-java/tree/master/temporal-opentracing) support.
It shows how to set up OpenTracing, as well as OpenTelemetry.
The sample uses the [CNCF Jaeger](https://github.com/jaegertracing/jaeger) distributed tracing
platform.

## Run the sample

Note, it is assumed that you have Temporal Server set up and running using Docker Compose.
For more information on how to do this see the [main readme](../../../../../../../README.md).

1. Start Jaeger via Docker:

```bash
docker run -d -p 5775:5775/udp -p 14250:14250 -p 16686:16686 -p 14268:14268 jaegertracing/all-in-one:latest
```

This starts Jaeger with udp port 5775 and grpc port 14250. Note that 
if these ports are different in your setup to reflect the changes in [JagerUtils](JaegerUtils.java).

1. Start the Worker:

* For OpenTelemetry:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.opentracing.TracingWorker
```

* For OpenTracing:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.opentracing.TracingWorker --args="OpenTracing"
```

2. Start the Starter

* For OpenTelemetry

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.opentracing.Starter
```

* For OpenTracing

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.opentracing.Starter --args="OpenTracing"
```

3. Go to your Jaeger UI on [http://127.0.0.1:16686/search](http://127.0.0.1:16686/search)

4. In the "Service" section select either "temporal-sample-opentelemetry" or 
   "temporal-sample-opentracing", depending on your starting options.
   
5. Check out the Operation dropdown to see all the different operations available

6. In the Tags search input you can tag a specific workflow id, for example:

```
workflowId=tracingWorkflow
```

7. Click on "Find Traces" in the Jager UI and see all look at all the spans info
