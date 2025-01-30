# Sleep for days

This sample demonstrates how to use Temporal to run a workflow that periodically sleeps for a number of days.

## Run the sample

1. Start the Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.sleepfordays.Worker
```

2. Start the Starter

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.sleepfordays.Starter
```