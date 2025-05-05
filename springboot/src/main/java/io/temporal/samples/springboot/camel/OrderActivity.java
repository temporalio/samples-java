

package io.temporal.samples.springboot.camel;

import io.temporal.activity.ActivityInterface;
import java.util.List;

@ActivityInterface
public interface OrderActivity {
  List<OfficeOrder> getOrders();
}
