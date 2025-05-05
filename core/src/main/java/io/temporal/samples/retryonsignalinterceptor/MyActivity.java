

package io.temporal.samples.retryonsignalinterceptor;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface MyActivity {
  void execute();
}
