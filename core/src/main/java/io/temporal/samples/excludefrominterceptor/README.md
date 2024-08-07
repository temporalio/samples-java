# Excluding certain Workflow and Activity Types from interceptors

This sample shows how to exclude certain workflow types and Activity types from Workflow and Activity Interceptors.

1. Start the Sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.excludefrominterceptor.RunMyWorkflows
```

Observe the event histories of MyWorkflowOne and MyWorkflowTwo in your Temporal Web UI. 
You should see that even tho both executions were served by same worker so both had the interceptors applied,
MyWorkflowTwo was excluded from being applied by these interceptors.

Also from the Activity interceptor logs (System.out prints during sample run) note that 
only ActivityOne activity is being intercepted and not ActivityTwo or the "ForInterceptor" activities.
