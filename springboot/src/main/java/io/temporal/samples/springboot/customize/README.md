# SpringBoot Customize Options Sample

This sample shows how to optimize default options such as
* WorkflowServiceStubsOptions
* WorkflowClientOption
* WorkerFactoryOptions
* WorkerOptions

WorkerOptions can be optimized per worker/task queue.

For this sample we set our specific worker to be "local activity worker" via custom options meaning
it would not poll for activity tasks. Click on "Run Workflow" button to start instance of
our sample workflow. This workflow will try to invoke our activity as "normal"
activity which should time out on ScheduleToClose timeout, then we invoke this activity
as local activity which should succeed.

## How to run
1. Start SpringBoot from main samples repo directory:

       ./gradlew :springboot:bootRun

2. In your browser navigate to:

       http://localhost:3030/customize

3. Press the "Run Workflow" button to start execution. You will see result show on page in 4 seconds