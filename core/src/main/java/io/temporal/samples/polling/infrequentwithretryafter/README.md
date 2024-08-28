## Infrequent polling with Service returning Retry-After time 

* Note - for this sample to work you should use Temporal Service
version 1.24.2 or Temporal Cloud

* Note - for sample we assume that our downstream service returns a retry-after duration
that is longer than 1 minute

This sample shows how we can use Activity retries for infrequent polling of a third-party service (for example via REST). 
This method can be used for infrequent polls of one minute or slower.
For this sample our test service also returns a Retry-After time (typically its done via response header but 
for sample its just done in service exception)

We utilize activity retries for this option, setting Retries options:
* setBackoffCoefficient to 1
* here we do not set initial interval as its changed by the Retry-After duration
sent to us by the downstream service our activity invokes
* 
This will allow us to retry our Activity based on the Retry-After duration our downstream service 
tells us.

To run this sample: 
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.polling.infrequent.InfrequentPollingWithRetryAfterStarter
```

Since our test service simulates it being "down" for 3 polling attempts and then returns "OK" on the 4th poll attempt, 
our Workflow is going to perform 3 activity retries 
with different intervals based on the Retry-After time our serviec gives us, 
and then return the service result on the successful 4th attempt. 

Note that individual Activity retries are not recorded in 
Workflow History, so we this approach we can poll for a very long time without affecting the history size.

### Sample result
If you run this sample you can see following in the logs for example:

```
Attempt: 1 Poll time: 2024-07-14T22:03:03.750506
Activity next retry in: 2 minutes
Attempt: 2 Poll time: 2024-07-14T22:05:03.780079
Activity next retry in: 3 minutes
Attempt: 3 Poll time: 2024-07-14T22:08:03.799703
Activity next retry in: 1 minutes
Attempt: 4 Poll time: 2024-07-14T22:09:03.817751
Result: OK
```