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

```
[...]
11:39:08.852 [main] INFO  i.t.s.u.DynamicSleepWorkflowWorker - Worker started for task queue: TimerUpdate
11:39:31.614 [workflow-method] INFO  i.t.s.updatabletimer.UpdatableTimer - sleepUntil: 2021-11-30T19:40:30.979Z
11:39:31.615 [workflow-method] INFO  i.t.s.updatabletimer.UpdatableTimer - Going to sleep for PT59.727S
```

Then run the updater as many times as you want to change timer to 10 seconds from now:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.WakeUpTimeUpdater
./gradlew -q execute -PmainClass=io.temporal.samples.updatabletimer.WakeUpTimeUpdater
```

Check the output of the worker window. The expected output is:

```
[...]
11:39:37.740 [signal updateWakeUpTime] INFO  i.t.s.updatabletimer.UpdatableTimer - updateWakeUpTime: 2021-11-30T19:39:47.552Z
11:39:37.740 [workflow-method] INFO  i.t.s.updatabletimer.UpdatableTimer - Going to sleep for PT9.841S
11:39:44.679 [signal updateWakeUpTime] INFO  i.t.s.updatabletimer.UpdatableTimer - updateWakeUpTime: 2021-11-30T19:39:54.494Z
11:39:44.680 [workflow-method] INFO  i.t.s.updatabletimer.UpdatableTimer - Going to sleep for PT9.838S
11:39:54.565 [workflow-method] INFO  i.t.s.updatabletimer.UpdatableTimer - sleepUntil completed
```