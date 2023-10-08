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

3. See the worker metrics on the exposed Prometheus Scrape Endpoint: [http://localhost:8077/metrics](http://localhost:8077/metrics)

4. See the starter metrics on the exposed Prometheus Scrape Endpoint [http://localhost:8078/metrics](http://localhost:8078/metrics)

5. Stop the worker and starter
