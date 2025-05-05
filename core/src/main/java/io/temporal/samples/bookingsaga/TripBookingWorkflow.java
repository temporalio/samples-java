

package io.temporal.samples.bookingsaga;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TripBookingWorkflow {
  @WorkflowMethod
  Booking bookTrip(String name);
}
