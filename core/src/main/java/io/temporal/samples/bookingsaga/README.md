## Saga example: trip booking

Temporal implementation of
the [Camunda BPMN trip booking example](https://github.com/berndruecker/trip-booking-saga-java) which demonstrates
Temporal approach to SAGA.

Run the following command to start the sample:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.bookingsaga.TripBookingSaga
```

Note that the booking is expected to fail to demonstrate the compensation flow.

Sample unit
testing: [TripBookingWorkflowTest](https://github.com/temporalio/samples-java/blob/main/core/src/test/java/io/temporal/samples/bookingsaga/TripBookingWorkflowTest.java)
