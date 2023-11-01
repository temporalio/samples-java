# Async Child Workflow execution

The sample demonstrates shows how to invoke an Untyped Child Workflow asynchronously.
The Child Workflow is allowed to complete its execution even after the Parent Workflow completes. 

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.asyncuntypedchild.Starter
```
