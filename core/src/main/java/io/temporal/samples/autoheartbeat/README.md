# Auto-heartbeating sample for activities that define HeartbeatTimeout

This sample shows an implementation of an "auto-heartbeating" utility that can be applied via interceptor to all
activities where you define HeartbeatTimeout. Use case where this can be helpful include situations where you have
long-running activities where you want to heartbeat but its difficult to explicitly call heartbeat api in activity code
directly.
Another useful scenario for this is where you have activity that at times can complete in very short amount of time,
but then at times can take for example minutes. In this case you have to set longer StartToClose timeout 
but you might not want first heartbeat to be sent right away but send it after the "shorter" duration of activity
execution. 

Warning: make sure to test this sample for your use case. This includes load testing. This sample was not 
tested on large scale workloads. In addition note that it is recommended to heartbeat from activity code itself. Using
this type of autoheartbeating utility does have disatvantage that activity code itself can continue running after 
a handled activity cancelation. Please be aware of these warnings when applying this sample.

1. Start the Sample:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.autoheartbeat.Starter
```