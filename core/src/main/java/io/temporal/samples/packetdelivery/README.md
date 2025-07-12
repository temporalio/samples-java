# Async Package Delivery Sample

This sample show how to run multiple "paths" of execution async within single workflow.
Sample starts deliveries of 5 items in parallel. Each item performs an activity
and then waits for a confirmation signal, then performs second activity.

Workflow waits until all packets have been delivered. Each packet delivery path can choose to
also "cancel" delivery of another item. This is done via signal and cancellation of the 
CancellationScope. 

## Start the Sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.packetdelivery.Starter
``

Run sample multiple times to see different scenarios (delivery failure and retry and delivery cancelation)
There is a 10% chance delivery is going to be canceled and 20% chane it will fail. 