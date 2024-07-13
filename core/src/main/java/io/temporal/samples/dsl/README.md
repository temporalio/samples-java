# DSL Sample

This sample shows how to use a DSL on top of Temporal.
The sample defines a number of domain specific json samples 
which are used to define steps of actions to be performed by our workflow.

As with all samples, use the following at your own risk. 

As a rule, DSLs provide limited and restrictive functionality. 
They are not suitable for full expressive development. 

In many cases, it's better to build customized DSLs to optimize simplicity and domain targeting for your particular use case.

## Run the sample

1Start the Starter

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.dsl.Starter
```