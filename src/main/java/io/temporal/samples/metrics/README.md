# Setting up SDK metrics (Prometheus)

This sample shows setup for SDK metrics.

1. Start the Worker:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.metrics.MetricsWorker
```

2. Start the Starter:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.metrics.MetricsStarter
```

3. See the metrics on: [http://localhost:8080/sdkmetrics](http://localhost:8080/sdkmetrics)
