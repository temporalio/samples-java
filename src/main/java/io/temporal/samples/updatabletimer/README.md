# Updatable Timer Sample

Demonstrates a helper class which relies on `Workflow.await` to implement a blocking sleep that can be updated at any moment.

The sample is composed of the three executables:

* `DynamicSleepWorkflowWorker` hosts the Workflow Executions.
* `DynamicSleepWorkflowStarter` starts Workflow Executions.
* `WakeUpTimeUpdater` Signals the Workflow Execution with the new time to wake up.

First start the Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.DynamicSleepWorkflowWorker
```

Then in a different terminal window start the Workflow Execution:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.DynamicSleepWorkflowStarter
```

Check the output of the Worker window. The expected output is:

```bash
10:38:56.282 [main] INFO  i.t.s.timerupdate.DynamicSleepWorker - Worker started for task queue: TimerUpdate
10:39:07.359 [workflow-732875527] INFO  i.t.s.t.DynamicSleepWorkflowImpl - sleepUntil: Thu May 28 10:40:06 PDT 2020
10:39:07.360 [workflow-732875527] INFO  i.t.s.t.DynamicSleepWorkflowImpl - Going to sleep for PT59.688S
```

Then run the updater as many times as you want to change timer to 10 seconds from now:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.WakeUpTimeUpdater
./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.WakeUpTimeUpdater
```

Check the output of the worker window. The expected output is:

```bash
10:39:12.934 [workflow-732875527] INFO  i.t.s.t.DynamicSleepWorkflowImpl - Going to sleep for PT9.721S
10:39:20.755 [workflow-732875527] INFO  i.t.s.t.DynamicSleepWorkflowImpl - Going to sleep for PT9.733S
10:39:30.772 [workflow-732875527] INFO  i.t.s.t.DynamicSleepWorkflowImpl - sleepUntil completed
```
