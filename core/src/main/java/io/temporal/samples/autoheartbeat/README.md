# Auto-heartbeating sample for activities that define HeartbeatTimeout

This sample shows an implementation of an "auto-heartbeating" utility that can be applied via interceptor
to all activities where you define HeartbeatTimeout. Use case where this can be helpful include
situations where you have long-running activities where you want to heartbeat but its difficult 
to explicitly call heartbeat api in activity code directly. 

1. Start the Sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.autoheartbeat.Starter
```

The sample workflow starts three activities async, two of which define heartbeat timeout. 
Activity interceptor in this sample applies the auto-heartbeating util to the two that define heartbeat timeout
and auto-heartbeats at its HeartbeatTimeout - 1s intervals.