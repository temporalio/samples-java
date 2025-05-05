

package io.temporal.samples.bookingsyncsaga;

import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TripBookingWorkflow {
  @WorkflowMethod
  void bookTrip(String name);

  /**
   * Used to wait for booking completion or failure. After this method returns a failure workflow
   * keeps running executing compensations.
   *
   * @return booking information.
   */
  @UpdateMethod
  Booking waitForBooking();
}
