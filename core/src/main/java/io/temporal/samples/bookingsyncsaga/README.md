## Saga example: synchronous trip booking

The sample demonstrates low latency workflow with client synchronously waiting for result using an update.
In case of failures the caller is unblocked and workflow continues executing compensations
for as long as needed.

Run the following command to start the worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.bookingsyncsaga.TripBookingWorker
```

Run the following command to request a booking.
Note that the booking is expected to fail to demonstrate the compensation flow.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.bookingsyncsaga.TripBookingClient
```
