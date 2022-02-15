# OpenTracing Sample

This sample shows the Temporal Java SDK OpenTracing support.
The sample uses [CNCF Jaeger](https://github.com/jaegertracing/jaeger) distributed tracing
platform.

## Run the sample

Note, it is assumed that you have Temporal Server set up and running using Docker Compose.
For more information on how to do this see the [main readme](../../../../../../../README.md).

1. Start Jaeger via Docker:

```bash
docker run -d -p 5775:5775/udp -p 16686:16686 jaegertracing/all-in-one:latest
```

Note that we set the udp port to 5775, this is also reflected in [JagerUtils](JaegerUtils.java).
If you start Jaeger with a different setup, please update JaegerUtils accordingly.


1. Start the Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.opentracing.TracingWorker
```

2. Start the Starter

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.opentracing.Starter
```

3. Go to your Jaeger UI on [http://127.0.0.1:16686/search](http://127.0.0.1:16686/search)

4. In the "Service" section select "temporal-sample" service

5. Check out the Operation dropdown to see all the different operations available

6. In the Tags search input you can tag a specific workflow id, for example:

```
workflowId=tracingWorkflow
```

7. Click on "Find Traces" in the Jager UI and see all look at all the spans info
