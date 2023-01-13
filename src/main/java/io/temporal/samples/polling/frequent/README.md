## Frequent polling

This sample shows how we can implement frequent polling (1 second or faster) inside our Activity.
The implementation is a loop that polls our service and then sleeps for the poll interval (1 second in the sample).

To ensure that polling activity is restarted in a timely manner, we make sure that it heartbeats on every iteration.
Note that heartbeating only works if we set the HeartBeatTimeout to a shorter value than the activity
StartToClose timeout.


To run this sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.polling.frequent.FrequentPollingStarter
```