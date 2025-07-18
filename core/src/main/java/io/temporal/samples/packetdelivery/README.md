# Async Package Delivery Sample

This sample show how to run multiple "paths" of execution async within single workflow.
Sample starts deliveries of 5 items in parallel. Each item performs an activity
and then waits for a confirmation signal, then performs second activity.

Workflow waits until all packets have been delivered. Each packet delivery path can choose to
also "cancel" delivery of another item. This is done via signal and cancellation of the 
CancellationScope. 

## Notes
1. In this sample we do not handle event history count and size partitioning via ContinueAsNew. It is assumed
that the total number of paths and path lengths (in terms of activity executions) would not exceed it. 
For your use case you might need to add ContinueAsNew checks to deal with this situation.
2. Use this sample as all other ones as reference for your implementation. It was not tested on high scale 
so using it as-is without load testing is not recommended.

## Start the Sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.packetdelivery.Starter
```

Run sample multiple times to see different scenarios (delivery failure and retry and delivery cancelation)
There is a 10% chance delivery is going to be canceled and 20% chane it will fail. 