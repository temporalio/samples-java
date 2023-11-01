# Async Child Workflow execution

The sample demonstrates shows how to invoke an Untyped Child Workflow asynchronously.
The Child Workflow continues running for some time after the Parent Workflow completes. 

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.asyncuntypedchild.Starter
```
