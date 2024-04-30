# Demo tasks interaction

This example demonstrates a generic implementation for "User Tasks" interaction with Temporal, 
which can be easily implemented as follows: 
- The main workflow [WorkflowWithTasks](./WorkflowWithTasks.java) have an activity (or local activity) that send the request to an external service. 
The _external service_, for this example, is another workflow ([WorkflowTaskManager](WorkflowTaskManager.java)), 
that takes care of the task life-cicle.
- The main workflow waits, with `Workflow.await`, to receive a Signal. 
- The _external service_ signal back the main 
workflow to unblock it.

The two first steps mentioned above are encapsulated in the class [TaskService.java](./TaskService.java), to make it easily reusable.

## Run the sample

- Schedule the main workflow execution ([WorkflowWithTasks](./WorkflowWithTasks.java)), the one that contains the _User Tasks_ 

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.StartWorkflow
```

- Open other terminal and start the Worker

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.worker.Worker
```

You will notice, from the worker logs, that it start the main workflow and execute two activities, the
two activities register two tasks to the external service ([WorkflowTaskManagerImpl.java](WorkflowTaskManagerImpl.java))

```
07:19:39.528 {WorkflowWithTasks1714454371179 } [workflow[WorkflowWithTasks1714454371179]-1] INFO  i.t.s.taskinteraction.TaskService - Before creating task : Task{token='WorkflowWithTasks1714454371179_1', title=TaskTitle{value='TODO 1'}} 
07:19:39.563 {WorkflowWithTasks1714454371179 } [workflow[WorkflowWithTasks1714454371179]-2] INFO  i.t.s.taskinteraction.TaskService - Before creating task : Task{token='WorkflowWithTasks1714454371179_2', title=TaskTitle{value='TODO 2'}} 
07:19:39.683 {WorkflowWithTasks1714454371179 } [workflow[WorkflowWithTasks1714454371179]-1] INFO  i.t.s.taskinteraction.TaskService - Task created: Task{token='WorkflowWithTasks1714454371179_1', title=TaskTitle{value='TODO 1'}} 
07:19:39.684 {WorkflowWithTasks1714454371179 } [workflow[WorkflowWithTasks1714454371179]-2] INFO  i.t.s.taskinteraction.TaskService - Task created: Task{token='WorkflowWithTasks1714454371179_2', title=TaskTit
```

- Now, we can start completing the tasks using the helper class [CompleteTask.java](./client/CompleteTask.java)

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.taskinteraction.client.CompleteTask
```
You can see from the implementation that [WorkflowWithTasksImpl](./WorkflowWithTasksImpl.java) has three task.
- two in parallel using `Async.procedure`,
- one blocking task at the end.

This class needs to be run three times. After the three task are completed the main workflow completes.
