# Demo Workflow Interceptor

The sample demonstrates: 
- the use of a simple Worker Workflow Interceptor that counts the number of Workflow Executions, Child Workflow Executions, and Activity Executions as well as the number of Signals and Queries.
- the use of a simple Client Workflow Interceptor that counts the number of Workflow Executions as well as the number of Signals, Queries and GetResult invocations.

Run the following command to start the sample:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.countinterceptor.InterceptorStarter
```
