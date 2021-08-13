# Async Child Workflow execution

The sample demonstrates shows how to invoke a child workflow
async.
The child workflow is allowed to complete its execution
even after the parent workflow completes. 

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.asyncchild.Starter
```
