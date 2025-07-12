# Async Package Delivery Sample

This sample show how to run multiple "paths" of execution async within single workflow.
Sample starts deliveries of 5 items in parallel. Each item performs an activity
and then waits for a confirmation signal, then performs second activity.

Workflow waits until all packets have been delivered. Each packet delivery path can choose to
also "cancel" delivery of another item. This is done via signal and cancellation of the 
CancellationScope. 

2. Start the Sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.packetdelivery.Starter
``