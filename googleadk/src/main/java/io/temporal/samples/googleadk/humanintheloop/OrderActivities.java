package io.temporal.samples.googleadk.humanintheloop;

import com.google.adk.tools.Annotations.Schema;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import java.util.Map;

@ActivityInterface
public interface OrderActivities {
  @ActivityMethod(name = "ShipOrder")
  Map<String, Object> shipOrder(
      @Schema(name = "orderId", description = "The order to ship") String orderId);
}
