# Standalone Activities

This sample demonstrates [Standalone Activities](https://docs.temporal.io/develop/java/activities/standalone-activities),
which run independently without being orchestrated by a Workflow. You start and manage them directly
from a Temporal Client using `ActivityClient`.

The sample has these separate programs:

| Program | Purpose |
|---|---|
| `StandaloneActivityWorker` | Runs a Worker that processes activity tasks |
| `ExecuteActivity` | Starts an activity and waits for its result |
| `StartActivity` | Starts an activity without blocking, then waits for the result |
| `ListActivities` | Lists activity executions on the task queue |
| `CountActivities` | Counts activity executions on the task queue |

## Prerequisites

- Temporal dev server with Standalone Activity support. See the
  [Java SDK Standalone Activities guide](https://docs.temporal.io/develop/java/activities/standalone-activities#get-started)
  for download instructions.

## Start the Temporal development server

```bash
./temporal server start-dev
```

## Run the Worker

In a terminal, start the Worker. Leave it running to process activities.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.standaloneactivities.StandaloneActivityWorker
```

## Execute a Standalone Activity

In another terminal, execute an activity and wait for its result:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.standaloneactivities.ExecuteActivity
```

Or use the Temporal CLI:

```bash
./temporal activity execute \
  --type composeGreeting \
  --activity-id standalone-activity-id \
  --task-queue standalone-activity-task-queue \
  --start-to-close-timeout 10s \
  --input '"Hello"' \
  --input '"World"'
```

## Start a Standalone Activity without waiting

Start an activity and retrieve its result separately:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.standaloneactivities.StartActivity
```

Or use the Temporal CLI:

```bash
./temporal activity start \
  --type composeGreeting \
  --activity-id standalone-activity-id \
  --task-queue standalone-activity-task-queue \
  --start-to-close-timeout 10s \
  --input '"Hello"' \
  --input '"World"'
```

## List Standalone Activities

List activity executions on the task queue:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.standaloneactivities.ListActivities
```

Or use the Temporal CLI:

```bash
./temporal activity list
```

## Count Standalone Activities

Count activity executions on the task queue:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.standaloneactivities.CountActivities
```

Or use the Temporal CLI:

```bash
./temporal activity count
```
