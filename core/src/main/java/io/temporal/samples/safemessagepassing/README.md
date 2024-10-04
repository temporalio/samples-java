# Safe Message Passing

This sample shows off important techniques for handling signals and updates, aka messages.  In particular, it illustrates how message handlers can interleave or not be completed before the workflow completes, and how you can manage that.

* Here, using Workflow.await, signal and update handlers will only operate when the workflow is within a certain state--between clusterStarted and clusterShutdown.
* You can run start_workflow with an initializer signal that you want to run before anything else other than the workflow's constructor.  This pattern is known as "signal-with-start."
* Message handlers can block and their actions can be interleaved with one another and with the main workflow.  This can easily cause bugs, so you can use a lock to protect shared state from interleaved access.
* An "Entity" workflow, i.e. a long-lived workflow, periodically "continues as new".  It must do this to prevent its history from growing too large, and it passes its state to the next workflow.  You can check `Workflow.getInfo().isContinueAsNewSuggested()` to see when it's time.
* Most people want their message handlers to finish before the workflow run completes or continues as new.  Use `Workflow.await(() -> Workflow.isEveryHandlerFinished())` to achieve this.
* Message handlers can be made idempotent.  See update `ClusterManagerWorkflow.assignNodesToJobs`.

First start the Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.safemessagepassing.ClusterManagerWorkflowWorker
```

Then in a different terminal window start the Workflow Execution:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.safemessagepassing.ClusterManagerWorkflowStarter
```
