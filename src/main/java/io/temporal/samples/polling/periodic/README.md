## Periodic polling

This samples shows periodic polling via child workflow. Note this is a rare case scenario
where polling requires execution of a sequence of activities, or activity arguments need to change
between polling retries.
For this case we use a child workflow calls polling activities a set number of times in a loop and then periodically 
calls continue as new. The parent workflow is not aware about the child workflow
calling continue as new and it gets notified when it completes (or fails). 

To run this sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.polling.periodic.Starter
```
